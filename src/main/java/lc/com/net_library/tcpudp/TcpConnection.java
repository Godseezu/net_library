package lc.com.net_library.tcpudp;

import android.util.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import lc.com.net_library.tcpudp.util.NETConfig;


/**
 * TCP封装  基于byte[] 数据流,基于NIO事件注册，系统底层通知的架构
 * @author Lin
 */
class TcpConnection {
	static private final String TAG=NETConfig.TAG;
	static private final int IPTOS_LOWDELAY = 0x10;

	SocketChannel socketChannel;
	int keepAliveMillis = 8000;
	final ByteBuffer readBuffer, writeBuffer;
	boolean bufferPositionFix;
	int timeoutMillis = 12000;
	float idleThreshold = 0.1f;
	int writeBufferSize=1024;
	int readBufferSize=1024;
	int sendLength=-1;

	private SelectionKey selectionKey;
	private volatile long lastWriteTime, lastReadTime;
	private final Object writeLock = new Object();

	public TcpConnection ( ) {
		writeBuffer = ByteBuffer.allocate(writeBufferSize);
		readBuffer = ByteBuffer.allocate(readBufferSize);
		readBuffer.flip();
	}

	public SelectionKey accept (Selector selector, SocketChannel socketChannel) throws IOException {
		writeBuffer.clear();
		readBuffer.clear();
		readBuffer.flip();
		try {
			this.socketChannel = socketChannel;
			socketChannel.configureBlocking(false);
			Socket socket = socketChannel.socket();
			socket.setTcpNoDelay(true);

			selectionKey = socketChannel.register(selector, SelectionKey.OP_READ);

			if (NETConfig.NET_DEBUG) {
				Log.i(TAG, "Port " + socketChannel.socket().getLocalPort() + "/TCP connected to: "
						+ socketChannel.socket().getRemoteSocketAddress());
			}

			lastReadTime = lastWriteTime = System.currentTimeMillis();

			return selectionKey;
		} catch (IOException ex) {
			close();
			throw ex;
		}
	}

	public void connect (Selector selector, SocketAddress remoteAddress, int timeout) throws IOException {
		close();
		writeBuffer.clear();
		readBuffer.clear();
		readBuffer.flip();
		try {
			SocketChannel socketChannel = selector.provider().openSocketChannel();
			Socket socket = socketChannel.socket();
			socket.setTcpNoDelay(true);
			// socket.setTrafficClass(IPTOS_LOWDELAY);
			socket.bind(new InetSocketAddress(NETConfig.NET_PORT));
			socket.connect(remoteAddress, timeout); // Connect using blocking mode for simplicity.
			socketChannel.configureBlocking(false);
			this.socketChannel = socketChannel;

			selectionKey = socketChannel.register(selector, SelectionKey.OP_READ);
			selectionKey.attach(this);

			if (NETConfig.NET_DEBUG) {
				Log.i(TAG, "Port " + socketChannel.socket().getLocalPort() + "/TCP connected to: "
						+ socketChannel.socket().getRemoteSocketAddress());
			}

			lastReadTime = lastWriteTime = System.currentTimeMillis();
		} catch (IOException ex) {
			close();
			IOException ioEx = new IOException("Unable to connect to: " + remoteAddress);
			ioEx.initCause(ex);
			throw ioEx;
		}
	}

	public byte[] readData (Connection connection) throws IOException {
		SocketChannel socketChannel = this.socketChannel;
		if (socketChannel == null) throw new SocketException("Connection is closed.");
		    readBuffer.clear();
			int bytesRead = socketChannel.read(readBuffer);
		    readBuffer.flip();
			if (bytesRead == -1) throw new SocketException("Connection is closed.");
			lastReadTime = System.currentTimeMillis();
		int dataLenght = readBuffer.limit();
		byte[] data=new byte[dataLenght];
		readBuffer.get(data,0,dataLenght);
		return data;
	}

	public void writeOperation () throws IOException {
		synchronized (writeLock) {
			if (writeToSocket()) {
				// Write successful, clear OP_WRITE.
				selectionKey.interestOps(SelectionKey.OP_READ);
			}
			lastWriteTime = System.currentTimeMillis();
		}
	}

	private boolean writeToSocket () throws IOException {
		SocketChannel socketChannel = this.socketChannel;
		if (socketChannel == null) throw new SocketException("Connection is closed.");

		ByteBuffer buffer = writeBuffer;
		buffer.flip();
		while (buffer.hasRemaining()) {
			if (bufferPositionFix) {
				buffer.compact();
				buffer.flip();
			}
			sendLength=socketChannel.write(buffer);
			if (sendLength == 0) break;
		}
		buffer.compact();

		return buffer.position() == 0;
	}

	/** This method is thread safe. */
	public int sendData (Connection connection,byte [] data) throws IOException {
		SocketChannel socketChannel = this.socketChannel;
		if (socketChannel == null) throw new SocketException("Connection is closed.");
		synchronized (writeLock) {
			writeBuffer.put(data);
			if (!writeToSocket()) {
				// A partial write, set OP_WRITE to be notified when more writing can occur.
				selectionKey.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
			} else {
				// Full write, wake up selector so idle event will be fired.
				selectionKey.selector().wakeup();
			}

			if (NETConfig.NET_DEBUG) {
				float percentage = writeBuffer.position() / (float)writeBuffer.capacity();
				if ( percentage > 0.25f)
					Log.i(TAG, connection + " TCP write buffer is approaching capacity: " + percentage + "%");
			}
			int limit = sendLength;
			sendLength=-1;
			lastWriteTime = System.currentTimeMillis();
			return limit;
		}
	}

	public void close () {
		try {
			if (socketChannel != null) {
				socketChannel.close();
				socketChannel = null;
				if (selectionKey != null) selectionKey.selector().wakeup();
			}
		} catch (IOException ex) {
			if (NETConfig.NET_DEBUG) Log.i(TAG, "Unable to close TCP connection.", ex);
		}
	}

	public boolean needsKeepAlive (long time) {
		return socketChannel != null && keepAliveMillis > 0 && time - lastWriteTime > keepAliveMillis;
	}

	public boolean isTimedOut (long time) {
		return socketChannel != null && timeoutMillis > 0 && time - lastReadTime > timeoutMillis;
	}
}
