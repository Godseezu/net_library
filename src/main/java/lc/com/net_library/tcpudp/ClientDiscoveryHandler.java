package lc.com.net_library.tcpudp;

import java.net.DatagramPacket;


public interface ClientDiscoveryHandler {

	/**
	 * This implementation of the {@link ClientDiscoveryHandler} is responsible
	 * for providing the {@link Client} with it's default behavior.
	 */
	public static final ClientDiscoveryHandler DEFAULT = new ClientDiscoveryHandler() {

		@Override
		public DatagramPacket onRequestNewDatagramPacket() {
			return new DatagramPacket(new byte[0], 0);
		}

		@Override
		public void onDiscoveredHost(DatagramPacket datagramPacket) {
			//
		}

		@Override
		public void onFinally() {
			//
		}

	};

	/**
	 * Implementations of this method should return a new {@link DatagramPacket}
	 * that the {@link Client} will use to fill with the incoming packet data
	 * sent by the {@link ServerDiscoveryHandler}.
	 * 
	 * @return a new {@link DatagramPacket}
	 */
	public DatagramPacket onRequestNewDatagramPacket();

	/**
	 * Called when the {@link Client} discovers a host.
	 * 
	 * @param datagramPacket
	 *            the same {@link DatagramPacket} from
	 *            {@link #onRequestNewDatagramPacket()}, after being filled with
	 *            the incoming packet data.
	 *
	 */
	public void onDiscoveredHost(DatagramPacket datagramPacket);

	/**
	 *
	 */
	public void onFinally();

}
