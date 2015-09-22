package lc.com.net_library.tcpudp;

import android.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import lc.com.net_library.tcpudp.util.NETConfig;

/**
 * 服务端java NIO封装
 * 采用selector事件注册机制，在每一个server启动的时候向系统申请一个Selector,Selector.Open()函数会返回唯一的Selector给该Server使用，若需要关闭Server，需要调用Stop函数线程安全关闭函数，Close函数为非线程安全，不能保证线程安全关闭，
 * 若重新启动Server,由于ServerSocketChannel已经关闭，ServerSocketChannel与Selector唯一配对，需要重新向系统申请Selector,使得Selector与ServerSocketChannel唯一配对，否则注册OP_ACCEPT、OP_WRITE、OP_READ事件会无效
 * 在android环境调用Server方法，需要注意线程安全，建议在HandlerThread里面使用Server方法
 *
 * @author Lin
 */
public class Server implements EndPoint {
    static final String TAG = NETConfig.TAG;
    private final int writeBufferSize, readBufferSize;
    private final Selector selector;
    private int emptySelects;
    private ServerSocketChannel serverChannel;
    private UdpConnection udp;
    ArrayList<Connection> tcpconnections = new ArrayList<>();
    ArrayList<InetSocketAddress> udpconnections = new ArrayList<>();
    ArrayList<Listener> listeners = new ArrayList<>();
    ArrayList<Listener> udplisteners = new ArrayList<>();
    private Object listenerLock = new Object();
    private volatile boolean shutdown;
    private Object updateLock = new Object();
    private Thread updateThread;
    private ServerDiscoveryHandler discoveryHandler;

    private Listener dispatchListener = new Listener() {
        public void connected(Connection connection) {
            for (Listener listener : listeners) {
                listener.connected(connection);
            }
        }

        public void disconnected(Connection connection) {
            for (Listener listener : listeners) {
                listener.disconnected(connection);
            }
        }

        public void received(Connection connection, byte[] data) {
            for (Listener listener : listeners) {
                listener.received(connection, data);
            }
        }


    };

    /**
     * Creates a Server with a write buffer size of 16384 and an object buffer size of 2048.
     */
    public Server() {
        this(16384, 2048);
    }

    /**
     * @param writeBufferSize One buffer of this size is allocated for each connected client. Objects are serialized to the write
     *                        buffer where the bytes are queued until they can be written to the TCP socket.
     *                        <p/>
     *                        Normally the socket is writable and the bytes are written immediately. If the socket cannot be written to and
     *                        enough serialized objects are queued to overflow the buffer, then the connection will be closed.
     *                        <p/>
     *                        The write buffer should be sized at least as large as the largest object that will be sent, plus some head room to
     *                        allow for some serialized objects to be queued in case the buffer is temporarily not writable. The amount of head
     *                        room needed is dependent upon the size of objects being sent and how often they are sent.
     * @param readBufferSize  One (using only TCP) or three (using both TCP and UDP) buffers of this size are allocated. These
     *                        buffers are used to hold the bytes for a single object graph until it can be sent over the network or
     *                        deserialized.
     *                        <p/>
     *                        The object buffers should be sized at least as large as the largest object that will be sent or received.
     */
    public Server(int writeBufferSize, int readBufferSize) {
        this.writeBufferSize = writeBufferSize;
        this.readBufferSize = readBufferSize;
        this.discoveryHandler = ServerDiscoveryHandler.DEFAULT;

        try {
            selector = Selector.open();
        } catch (IOException ex) {
            throw new RuntimeException("Error opening selector.", ex);
        }
    }

    public void setDiscoveryHandler(ServerDiscoveryHandler newDiscoveryHandler) {
        discoveryHandler = newDiscoveryHandler;
    }

    /**
     * Opens a TCP only server.
     *
     * @throws IOException if the server could not be opened.
     */
    public void bind(int tcpPort) throws IOException {
        InetSocketAddress tcpadr = null;
        if (tcpPort > 0) {
            tcpadr = new InetSocketAddress(tcpPort);
        }
        bind(tcpadr, null);
    }

    /**
     * Opens a TCP and UDP server.
     *
     * @throws IOException if the server could not be opened.
     */
    public void bind(int tcpPort, int udpPort) throws IOException {
        InetSocketAddress tcpadr = null;
        InetSocketAddress udpadr = null;
        if (tcpPort > 0) {
            tcpadr = new InetSocketAddress(tcpPort);
        }

        if (udpPort > 0) {
            udpadr = new InetSocketAddress(udpPort);
        }
        bind(tcpadr, udpadr);
    }

    /**
     * @param udpPort May be null.
     */
    public void bind(InetSocketAddress tcpPort, InetSocketAddress udpPort) throws IOException {
        close();
        synchronized (updateLock) {
            selector.wakeup();
            try {
                if (tcpPort != null) {
                    serverChannel = selector.provider().openServerSocketChannel();
                    serverChannel.socket().bind(tcpPort);
                    serverChannel.configureBlocking(false);
                    serverChannel.register(selector, SelectionKey.OP_ACCEPT);
                    if (NETConfig.NET_DEBUG)
                        Log.i(TAG, "Accepting connections on port: " + tcpPort + "/TCP");
                }

                if (udpPort != null) {
                    udp = new UdpConnection();
                    udp.bind(selector, udpPort);
                    if (NETConfig.NET_DEBUG)
                        Log.i(TAG, "Accepting connections on port: " + udpPort + "/UDP");
                }
            } catch (IOException ex) {
                close();
                throw ex;
            }
        }
        if (NETConfig.NET_DEBUG)
            Log.i(TAG, "Server opened.");
    }

    /**
     * Accepts any new connections and reads or writes any pending data for the current connections.
     *
     * @param timeout Wait for up to the specified milliseconds for a connection to be ready to process. May be zero to return
     *                immediately if there are no connections to process.
     */
    public void update(int timeout) throws IOException {
        updateThread = Thread.currentThread();
        synchronized (updateLock) { // Blocks to avoid a select while the selector is used to bind the server connection.
        }
        long startTime = System.currentTimeMillis();
        int select = 0;
        if (timeout > 0) {
            select = selector.select(timeout);
        } else {
            select = selector.selectNow();
        }
        if (select == 0) {
            emptySelects++;
            if (emptySelects == 100) {
                emptySelects = 0;
                // NIO freaks and returns immediately with 0 sometimes, so try to keep from hogging the CPU.
                long elapsedTime = System.currentTimeMillis() - startTime;
                try {
                    if (elapsedTime < 25) Thread.sleep(25 - elapsedTime);
                } catch (InterruptedException ex) {
                }
            }
        } else {
            emptySelects = 0;
            Set<SelectionKey> keys = selector.selectedKeys();
            synchronized (keys) {
                UdpConnection udp = this.udp;
                outer:
                for (Iterator<SelectionKey> iter = keys.iterator(); iter.hasNext(); ) {
                    SelectionKey selectionKey = iter.next();
                    iter.remove();
                    Connection fromConnection = (Connection) selectionKey.attachment();
                    try {
                        int ops = selectionKey.readyOps();

                        if (fromConnection != null) { // Must be a TCP read or write operation.
                            if (udp != null && fromConnection.udpRemoteAddress == null) {
                                fromConnection.close();
                                continue;
                            }
                            if ((ops & SelectionKey.OP_READ) == SelectionKey.OP_READ) {
                                try {
                                    byte[] tcpdata = fromConnection.tcp.readData(fromConnection);
                                    if (tcpdata == null) break;
                                    if (NETConfig.NET_DEBUG) {
                                        Log.i(TAG, fromConnection + " received TCP: " + new String(tcpdata));
                                    }
                                    fromConnection.notifyReceived(tcpdata);
                                } catch (IOException ex) {
                                    if (NETConfig.NET_DEBUG) {
                                        Log.i(TAG, "Unable to read TCP from: " + fromConnection, ex);
                                    }
                                    fromConnection.close();
                                }
                            }
                            if ((ops & SelectionKey.OP_WRITE) == SelectionKey.OP_WRITE) {
                                try {
                                    fromConnection.tcp.writeOperation();
                                } catch (IOException ex) {
                                    if (NETConfig.NET_DEBUG) {
                                        Log.i(TAG, "Unable to write TCP to connection: " + fromConnection, ex);
                                    }
                                    fromConnection.close();
                                }
                            }
                            continue;
                        }

                        if ((ops & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT) {
                            ServerSocketChannel serverChannel = this.serverChannel;
                            if (serverChannel == null) continue;
                            try {
                                SocketChannel socketChannel = serverChannel.accept();
                                if (socketChannel != null) acceptOperation(socketChannel);
                            } catch (IOException ex) {
                                if (NETConfig.NET_DEBUG)
                                    Log.i(TAG, "Unable to accept new connection.", ex);
                            }
                            continue;
                        }

                        // Must be a UDP read operation.
                        if (udp == null) {
                            selectionKey.channel().close();
                            continue;
                        }
                        InetSocketAddress fromAddress;
                        try {
                            fromAddress = udp.readFromAddress();
                        } catch (IOException ex) {
                            if (NETConfig.NET_DEBUG) Log.i(TAG, "Error reading UDP data.", ex);
                            continue;
                        }
                        if (fromAddress == null)
                            continue;
                        else
                            addUDPConnection(fromAddress);

                        byte[] udpdata = udp.readData(fromConnection);
                        if (udpdata != null) {
                            if (NETConfig.NET_DEBUG) {
                                Log.i(TAG, fromConnection + " received UDP: " + new String(udpdata));
                            }
                            for (Listener listener : udplisteners) {
                                listener.received(fromConnection, udpdata);
                            }
                            continue;
                        }
                    } catch (CancelledKeyException ex) {
                        if (fromConnection != null)
                            fromConnection.close();
                        else
                            selectionKey.channel().close();
                    }
                }
            }
        }
    }

    public void run() {
        if (NETConfig.NET_DEBUG) Log.i(TAG, "Server thread started.");
        shutdown = false;
        while (!shutdown) {
            try {
                update(250);
            } catch (IOException ex) {
                if (NETConfig.NET_DEBUG) Log.i(TAG, "Error updating server connections.", ex);
                close();
            }
        }
        if (NETConfig.NET_DEBUG) Log.i(TAG, "Server thread stopped.");
    }

    public void start() {
        new Thread(this, "Server").start();
    }

    public void stop() {
        if (shutdown) return;
        close();
        if (NETConfig.NET_DEBUG) Log.i(TAG, "Server thread stopping.");
        shutdown = true;
    }

    private void acceptOperation(SocketChannel socketChannel) {
        Connection connection = newConnection();
        connection.initialize(writeBufferSize, readBufferSize);
        connection.endPoint = this;
        UdpConnection udp = this.udp;
        if (udp != null) connection.udp = udp;
        try {
            SelectionKey selectionKey = connection.tcp.accept(selector, socketChannel);
            selectionKey.attach(connection);
            connection.id = connection.getRemoteAddressTCP().getHostName() + ":" + connection.getRemoteAddressTCP().getPort();
            connection.setConnected(true);
            connection.addListener(dispatchListener);

            if (udp == null)
                addTCPConnection(connection);

            if (udp == null) connection.notifyConnected();
        } catch (IOException ex) {
            connection.close();
            if (NETConfig.NET_DEBUG) Log.i(TAG, "Unable to accept TCP connection.", ex);
        }
    }

    /**
     * Allows the connections used by the server to be subclassed. This can be useful for storage per connection without an
     * additional lookup.
     */
    protected Connection newConnection() {
        return new Connection();
    }

    private void addTCPConnection(Connection connection) {
        if (tcpconnections != null) {
            if (tcpconnections.contains(connection)) return;
            tcpconnections.add(connection);
        }
    }

    private void addUDPConnection(InetSocketAddress address) {
        if (udpconnections != null) {
            for (InetSocketAddress addr : udpconnections) {
                if (addr.getHostName().equals(address.getHostName()) && (addr.getPort() == address.getPort()))
                    break;
            }
            if (udpconnections.contains(address)) return;
            udpconnections.add(address);
        }
    }

    void removeConnection(Connection connection) {
        if (tcpconnections != null) {
            if (!tcpconnections.contains(connection)) return;
            tcpconnections.remove(connection);
        }
    }

    // BOZO - Provide mechanism for sending to multiple clients without serializing multiple times.

    public void sendToAllTCP(byte[] data) {
        if (tcpconnections != null) {
            for (Connection connection : tcpconnections) {
                int length = connection.sendTCP(data);
                if (length == 0 && data.length > 0) {
                    tcpconnections.remove(connection);
                }
            }
        }
    }

    public void sendToAllExceptTCP(String iptoport, byte[] data) {
        if (tcpconnections != null) {
            for (Connection connection : tcpconnections) {
                if (connection.id.equals(iptoport)) {
                    int length = connection.sendTCP(data);
                    if (length == 0 && data.length > 0) {
                        tcpconnections.remove(connection);
                    }
                }

            }
        }
    }

    public void sendToTCP(String ip2port, byte[] data) {
        if (tcpconnections != null) {
            for (Connection connection : tcpconnections) {
                if (connection.id.equals(ip2port)) {
                    int length = connection.sendTCP(data);
                    if (length == 0 && data.length > 0) {
                        tcpconnections.remove(connection);
                    }
                    break;
                }
            }
        }
    }

    public void sendToAllUDP(byte[] data) {
        if (udpconnections != null) {
            for (InetSocketAddress addr : udpconnections) {
                try {
                    this.udp.sendData(null, data, addr);
                } catch (IOException e) {
                    e.printStackTrace();
                    udpconnections.remove(addr);
                }
            }
        }
    }

    public void sendToAllExceptUDP(InetSocketAddress exaddr, byte[] data) {
        if (udpconnections != null) {
            for (InetSocketAddress addr : udpconnections) {
                if (!addr.getHostName().equals(exaddr.getHostName()) && !(addr.getPort() == exaddr.getPort()))
                    try {
                        this.udp.sendData(null, data, addr);
                    } catch (IOException e) {
                        e.printStackTrace();
                        udpconnections.remove(addr);
                    }
            }
        }
    }

    public void sendToUDP(InetSocketAddress addr, byte[] data) {
        try {
            this.udp.sendData(null, data, addr);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addUDPListen(Listener listener) {
        if (udplisteners != null && !udplisteners.contains(listener)) {
            udplisteners.add(listener);
        }
    }

    public void removeUDPListen(Listener listener) {
        if (udplisteners != null && udplisteners.contains(listener)) {
            udplisteners.remove(listener);
        }
    }

    public void addListener(Listener listener) {
        if (listener == null) throw new IllegalArgumentException("listener cannot be null.");
        synchronized (listenerLock) {
            if (listeners != null) {
                if (listeners.contains(listener)) return;
                listeners.add(listener);
            }
        }
        if (NETConfig.NET_DEBUG)
            Log.i(TAG, "Server listener added: " + listener.getClass().getName());
    }

    public void removeListener(Listener listener) {
        if (listener == null) throw new IllegalArgumentException("listener cannot be null.");
        synchronized (listenerLock) {
            if (listeners != null) {
                if (listeners.contains(listener)) return;
                listeners.remove(listener);
            }
        }
        if (NETConfig.NET_DEBUG)
            Log.i(TAG, "Server listener removed: " + listener.getClass().getName());
    }

    /**
     * Closes all open connections and the server port(s).
     */
    public void close() {
        if (tcpconnections != null) {
            for (Connection connection : tcpconnections) {
                connection.close();
            }
            tcpconnections.clear();
        }

        if (udpconnections != null) {
            udpconnections.clear();
        }
        ServerSocketChannel serverChannel = this.serverChannel;
        if (serverChannel != null) {
            try {
                serverChannel.close();
                if (NETConfig.NET_DEBUG) Log.i(TAG, "Server closed.");
            } catch (IOException ex) {
                if (NETConfig.NET_DEBUG) Log.i(TAG, "Unable to close server.", ex);
            }
            this.serverChannel = null;
        }

        UdpConnection udp = this.udp;
        if (udp != null) {
            udp.close();
            this.udp = null;
        }

        synchronized (updateLock) { // Blocks to avoid a select while the selector is used to bind the server connection.
        }
        // Select one last time to complete closing the socket.
        selector.wakeup();
        try {
            selector.selectNow();
        } catch (IOException ignored) {
        }
    }

    /**
     * Releases the resources used by this server, which may no longer be used.
     */
    public void dispose() throws IOException {
        close();
        selector.close();
    }

    public Thread getUpdateThread() {
        return updateThread;
    }

    /**
     * Returns the current connections. The array returned should not be modified.
     */
    public ArrayList<Connection> getTCPConnections() {
        return tcpconnections;
    }

    public ArrayList<InetSocketAddress> getUDPConnections() {
        return udpconnections;
    }
}
