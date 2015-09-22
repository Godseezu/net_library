package lc.com.net_library.tcpudp.util;

import lc.com.net_library.tcpudp.Connection;
import lc.com.net_library.tcpudp.Listener;

abstract public class TcpIdleSender extends Listener {
	boolean started;

	public void idle (Connection connection) {
		if (!started) {
			started = true;
			start();
		}
		byte[] data = next();
		if (data == null)
			connection.removeListener(this);
		else
			connection.sendTCP(data);
	}

	/** Called once, before the first send. Subclasses can override this method to send something so the receiving side expects
	 * subsequent objects. */
	protected void start () {
	}

	/** Returns the next object to send, or null if no more objects will be sent. */
	abstract protected byte[] next ();
}
