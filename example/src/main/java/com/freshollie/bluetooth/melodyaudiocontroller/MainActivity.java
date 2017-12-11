package com.freshollie.bluetooth.melodyaudiocontroller;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.freshollie.uart.melodyaudio.MelodyAudioUartConnection;
import com.freshollie.uart.melodyaudio.MelodyAudioUartInterface;

public class MainActivity extends AppCompatActivity implements
        MelodyAudioUartConnection.ConnectionStateChangeListener {

    private MelodyAudioUartConnection melodyAudioUartConnection;
    private MelodyAudioManager melodyAudioManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        melodyAudioUartConnection = new MelodyAudioUartConnection(this, 115200);
        melodyAudioUartConnection.registerConnectionChangeListener(this);
        melodyAudioUartConnection.open();

    }

    @Override
    public void onChange(int newState) {
        if (newState == MelodyAudioUartConnection.STATE_CONNECTED) {
            Log.d("Test", "Connected");
            melodyAudioManager = new MelodyAudioManager(melodyAudioUartConnection.getInterface());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        melodyAudioUartConnection.close();
        if (melodyAudioManager != null) {
            melodyAudioManager.close();
        }
    }


}
