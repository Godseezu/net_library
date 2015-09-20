package lc.com.net_library;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

import lc.com.net_library.util.NETConfig;

/**
 * UDP 封装  基于byte [] 的数据流，基于NIO事件注册，系统底层通知的架构
 *
 * @author Lin
 */
class UdpConnection {
    static final String TAG = NETConfig.TAG;
    InetSocketAddress connectedAddress;
    DatagramChannel datagramChannel;
    int keepAliveMillis = 19000;
    final ByteBuffer readBuffer, writeBuffer;
    private SelectionKey selectionKey;
    private final Object writeLock = new Object();
    private long lastCommunicationTime;
    int bufferSize = 1024;
    int recevieLength=0;

    public UdpConnection() {
        readBuffer = ByteBuffer.allocate(bufferSize);
        writeBuffer = ByteBuffer.allocateDirect(bufferSize);
    }

    public void bind(Selector selector, InetSocketAddress localPort) throws IOException {
        close();
        readBuffer.clear();
        writeBuffer.clear();
        try {
            datagramChannel = selector.provider().openDatagramChannel();
            datagramChannel.socket().bind(localPort);
            datagramChannel.configureBlocking(false);
            selectionKey = datagramChannel.register(selector, SelectionKey.OP_READ);

            lastCommunicationTime = System.currentTimeMillis();
        } catch (IOException ex) {
            close();
            throw ex;
        }
    }

    public void connect(Selector selector, InetSocketAddress remoteAddress) throws IOException {
        close();
        readBuffer.clear();
        writeBuffer.clear();
        try {
            datagramChannel = selector.provider().openDatagramChannel();
                datagramChannel.socket().bind(new InetSocketAddress(NETConfig.NET_PORT));
                datagramChannel.socket().connect(remoteAddress);

            datagramChannel.configureBlocking(false);

            selectionKey = datagramChannel.register(selector, SelectionKey.OP_READ);

            lastCommunicationTime = System.currentTimeMillis();

            connectedAddress = remoteAddress;
        } catch (IOException ex) {
            close();
            IOException ioEx = new IOException("Unable to connect to: " + remoteAddress);
            ioEx.initCause(ex);
            throw ioEx;
        }
    }

    public InetSocketAddress readFromAddress() throws IOException {
        readBuffer.clear();
        recevieLength=0;
        DatagramChannel datagramChannel = this.datagramChannel;
        if (datagramChannel == null) throw new SocketException("Connection is closed.");
        lastCommunicationTime = System.currentTimeMillis();
        InetSocketAddress mupdAdr=null;
        /**
         * datagramChannel.receive(readBuffer)返回InetSocketAddress地址的API无效,关键是是否绑定远程地址，如没有绑定则有效
         */
//        if(!isConnnectTarget) {
//             mupdAdr = (InetSocketAddress) datagramChannel.receive(readBuffer);
//        }
        mupdAdr=(InetSocketAddress)datagramChannel.socket().getRemoteSocketAddress();
        if(mupdAdr!=null){
            byte [] temp=new byte[bufferSize];
            DatagramPacket mreceiveddp=new DatagramPacket(temp,temp.length);
            recevieLength=datagramChannel.read(readBuffer);
            if(recevieLength>0) {
                mupdAdr=(InetSocketAddress)datagramChannel.socket().getRemoteSocketAddress();
            }
        }else{
            mupdAdr = (InetSocketAddress) datagramChannel.receive(readBuffer);
        }
        return mupdAdr;
    }

    public byte[] readData(Connection connection) {
        byte[] data=null;
        try {
            readBuffer.flip();

            if(connectedAddress!=null){
                 data = new byte[recevieLength];
            }else{
                data = new byte[readBuffer.limit()];
            }
            readBuffer.get(data, 0, data.length);
            recevieLength=0;

        } catch (Exception ex) {
            Log.e(TAG, ex.getMessage());
        } finally {
            readBuffer.clear();
            return data;
        }
    }

    /**
     * This method is thread safe.
     */
    public int sendData(Connection connection, byte[] data, SocketAddress address) throws IOException {
        DatagramChannel datagramChannel = this.datagramChannel;
        if (datagramChannel == null) throw new SocketException("Connection is closed.");
        synchronized (writeLock) {
            try {
                writeBuffer.clear();
                writeBuffer.put(data);
                writeBuffer.flip();
                int length = writeBuffer.limit();
                datagramChannel.send(writeBuffer, address);

                lastCommunicationTime = System.currentTimeMillis();

                boolean wasFullWrite = !writeBuffer.hasRemaining();
                return wasFullWrite ? length : -1;
            } finally {
                writeBuffer.clear();
            }
        }
    }

    public void close() {
        connectedAddress = null;
        try {
            if (datagramChannel != null) {
                datagramChannel.close();
                datagramChannel = null;
                if (selectionKey != null) selectionKey.selector().wakeup();
            }
        } catch (IOException ex) {
            if (NETConfig.NET_DEBUG) Log.i(TAG, "Unable to close UDP connection.", ex);
        }
    }

    public boolean needsKeepAlive(long time) {
        return connectedAddress != null && keepAliveMillis > 0 && time - lastCommunicationTime > keepAliveMillis;
    }
}
