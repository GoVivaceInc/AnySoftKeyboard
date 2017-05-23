package com.google.android.voiceime;

/**
 * Created by Dell on 5/16/2017.
 */

import android.app.Activity;
import com.menny.android.anysoftkeyboard.R;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.CompoundButton;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import android.os.IBinder;

/**
 * Created by Dell on 5/16/2017.
 */

public class DisplayMessageActivity extends Activity {
    private ToggleButton toggleButton;
    private ServiceBridge mServiceBridge;
    final Context context = this;
    private ImageButton imagebutton;
    private TextView textView;
    private static final int RECORDER_SAMPLERATE = 8000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private AudioRecord recorder = null;
    boolean mStartRecording = true;
    boolean isPartialText=true;
    private IntentApiTrigger mIntentApiTrigger;
    StringBuilder output = new StringBuilder("");
    StringBuilder partialresult = new StringBuilder("");
    //private int bufferSize = 0;
    private Thread recordingThread = null;
    private boolean isRecording = false;
    private WebSocketClient mWebSocketClient=null;
    private ServiceHelper serviceHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState1) {
        mServiceBridge = new ServiceBridge();
        super.onCreate(savedInstanceState1);
        setContentView(R.layout.activity_display_message);
         serviceHelper = new ServiceHelper();

        addListenerOnButtonClick();
    }

    private boolean isNetworkAvailable() {
        boolean connected = false;
        ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        if(connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED ||
                connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED) {
            //we are connected to a network
            connected = true;
            return connected;
        }
        else
            connected = false;
        return connected;
    }

    private Boolean exit = false;
    @Override
    public void onBackPressed() {
        if (exit) {
            if(mWebSocketClient!=null)
            {
                mWebSocketClient.send("EOS");
                mWebSocketClient=null;
                onRecord(mStartRecording);
                notifyResult(output.toString());
                //onBackPressed();
            }
            else {
                if (output.length()==0) {
                    finish();
                } else {
                    notifyResult(output.toString());
                }
            }
        } else {
            Toast.makeText(this, "Press Back again to Exit.",
                    Toast.LENGTH_SHORT).show();
            exit = true;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    exit = false;
                }
            }, 3 * 1000);
        }
    }

    public void addListenerOnButtonClick(){
        //Getting the ToggleButton and Button instance from the layout xml file
        toggleButton=(ToggleButton)findViewById(R.id.toggleButton);
        imagebutton=(ImageButton) findViewById(R.id.imageButton);
        textView=(TextView)findViewById(R.id.textView2);
        textView.setMovementMethod(new ScrollingMovementMethod());
        //Performing action on button click
        imagebutton.setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View view) {
                switch(view.getId())
                {
                    case R.id.imageButton:
                        if(mWebSocketClient!=null)
                        {
                            mWebSocketClient.send("EOS");
                            mWebSocketClient=null;
                            onRecord(mStartRecording);
                        }
                        else
                        {
                            finish();
                        }
                        StringBuilder result1 = new StringBuilder();
                        result1.append("ToggleButton2 : ").append(toggleButton.getText());
                        //Displaying the message in toast
                        //Toast.makeText(getApplicationContext(), result1.toString(),Toast.LENGTH_LONG).show();
                        break;
                }
            }

        });

        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // The toggle is enabled
                    StringBuilder result = new StringBuilder();
                    result.append(toggleButton.getText()).append(" Now");
                    //Displaying the message in toast
                    if(!isNetworkAvailable())
                    {
                        Toast.makeText(getApplicationContext(), "You currently have no network connection!", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                    else {
                        Toast.makeText(getApplicationContext(), result.toString(),Toast.LENGTH_LONG).show();
                        createWebSocket();
                    }

                } else {
                    // The toggle is disabled
                    if(mWebSocketClient!=null) {
                        mWebSocketClient.send("EOS");
                        mWebSocketClient = null;
                        onRecord(mStartRecording);

                        StringBuilder result = new StringBuilder();
                        result.append(toggleButton.getText()).append(" Speak");
                        //Displaying the message in toast
                        Toast.makeText(getApplicationContext(), result.toString(), Toast.LENGTH_LONG).show();

                        //onBackPressed();
                    }
                }
            }
        });
    }

    String errorNotifyer;
    String comingData;
    public void createWebSocket() {

        URI uri;
        try {
            //uri = new URI("ws://104.236.244.251:7682/telephony");
            //uri = new URI("ws://mrcp.govivace.com:7682/telephony");
            //uri = new URI("ws://echo.websocket.org");
            uri = new URI("ws://services.govivace.com:49154/telephony");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }
        Map<String, String> headers = new HashMap<>();
        mWebSocketClient = new WebSocketClient(uri,new Draft_17(),headers,0) {

            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                Log.i("Websocket", "Opened");
                onRecord(mStartRecording);
               // Toast.makeText(getApplicationContext(), "Socket Opened",Toast.LENGTH_LONG).show();
                mStartRecording = !mStartRecording;
                //Log.i("Websocket","call onopen after onrecord method");
            }

            @Override
            public void onMessage(String s) {
                final String message = s;
                Log.i("websocket","recieved" + message);

                if (message != null) {
                    try {
                        JSONObject jsonObj = new JSONObject(message);
                        int status = jsonObj.optInt("status");
                        if (status == 1) {
                            Toast.makeText(getApplicationContext(), "Speech contains a large portion of silence or non-speech", Toast.LENGTH_SHORT).show();
                        } else if (status == 9) {
                            errorNotifyer = jsonObj.optString("message");
                            Toast.makeText(getApplicationContext(), errorNotifyer, Toast.LENGTH_SHORT).show();
                        } else if (status == 5) {
                            errorNotifyer = jsonObj.optString("message");
                            Toast.makeText(getApplicationContext(), errorNotifyer, Toast.LENGTH_SHORT).show();
                        }
                        // Getting JSON Array node
                        if (status == 0) {
                            JSONObject hypotheses = jsonObj.getJSONObject("result");
                            boolean final1 = hypotheses.getBoolean("final");
                            JSONArray jsonArray = hypotheses.optJSONArray("hypotheses");
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonObject = jsonArray.getJSONObject(i); // getting JSON Object at I'th index
                                String name = jsonObject.optString("transcript");

                                comingData = name.replaceAll("<UNK>","");
                                comingData = comingData.replaceAll("  "," ");
                                if ( final1)
                                {
                                    appenddata();
                                }
                                else
                                {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            if(partialresult.length()==0)
                                            {
                                                textView.setText(comingData);
                                            }
                                            else {
                                                textView.setText(partialresult.toString() + comingData);
                                            }
                                        }
                                    });

                                }

                            }

                        }

                    } catch (JSONException ex) {
                        Log.i("JsonException","exception"+ ex);
                    }
                } else {
                    Log.i("Json", "Couldn't get json from server.");

                    Toast.makeText(getApplicationContext(), "Couldn't get json from server. Check LogCat for possible errors!", Toast.LENGTH_SHORT).show();

                }

            }

            @Override
            public void onClose(int i, String s, boolean b) {
                Log.i("Websocket", "Closed " + s);
                notifyResult(output.toString());
                mWebSocketClient=null;
            }

            @Override
            public void onError(Exception e) {
                Log.i("Websocket", "Error " + e.getMessage());
            }
        };
        mWebSocketClient.connect();
    }

    private void appenddata()
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //stuff that updates ui
                if(output.length()==0)
                {
                    output.append(comingData);
                }
                else
                {
                    output.append(" ").append(comingData);
                }
                textView.setText(output.toString());
                partialresult.append(" ").append(output.toString());

            }
        });
    }

    private void onRecord(boolean start) {
        if (start) {
            startRecording();
        } else {
            stopRecording();
        }
    }

    private void notifyResult(String result) {
        if(result.length()!=0)
        {
            mServiceBridge.notifyResult(this,result);
            finish();
        }
        else
        {
            finish();
        }
    }

    final byte data[] = new byte[100000];
    private void startRecording() {
        Log.i("Websocket","startrecording function call");
        int bufferSize = AudioRecord.getMinBufferSize(8000,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        if(bufferSize>0) {
            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING,data.length);

            int i = recorder.getState();
            Log.i("Websocket", "value of recorder getstate" + i);
            if (i == 1)
                recorder.startRecording();

            isRecording = true;
            recordingThread = new Thread(new Runnable() {

                public void run() {

                    SendDataToServer();

                }
            }, "AudioRecorder Thread");
            recordingThread.start();
        }
    }

    private void SendDataToServer()
    {
        while (isRecording) {
            Log.i("Websocket", "call while loop");
            recorder.read(data, 0, data.length);
            //byte sendbuffer[]=new byte[readbyte];
            //sendbuffer = Arrays.copyOf(data,readbyte);
            if (mWebSocketClient != null) {
                Log.i("Websocket","call websocket send ");
                mWebSocketClient.send(data);
                /*try {
                    Thread.sleep(250);
                } catch (InterruptedException ie)
                {
                    Log.i("Websocket","threadsleep exception"+ie);
                }*/
            }
        }
    }

    private void stopRecording() {
        Log.i("Websocket","call stoprecording function");
        if(null != recorder) {
            isRecording = false;

            int i = recorder.getState();
            if (i == 1)
                recorder.stop();
            recorder.release();

            recorder = null;
            recordingThread = null;
            mStartRecording=!mStartRecording;
        }
    }
}
