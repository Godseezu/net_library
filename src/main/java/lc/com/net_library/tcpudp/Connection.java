package lc.com.net_library.tcpudp;

import android.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;

import lc.com.net_library.tcpudp.util.NETConfig;

/**
 * TCP UDP连接进一步封装
 * @author  Lin
 */
public class Connection {
	private static final String TAG=NETConfig.TAG;
	EndPoint endPoint;
	TcpConnection tcp;
	UdpConnection udp;
	InetSocketAddress udpRemoteAddress;
	public String id;
	private ArrayList<Listener> listeners =new ArrayList<>();
	private Object listenerLock = new Object();
	private int lastPingID;
	private long lastPingSendTime;
	private int returnTripTime;
	volatile boolean isConnected;

	protected Connection () {
	}

	void initialize (int writeBufferSize, int readBufferSize) {
		tcp = new TcpConnection();
	}

	/** Returns true if this connection is connected to the remote end. Note that a connection can become disconnected at any time. */
	public boolean isConnected () {
		return isConnected;
	}
	


	/** Sends the object over the network using TCP.
	 * @return The number of bytes sent.
	 *  */
	public int sendTCP (byte [] data) {
		if (data == null) throw new IllegalArgumentException("data cannot be null.");
		try {
			int length = tcp.sendData(this, data);
			if (length == 0) {
				if (NETConfig.NET_DEBUG) Log.i(TAG, this + " TCP had nothing to send.");
			} else if (NETConfig.NET_DEBUG) {
					Log.i(TAG, this + " sent TCP: length" + " (" + length + ")");
			}
			return length;
		} catch (IOException ex) {
			if (NETConfig.NET_DEBUG) Log.i(TAG, "Unable to send TCP with connection: " + this, ex);
			close();
			return 0;
		}
	}

	/** Sends the object over the network using UDP.
	 * @return The number of bytes sent.
	 * @throws IllegalStateException if this connection was not opened with both TCP and UDP. */
	public int sendUDP (byte[]  data) {
		if (data == null) throw new IllegalArgumentException("data cannot be null.");
		SocketAddress address = udpRemoteAddress;
		if (address == null && udp != null) address = udp.connectedAddress;
		if (address == null && isConnected) throw new IllegalStateException("Connection is not connected via UDP.");

		try {
			if (address == null) throw new SocketException("Connection is closed.");
			int length= udp.sendData(this, data, address);

			if (length == 0) {
				if (NETConfig.NET_DEBUG) Log.i(TAG, this + " UDP had nothing to send.");
			} else if (NETConfig.NET_DEBUG) {
				if (length != -1) {
						Log.i(TAG, this + " sent UDP: length"  + " (" + length + ")");
				} else
					Log.i(TAG, this + " was unable to send, UDP socket buffer full.");
			}
			return length;
		} catch (IOException ex) {
			if (NETConfig.NET_DEBUG) Log.i(TAG, "Unable to send UDP with connection: " + this, ex);
			close();
			return 0;
		}
	}

	public void close () {
		boolean wasConnected = isConnected;
		isConnected = false;
		tcp.close();
		if (udp != null && udp.connectedAddress != null) {
			udp.close();
			notifyDisconnected();
		}

		if (wasConnected) {
			notifyDisconnected();
		}
		if (NETConfig.NET_DEBUG) Log.i(TAG, this + " disconnected.");
		setConnected(false);
	}


	/** An empty object will be sent if the TCP connection has not sent an object within the specified milliseconds. Periodically
	 * sending a keep alive ensures that an abnormal close is detected in a reasonable amount of time (see {@link #setTimeout(int)}
	 * ). Also, some network hardware will close a TCP connection that ceases to transmit for a period of time (typically 1+
	 * minutes). Set to zero to disable. Defaults to 8000. */
	public void setKeepAliveTCP (int keepAliveMillis) {
		tcp.keepAliveMillis = keepAliveMillis;
	}

	/** If the specified amount of time passes without receiving an object over TCP, the connection is considered closed. When a TCP
	 * socket is closed normally, the remote end is notified immediately and this timeout is not needed. However, if a socket is
	 * closed abnormally (eg, power loss), KryoNet uses this timeout to detect the problem. The timeout should be set higher than
	 * the {@link #setKeepAliveTCP(int) TCP keep alive} for the remote end of the connection. The keep alive ensures that the remote
	 * end of the connection will be constantly sending objects, and setting the timeout higher than the keep alive allows for
	 * network latency. Set to zero to disable. Defaults to 12000. */
	public void setTimeout (int timeoutMillis) {
		tcp.timeoutMillis = timeoutMillis;
	}

	/** If the listener already exists, it is not added again. */
	public void addListener (Listener listener) {
		if (listener == null) throw new IllegalArgumentException("listener cannot be null.");
		synchronized (listenerLock) {
			if (listeners != null) {
				if (listeners.contains(listener)) return;
				listeners.add(listener);
			}
		}
		if (NETConfig.NET_DEBUG) Log.i(TAG, "Connection listener added: " + listener.getClass().getName());
	}

	public void removeListener (Listener listener) {
		if (listener == null) throw new IllegalArgumentException("listener cannot be null.");
		synchronized (listenerLock) {
			if(listeners!=null){
				if(!listeners.contains(listener)) return;
				listeners.remove(listener);
			}
		}
		if (NETConfig.NET_DEBUG) Log.i(TAG, "Connection listener removed: " + listener.getClass().getName());
	}

	void notifyConnected () {
		if (NETConfig.NET_DEBUG) {
			SocketChannel socketChannel = tcp.socketChannel;
			if (socketChannel != null) {
				Socket socket = tcp.socketChannel.socket();
				if (socket != null) {
					InetSocketAddress remoteSocketAddress = (InetSocketAddress)socket.getRemoteSocketAddress();
					if (remoteSocketAddress != null) Log.i(TAG, this + " connected: " + remoteSocketAddress.getAddress());
				}
			}
		}
		for(Listener listener:listeners){
			listener.connected(this);
		}
	}

	void notifyDisconnected () {
		for(Listener listener:listeners){
			listener.disconnected(this);
		}
	}


	void notifyReceived (byte[] data) {
		for(Listener listener:listeners){
			listener.received(this,data);
		}
	}

	/** Returns the local {@link Client} or {@link Server} to which this connection belongs. */
	public EndPoint getEndPoint () {
		return endPoint;
	}

	/** Returns the IP address and port of the remote end of the TCP connection, or null if this connection is not connected. */
	public InetSocketAddress getRemoteAddressTCP () {
		SocketChannel socketChannel = tcp.socketChannel;
		if (socketChannel != null) {
			Socket socket = tcp.socketChannel.socket();
			if (socket != null) {
				return (InetSocketAddress)socket.getRemoteSocketAddress();
			}
		}
		return null;
	}

	/** Returns the IP address and port of the remote end of the UDP connection, or null if this connection is not connected. */
	public InetSocketAddress getRemoteAddressUDP () {
		InetSocketAddress connectedAddress = udp.connectedAddress;
		if (connectedAddress != null) return connectedAddress;
		return udpRemoteAddress;
	}

	/** Workaround for broken NIO networking on Android 1.6. If true, the underlying NIO buffer is always copied to the beginning of
	 * the buffer before being given to the SocketChannel for sending. The Harmony SocketChannel implementation in Android 1.6
	 * ignores the buffer position, always copying from the beginning of the buffer. This is fixed in Android 2.0+. */
	public void setBufferPositionFix (boolean bufferPositionFix) {
		tcp.bufferPositionFix = bufferPositionFix;
	}


	/** Returns the number of bytes that are waiting to be written to the TCP socket, if any. */
	public int getTcpWriteBufferSize () {
		return tcp.writeBuffer.position();
	}

	void setConnected (boolean isConnected) {
		this.isConnected = isConnected;
	}
}
