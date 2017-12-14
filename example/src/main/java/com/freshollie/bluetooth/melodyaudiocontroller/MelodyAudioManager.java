package com.freshollie.bluetooth.melodyaudiocontroller;

import android.content.Context;
import android.util.Log;
import android.util.SparseArray;

import com.freshollie.uart.melodyaudio.MelodyAudioUartConnection;
import com.freshollie.uart.melodyaudio.MelodyAudioUartInterface;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.PriorityQueue;

/**
 * Created by freshollie on 09.12.17.
 */

public class MelodyAudioManager extends MelodyAudioUartInterface.MelodyAudioUartInterfaceCallback implements
        MelodyAudioUartConnection.ConnectionStateChangeListener {
    private static final String TAG = MelodyAudioManager.class.getSimpleName();

    private boolean running;

    private final Context context;

    private final MelodyAudioUartConnection melodyAudioUartConnection;
    private final MelodyAudioUartInterface melodyAudioUartInterface;

    private final PriorityQueue<String> pendingCommands;
    private A2DPAudioSource audioSource;

    MelodyAudioManager(Context context) {
        this.context = context;

        pendingCommands = new PriorityQueue<>();


        melodyAudioUartConnection = new MelodyAudioUartConnection(context, 115200);
        melodyAudioUartConnection.registerConnectionChangeListener(this);

        melodyAudioUartInterface = melodyAudioUartConnection.getInterface();
        melodyAudioUartInterface.registerMelodyAudioCallback(this);

        running = false;
    }

    public void start() {
        if (!running) {
            running = true;
            melodyAudioUartConnection.open();
        }
    }

    public void stop() {
        if (running) {
            melodyAudioUartConnection.close();
        }
    }

    public void sendCommand(String command) {
        melodyAudioUartInterface.sendCommand(command);
    }


    public void sendCommand(String command, String args) {
        melodyAudioUartInterface.sendCommand(command, args);
    }

    @Override
    public void onLinkStatusReceived(int linkId, String status, String linkType, String address, String[] extras) {
        Log.d(TAG, "Link status " + linkId + " " + address);
    }

    @Override
    public void onCallStatusReceived(int linkId, int linkType, String callStatus) {
        Log.d(TAG, "Call status " + callStatus);
    }

    @Override
    public void onCallerNumberReceived(int linkId, String number) {
        Log.d(TAG, "Someone is calling! " + number);
    }

    @Override
    public void onListReceived(String address, String[] supportedProfiles) {
        Log.d(TAG, "Saved device: " + address);
        Log.d(TAG, "Getting name");
        melodyAudioUartInterface.sendCommand(MelodyAudioUartInterface.Commands.NAME, address);
    }

    @Override
    public void onNameReceived(String address, String name) {
        Log.d(TAG, "Device name " + name);
    }

    @Override
    public void onPreferenceReceived(String key, String value) {
        Log.d(TAG, "Preference received " + key + ": " + value);
    }

    @Override
    public void onAVRCPReceived(int linkId, String avrcpType, String[] extras) {
        Log.d(TAG, "AVRCP received: " + linkId + " " + avrcpType + " " + Arrays.toString(extras));

        if (audioSource != null && audioSource.getLinkId() != linkId && linkId != -1) {
            audioSource.destroy();
            audioSource = null;
        }

        if (audioSource == null && linkId != -1) {
            audioSource = new A2DPAudioSource(context, this, linkId, "");
        }

        if (audioSource != null) {
            audioSource.onAVRCPReceived(avrcpType, extras);
        }
    }

    @Override
    public void onErrorReceived(int code) {
        if (code == MelodyAudioUartInterface.Errors.NAME_NOT_FOUND) {
            Log.d(TAG, "Could not find device name");
        }
    }

    @Override
    public void onConnectionStateChange(int newState) {
        if (newState == MelodyAudioUartConnection.STATE_CONNECTED) {
            Log.d(TAG, "Connected");
            melodyAudioUartInterface.sendCommand(MelodyAudioUartInterface.Commands.LIST);
            melodyAudioUartInterface.sendCommand(MelodyAudioUartInterface.Commands.STATUS);
            melodyAudioUartInterface.sendCommand(MelodyAudioUartInterface.Commands.GET, MelodyAudioUartInterface.ConfigKeys.NAME);
        }
    }
}
