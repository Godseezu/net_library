package lc.com.net_library.rmi;

/** Thrown when a method with a return value is invoked on a remote object and the response is not received with the
 * {@link RemoteObject#setResponseTimeout(int) response timeout}.
 *  */
public class TimeoutException extends RuntimeException {
	public TimeoutException () {
		super();
	}

	public TimeoutException (String message, Throwable cause) {
		super(message, cause);
	}

	public TimeoutException (String message) {
		super(message);
	}

	public TimeoutException (Throwable cause) {
		super(cause);
	}
}
