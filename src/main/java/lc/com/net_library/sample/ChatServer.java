
package lc.com.net_library.sample;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.NoCopySpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;

import lc.com.net_library.Connection;
import lc.com.net_library.FrameworkMessage;
import lc.com.net_library.Listener;
import lc.com.net_library.R;
import lc.com.net_library.Server;
import lc.com.net_library.util.NETConfig;
import lc.com.net_library.util.NETUtil;

public class ChatServer extends Activity implements View.OnClickListener{
    static final String TAG=NETConfig.TAG;
    Context mContext;
    EditText etIP;
    EditText etPort;
    Button btnConnnect;
    Button btnClose;
    Button btnSend;
    TextView tvName;
    TextView tvMessage;
    Server server;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext=this;
        setContentView(R.layout.net_server_layout);
        initView();
        initNet();
        initEvnet();
    }

    private void initView() {
        etIP = (EditText) findViewById(R.id.net_server_ip);
        String ip= NETUtil.getIP(mContext);
        if(ip!=null){
            etIP.setText(ip);
        }
        etPort = (EditText) findViewById(R.id.net_server_port);

        etPort.setText(String.valueOf(NETConfig.NET_PORT));
        btnClose = (Button) findViewById(R.id.net_server_close);
        btnConnnect = (Button) findViewById(R.id.net_server_connect);
        btnSend = (Button) findViewById(R.id.net_client_send);
        tvName = (TextView) findViewById(R.id.net_server_name);
        tvMessage = (TextView) findViewById(R.id.net_server_message);
    }

    private void initNet() {
        server = new Server();

        server.addListener(new Listener() {
            public void received(Connection c, byte [] data) {
                Log.i(TAG, "Connect succeful: remoteIP=" + c.getRemoteAddressTCP().getHostName());
            }

            public void disconnected(Connection c) {
                btnConnnect.post(new Runnable() {
                    @Override
                    public void run() {
                        btnConnnect.setClickable(true);
                    }
                });
                Log.i(TAG,"disconnect");

            }
        });

    }

    private void initEvnet() {
        btnConnnect.setOnClickListener(this);
        btnClose.setOnClickListener(this);
        btnSend.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
         if(view==btnConnnect){
             try {
                 initNet();
                 server.bind(NETConfig.NET_PORT);
                 server.addListener(new Listener(){
                     @Override
                     public void received(Connection connection, byte[] data) {
                         Log.i(TAG,"UDP RECEIVE  SUCESSFUL");
                     }
                 });
                 server.start();
             }catch (Exception ex){

             }
         }else if(view==btnClose){
             server.stop();
             server=null;
         }else if(view==btnSend){
             if(server!=null){
                 new Thread("Send") {
                     public void run() {
                         try {
                             FrameworkMessage.RegisterTCP mregisterTCP=new FrameworkMessage.RegisterTCP();
                             if(server!=null)
                             server.sendToAllTCP(mregisterTCP.getByte());
                         } catch (Exception ex) {
                             ex.printStackTrace();
                             Log.i(TAG, "send failed:" + ex.getMessage());
                         }
                     }
                 }.start();


             }
         }
    }
}
