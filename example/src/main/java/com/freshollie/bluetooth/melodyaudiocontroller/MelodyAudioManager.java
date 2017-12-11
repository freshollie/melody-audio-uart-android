package com.freshollie.bluetooth.melodyaudiocontroller;

import android.util.Log;

import com.freshollie.uart.melodyaudio.MelodyAudioUartInterface;

import java.util.ArrayList;
import java.util.PriorityQueue;

/**
 * Created by freshollie on 09.12.17.
 */

public class MelodyAudioManager implements MelodyAudioUartInterface.ResponseListener {
    private static final String TAG = MelodyAudioManager.class.getSimpleName();

    private final MelodyAudioUartInterface melodyAudioUartInterface;

    private final PriorityQueue<String> pendingCommands;

    public MelodyAudioManager(MelodyAudioUartInterface melodyInterface) {
        pendingCommands = new PriorityQueue<>();

        melodyAudioUartInterface = melodyInterface;
        melodyAudioUartInterface.registerResponseListener(this);
        pullConfig();
    }

    public void registerMelodyAudioCallback(MelodyAudioCallback melodyAudioCallback) {

    }

    public void pullConfig() {
        sendCommand(MelodyAudioUartInterface.Commands.CONFIG);
    }

    private void sendCommand(String command) {
        sendCommand(command, "");
    }

    private void sendCommand(String command, String args) {
        pendingCommands.add(command);
        melodyAudioUartInterface.sendCommand(command + " " + args);
    }

    private void onOkReceived() {
        String command = pendingCommands.poll();

    }

    private void onErrorReceived(String data) {
        String hexCode =
                data.replace(MelodyAudioUartInterface.ResponseKeys.ERROR, "");
        int code = Integer.parseInt(hexCode, 16);
        String command = pendingCommands.poll();

    }

    private void onPreferenceReceived(String data) {
        String[] pair = data.split("=");
        String key = pair[0];
        String value = pair[1];

    }

    private void onABSVolReceived(String data) {
        String[] values = data.split(" ");
        int linkId = Integer.valueOf(values[1]);
        int volume = Integer.valueOf(values[2]);

    }

    private void onAVRCPReceived(String data) {
        String[] values = data.split(" ");
        String avrcpType = values[0];
        int linkId = Integer.valueOf(values[1]);

    }

    private void onCallStatusReceived(String data) {
        String[] values = data.split(" ");
        int type = values[1].equals("HFPAG") ?
                MelodyAudioUartInterface.BluetoothProfiles.HFPAG :
                MelodyAudioUartInterface.BluetoothProfiles.HFP;

        int linkId = Integer.parseInt(values[2]);

    }

    private void onCallerNumberReceived(String data) {
        String[] values = data.split(" ");
        int linkId = Integer.parseInt(values[0]);
        String number = values[1];

    }

    private void onA2DPStreamStatusReceived(String data) {
        String[] values = data.split(" ");

        String a2dpStreamType = values[0];
        int linkId = Integer.parseInt(values[1]);

    }

    private void routeResponse(String response) {
        Log.d(TAG, "Route response " + response);
        Log.d(TAG, response);


        if (response.equals(MelodyAudioUartInterface.ResponseKeys.OK)) {

        } else if (response.contains("=")) {
            // Preference response
            onPreferenceReceived(response);

        } else if (response.startsWith(MelodyAudioUartInterface.ResponseKeys.ERROR)) {
            onErrorReceived(response);

        } else if (response.startsWith(MelodyAudioUartInterface.ResponseKeys.PENDING)) {
            onErrorReceived(response);

        } else if (response.startsWith(MelodyAudioUartInterface.ResponseTypes.AVRCP)) {
            onAVRCPReceived(response);

        } else if (response.startsWith(MelodyAudioUartInterface.ResponseKeys.ABS_VOL)) {
            onABSVolReceived(response);

        } else if (response.startsWith(MelodyAudioUartInterface.ResponseTypes.CALL)) {
            onCallStatusReceived(response);

        } else if (response.startsWith(MelodyAudioUartInterface.ResponseKeys.CALLER_NUMBER)) {
            onCallerNumberReceived(response);

        } else if (response.startsWith(MelodyAudioUartInterface.ResponseTypes.A2DP_STREAM)) {
            onCallerNumberReceived(response);

        } else if (response.startsWith(MelodyAudioUartInterface.ResponseKeys.CALLER_NUMBER)) {
            onCallerNumberReceived(response);

        }
    }


    @Override
    public void onResponse(String response) {
        Log.d("Response", response);
        routeResponse(response);
    }

    public void close() {
        melodyAudioUartInterface.unregisterResponseListener(this);
    }

    public static class MelodyAudioCallback {
        public void onAVRCPReceived(int linkId, String avrcpType) {}

        public void onABSVolReceived(int linkId, int volume) {}

        public void onCallStatusReceived(int linkId, String callStatus) {}

        public void onCallerNumberReceived(int linkId, String number)  {}

        public void onPreferenceReceived(String key, String value) {}

        public void onOKReceived(String command) {}

        public void onErrorReceived(String command, int code) {}
    }
}
