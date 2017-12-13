package com.freshollie.bluetooth.melodyaudiocontroller;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.freshollie.uart.melodyaudio.MelodyAudioUartConnection;
import com.freshollie.uart.melodyaudio.MelodyAudioUartInterface;

public class MainActivity extends AppCompatActivity {
    private MelodyAudioManager melodyAudioManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        melodyAudioManager = new MelodyAudioManager(this);
        melodyAudioManager.start();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        melodyAudioManager.stop();
    }
}
