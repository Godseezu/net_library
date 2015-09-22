package lc.com.net_library.tcpudp;

import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import java.io.IOException;

import lc.com.net_library.tcpudp.util.NETConfig;


/**
 * 把Client类API放在HandlerThread类里面的消息队列执行，避免Socket相关API的执行发生阻塞，影响用户体验
 * Created by Lin on 2015/9/21.
 */
public class HandlerClient {
    private HandlerThread mHandlerThread;
    private Handler mHandler;
    private Client mClient;
    private OnReceiveListen mOnReceiveListen;

    public interface OnReceiveListen {
        void OnReceive(byte[] data);
    }

    public HandlerClient() {
        mHandlerThread = new HandlerThread("HandlerClient");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(Message message) {
                switch (message.what) {
                    case NETConfig.NETEvent.SOCKET_EVENT_CONNECT_TCP:

                        mClient = new Client();
                        mClient.start();
                        mClient.addListener(mListen);
                        String tcpip = (String) message.obj;
                        try {
                            mClient.connect(NETConfig.NET_TIMEOUT, tcpip, NETConfig.NET_PORT, -1);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                    case NETConfig.NETEvent.SOCKET_EVENT_CONNECT_UDP:
                        mClient = new Client();
                        mClient.start();
                        mClient.addListener(mListen);
                        String udpip = (String) message.obj;
                        try {
                            mClient.connect(NETConfig.NET_TIMEOUT, udpip, -1, NETConfig.NET_PORT);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                    case NETConfig.NETEvent.SOCKET_EVENT_SEND_TCP_DATA:
                        if (mClient != null) {
                            byte[] tcpdata = (byte[]) message.obj;
                            mClient.sendTCP(tcpdata);
                        }
                        break;
                    case NETConfig.NETEvent.SOCKET_EVENT_SEND_UDP_DATA:
                        if (mClient != null) {
                            byte[] udpdata = (byte[]) message.obj;
                            mClient.sendUDP(udpdata);
                        }
                        break;
                    case NETConfig.NETEvent.SOCKET_EVENT_CLOSE:
                        if (mClient != null) {
                            mClient.removeListener(mListen);
                            mClient.stop();
                            mClient = null;
                        }
                        break;
                    default:
                        break;
                }
                return true;
            }
        });
    }

    public void setOnReceiveListen(OnReceiveListen onreceivelisten) {
        this.mOnReceiveListen = onreceivelisten;
    }

    public void connectTCP(String ip) {
        Message msg = mHandler.obtainMessage();
        msg.obj = ip;
        msg.what = NETConfig.NETEvent.SOCKET_EVENT_CONNECT_TCP;
        msg.sendToTarget();
    }

    public void connectUDP(String ip) {
        Message msg = mHandler.obtainMessage();
        msg.obj = ip;
        msg.what = NETConfig.NETEvent.SOCKET_EVENT_CONNECT_UDP;
        msg.sendToTarget();
    }

    public void sendTCP(byte[] data) {
        Message msg = mHandler.obtainMessage();
        msg.obj = data;
        msg.what = NETConfig.NETEvent.SOCKET_EVENT_SEND_TCP_DATA;
        msg.sendToTarget();
    }

    public void sendUDP(byte[] data) {
        Message msg = mHandler.obtainMessage();
        msg.obj = data;
        msg.what = NETConfig.NETEvent.SOCKET_EVENT_SEND_UDP_DATA;
        msg.sendToTarget();
    }



    public void close() {
        mHandler.removeMessages(NETConfig.NETEvent.SOCKET_EVENT_CONNECT_TCP);
        mHandler.removeMessages(NETConfig.NETEvent.SOCKET_EVENT_CONNECT_UDP);
        mHandler.removeMessages(NETConfig.NETEvent.SOCKET_EVENT_SEND_UDP_DATA);
        mHandler.removeMessages(NETConfig.NETEvent.SOCKET_EVENT_SEND_TCP_DATA);
        Message msg = mHandler.obtainMessage();
        msg.what = NETConfig.NETEvent.SOCKET_EVENT_CLOSE;
        msg.sendToTarget();
    }

    public void destory(){
        if(Build.VERSION.SDK_INT>18) {
            mHandlerThread.quitSafely();
        }else{
            mHandlerThread.quit();
        }
    }

    private Listener mListen = new Listener() {
        @Override
        public void connected(Connection connection) {

        }

        @Override
        public void disconnected(Connection connection) {

        }

        @Override
        public void received(Connection connection, byte[] data) {
            if (mOnReceiveListen != null) {
                mOnReceiveListen.OnReceive(data);
            }

        }
    };
}
