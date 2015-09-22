package lc.com.net_library.tcpudp.util;

/**
 * Created by Administrator on 2015/9/16.
 */
public class NETConfig {
    public static boolean NET_DEBUG = true;
    public static String TAG = "NETConfig";
    public static int NET_PORT = 55555;
    public static int NET_TIMEOUT=5000;

    public static class NETState {
        public static final int SOCKET_INIT = 0x01;
        public static final int SOCKET_BIND = 0x02;
        public static final int SOCKET_CONNECTING = 0x03;
        public static final int SOCKET_CONNECTED = 0x04;
        public static final int SOCKET_SENDDATA = 0x05;
        public static final int SOCKET_RECEDATA = 0x06;
        public static final int SOCKET_CLOSE = 0x07;
    }

    public static class NETEvent {
        public static final int SOCKET_EVENT_CONNECT_TCP = 0x01;
        public static final int SOCKET_EVENT_BIND_TCP = 0x02;
        public static final int SOCKET_EVENT_CONNECT_UDP = 0x03;
        public static final int SOCKET_EVENT_BIND_UDP = 0x04;
        public static final int SOCKET_EVENT_SEND_TCP_DATA = 0x05;
        public static final int SOCKET_EVENT_SEND_UDP_DATA = 0x06;
        public static final int SOCKET_EVENT_CLOSE = 0x07;
    }
}
