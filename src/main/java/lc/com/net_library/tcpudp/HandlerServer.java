package lc.com.net_library.tcpudp;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import java.io.IOException;
import java.net.InetSocketAddress;

import lc.com.net_library.tcpudp.util.NETConfig;

/**
 * 把Server类API放在HandlerThread类里面的消息队列执行，避免Socket相关API的执行发生阻塞，影响用户体验
 * Created by Lin on 2015/9/21.
 */
public class HandlerServer {
    private HandlerThread mHandlerThread;
    private Handler mHandler;
    private Server mServer;
    private OnReceiveListen mOnReceiveListen;
    private final String UDP_ADDR = "udp_addr";

    public interface OnReceiveListen {
        void OnReceive(byte[] data);
    }

    public HandlerServer() {
        mHandlerThread = new HandlerThread("HandlerClient");
        mHandlerThread.start();
        mHandler = new Handler(mHandlerThread.getLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(Message message) {
                switch (message.what) {
                    case NETConfig.NETEvent.SOCKET_EVENT_BIND_TCP:
                        mServer = new Server();
                        mServer.addListener(mListen);
                        try {
                            mServer.bind(NETConfig.NET_PORT, -1);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        mServer.start();
                        break;
                    case NETConfig.NETEvent.SOCKET_EVENT_BIND_UDP:
                        mServer = new Server();
                        mServer.addListener(mListen);
                        try {
                            mServer.bind(-1, NETConfig.NET_PORT);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        mServer.start();
                        break;
                    case NETConfig.NETEvent.SOCKET_EVENT_SEND_TCP_DATA:
                        if (mServer != null) {
                            byte[] tcpdata = (byte[]) message.obj;
                            mServer.sendToAllTCP(tcpdata);
                        }
                        break;
                    case NETConfig.NETEvent.SOCKET_EVENT_SEND_UDP_DATA:
                        if (mServer != null) {
                            byte[] udpdata = (byte[]) message.obj;
                            InetSocketAddress targetAddr = (InetSocketAddress) message.getData().get("UDP_ADDR");
                            if (targetAddr != null) {
                                mServer.sendToUDP(targetAddr, udpdata);
                            } else {
                                mServer.sendToAllUDP(udpdata);
                            }
                        }
                        break;
                    case NETConfig.NETEvent.SOCKET_EVENT_CLOSE:
                        if (mServer != null) {
                            mServer.removeListener(mListen);
                            mServer.stop();
                            mServer = null;
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

    public void bindTCP() {
        Message msg = mHandler.obtainMessage();
        msg.what = NETConfig.NETEvent.SOCKET_EVENT_BIND_TCP;
        msg.sendToTarget();
    }

    public void bindUDP() {
        Message msg = mHandler.obtainMessage();
        msg.what = NETConfig.NETEvent.SOCKET_EVENT_BIND_UDP;
        msg.sendToTarget();
    }

    public void sendTCP(byte[] data) {
        Message msg = mHandler.obtainMessage();
        msg.obj = data;
        msg.what = NETConfig.NETEvent.SOCKET_EVENT_SEND_TCP_DATA;
        msg.sendToTarget();
    }

    public void sendUDP(InetSocketAddress addr, byte[] data) {
        Message msg = mHandler.obtainMessage();
        msg.obj = data;
        Bundle mbundle = new Bundle();
        if (addr != null) {
            mbundle.putSerializable(UDP_ADDR, addr);
        }
        msg.what = NETConfig.NETEvent.SOCKET_EVENT_SEND_UDP_DATA;
        msg.sendToTarget();
    }

    public void close() {
        mHandler.removeMessages(NETConfig.NETEvent.SOCKET_EVENT_BIND_UDP);
        mHandler.removeMessages(NETConfig.NETEvent.SOCKET_EVENT_BIND_TCP);
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
