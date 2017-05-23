/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.android.voiceime;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.inputmethodservice.InputMethodService;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.view.inputmethod.InputMethodSubtype;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.handshake.ServerHandshake;
import java.net.URI;
import java.net.URISyntaxException;
import android.util.Log;
import android.media.MediaRecorder;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

/**
 * Triggers a voice recognition by using {@link ImeTrigger} or
 * {@link IntentApiTrigger}.
 */
public class VoiceRecognitionTrigger {

    private final InputMethodService mInputMethodService;
    //Activity context;
    private BroadcastReceiver mReceiver;

    private Trigger mTrigger;

    private static final int RECORDER_SAMPLERATE = 8000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private AudioRecord recorder = null;
    //private int bufferSize = 0;
    private Thread recordingThread = null;
    private boolean isRecording = false;
    private WebSocketClient mWebSocketClient=null;

    private ImeTrigger mImeTrigger;
    private IntentApiTrigger mIntentApiTrigger;
    private ActivityHelper activityHelper=new ActivityHelper();


    public VoiceRecognitionTrigger(InputMethodService inputMethodService) {
        mInputMethodService = inputMethodService;
        //mIntentApiTrigger = new IntentApiTrigger(mInputMethodService);
        mTrigger = getTrigger();
       // mIntentApiTrigger = new IntentApiTrigger(mInputMethodService);
    }



   /* private Trigger getTrigger() {
        if (ImeTrigger.isInstalled(mInputMethodService)) {
            return getImeTrigger();
        } else if (IntentApiTrigger.isInstalled(mInputMethodService)) {
            return getIntentTrigger();
        } else {
            return null;
        }
    }*/

    private Trigger getTrigger() {
        if (IntentApiTrigger.isInstalled(mInputMethodService)) {
            return getIntentTrigger();
        } else {
            return null;
        }
    }

    private Trigger getIntentTrigger() {
        if (mIntentApiTrigger == null) {
            mIntentApiTrigger = new IntentApiTrigger(mInputMethodService);
        }
        return mIntentApiTrigger;
    }

    private Trigger getImeTrigger() {
        if (mImeTrigger == null) {
            mImeTrigger = new ImeTrigger(mInputMethodService);
        }
        return mImeTrigger;
    }

    public boolean isInstalled() {
        return mTrigger != null;
    }

    public boolean isEnabled() {
        return isNetworkAvailable();
    }



    /**
     * Starts a voice recognition. The language of the recognition will match
     * the voice search language settings, or the locale of the calling IME.
     */
    public void startVoiceRecognition() {
        startVoiceRecognition(null);
    }

    /*
   *websocket connected
   */



    public void appenddata(String comingData)
    {
        mIntentApiTrigger.SendDatafromwebsocket(comingData);
        mIntentApiTrigger.onStartInputView();
        Log.i("Result","Result="+comingData);
    }



    /**
     * Starts a voice recognition
     *
     * @param language The language in which the recognition should be done. If
     *                 the recognition is done through the Google voice typing, the
     *                 parameter is ignored and the recognition is done using the
     *                 locale of the calling IME.
     * @see InputMethodSubtype
     */
    public void startVoiceRecognition(String language) {
        if (mTrigger != null) {
            mTrigger.startVoiceRecognition(language);
        }
    }

    public void onStartInputView() {
        if (mTrigger != null) {
            mTrigger.onStartInputView();
        }

        // The trigger is refreshed as the system may have changed in the meanwhile.
        mTrigger = getTrigger();
    }

    private boolean isNetworkAvailable() {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) mInputMethodService
                    .getSystemService(
                            Context.CONNECTIVITY_SERVICE);
            final NetworkInfo info = connectivityManager.getActiveNetworkInfo();
            return info != null && info.isConnected();
        } catch (SecurityException e) {
            // The IME does not have the permission to check the networking
            // status. We hope for the best.
            return true;
        }
    }




    /**
     * Register a listener to receive a notification every time the status of
     * Voice IME may have changed. The {@link Listener} should
     * update the UI to reflect the current status of Voice IME. When
     * {@link Listener} is registered,
     * {@link #unregister(Context)} must be called when the IME is dismissed
     * {@link InputMethodService#onDestroy()}.
     */
    public void register(final Listener listener) {
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();
                if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                    listener.onVoiceImeEnabledStatusChange();
                }
            }
        };
        final IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        mInputMethodService.registerReceiver(mReceiver, filter);
    }

    /**
     * Unregister the {@link Listener}.
     */
    public void unregister(Context context) {
        if (mReceiver != null) {
            mInputMethodService.unregisterReceiver(mReceiver);
            mReceiver = null;
        }
    }

    public interface Listener {

        /**
         * The enable status of Voice IME may have changed.
         */
        void onVoiceImeEnabledStatusChange();
    }
}
