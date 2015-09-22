
package lc.com.net_library.tcpudp.sample;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;

import lc.com.net_library.tcpudp.Client;
import lc.com.net_library.tcpudp.Connection;
import lc.com.net_library.tcpudp.FrameworkMessage;
import lc.com.net_library.tcpudp.Listener;
import lc.com.net_library.R;
import lc.com.net_library.tcpudp.util.NETConfig;


public class ChatClient extends Activity implements View.OnClickListener {
    static final String TAG= NETConfig.TAG;
    Client client;
    String name;
    EditText etIP;
    EditText etPort;
    Button btnConnnect;
    Button btnClose;
    Button btnSend;
    TextView tvName;
    TextView tvMessage;
    Context mContext;
    int port=55555;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext=this;
        setContentView(R.layout.net_client_layout);
        initView();
        initNet();
        initEvnet();
    }

    private void initView() {
        etIP = (EditText) findViewById(R.id.net_client_ip);
        etPort = (EditText) findViewById(R.id.net_client_port);
        etPort.setText(String.valueOf(port));
        btnClose = (Button) findViewById(R.id.net_client_close);
        btnConnnect = (Button) findViewById(R.id.net_client_connect);
        btnSend = (Button) findViewById(R.id.net_client_send);
        tvName = (TextView) findViewById(R.id.net_client_name);
        tvMessage = (TextView) findViewById(R.id.net_client_message);
    }

    private void initNet() {
        client = new Client();
        client.start();
        client.addListener(new Listener() {
            @Override
            public void connected(Connection connection) {
                Log.i(TAG, "Connect succeful: remoteIP=" + connection.getRemoteAddressTCP().getHostName());
            }

            @Override
            public void disconnected(Connection connection) {
                btnConnnect.post(new Runnable() {
                    @Override
                    public void run() {
                        btnConnnect.setClickable(true);
                    }
                });
                Log.i(TAG,"disconnect");
            }

            @Override
            public void received(Connection connection,byte[] data) {
               Log.i(TAG,new String(data));
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
        if (view == btnClose) {
            client.close();
        } else if (view == btnConnnect) {
            final String ip=etIP.getText().toString();
            final int port=Integer.valueOf(etPort.getText().toString());
            btnConnnect.setClickable(false);
            new Thread("Connect") {
                public void run() {
                    try {
                        client.connect(5000,ip,-1,port);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                        Log.i(TAG, "connect failed:" + ex.getMessage());
                        btnConnnect.post(new Runnable() {
                            @Override
                            public void run() {
                                btnConnnect.setClickable(true);
                            }
                        });
                    }
                }
            }.start();
        }else if(view==btnSend){

            if(client!=null){
                new Thread("Send") {
                    public void run() {
                        try {
                            FrameworkMessage.RegisterTCP mregisterTCP=new FrameworkMessage.RegisterTCP();
                            client.sendUDP(mregisterTCP.getByte());
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
