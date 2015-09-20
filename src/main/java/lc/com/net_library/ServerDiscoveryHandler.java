package lc.com.net_library;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public interface ServerDiscoveryHandler {
	/** This implementation of {@link ServerDiscoveryHandler} is responsible for providing the {@link Server} with it's default
	 * behavior. */
	public static final ServerDiscoveryHandler DEFAULT = new ServerDiscoveryHandler() {
		private ByteBuffer emptyBuffer = ByteBuffer.allocate(0);

		@Override
		public boolean onDiscoverHost (DatagramChannel datagramChannel, InetSocketAddress fromAddress)
			throws IOException {
			datagramChannel.send(emptyBuffer, fromAddress);
			return true;
		}
	};

	/** Called when the {@link Server} receives a { DiscoverHost} packet.
	 * @param fromAddress {@link InetSocketAddress} the { DiscoverHost} came from
	 * @return true if a response was sent to {@code fromAddress}, false otherwise
	 * @throws IOException from the use of {@link DatagramChannel#send(ByteBuffer, java.net.SocketAddress)} */
	public boolean onDiscoverHost(DatagramChannel datagramChannel, InetSocketAddress fromAddress)
		throws IOException;
}
