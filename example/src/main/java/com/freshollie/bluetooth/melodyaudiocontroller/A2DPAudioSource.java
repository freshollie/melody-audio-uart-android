package com.freshollie.bluetooth.melodyaudiocontroller;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.os.Handler;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;

import com.freshollie.uart.melodyaudio.MelodyAudioUartInterface;

/**
 * Created by freshollie on 13.12.17.
 */

public class A2DPAudioSource implements AudioManager.OnAudioFocusChangeListener {
    private final static String TAG = A2DPAudioSource.class.getSimpleName();

    private AudioManager audioManager;
    private MelodyAudioManager melodyAudioManager;

    private int linkId;
    private String deviceName;

    private PlaybackStateCompat.Builder playbackStateBuilder;
    private MediaSessionCompat mediaSession;

    private boolean hasFocus;

    public A2DPAudioSource(Context context, MelodyAudioManager manager, int linkId, String deviceName) {
        Log.d(TAG, "Creating");

        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        melodyAudioManager = manager;
        mediaSession = new MediaSessionCompat(context, TAG);
        mediaSession.setActive(true);

        mediaSession.setMetadata(new MediaMetadataCompat.Builder().build());

        playbackStateBuilder = new PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY_PAUSE |
                        PlaybackStateCompat.ACTION_PLAY |
                        PlaybackStateCompat.ACTION_PAUSE |
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                );

        this.linkId = linkId;
        this.deviceName = deviceName;
        hasFocus = false;
    }

    public void setDeviceName(String name) {
        mediaSession.setMetadata(
                new MediaMetadataCompat.Builder(mediaSession.getController().getMetadata())
                        .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, name)
                        .build()
        );
        deviceName = name;
    }

    public String getDeviceName(String name) {
        return deviceName;
    }

    public int getLinkId() {
        return linkId;
    }

    private void setPlaybackState(int playbackState) {
        mediaSession.setPlaybackState(
                playbackStateBuilder.setState(
                        playbackState,
                        0,
                        1
                ).build()
        );
    }

    private void setMetadata(MediaMetadataCompat metadata) {

    }

    public int getPlaybackState() {
        return mediaSession
                .getController()
                .getPlaybackState()
                .getState();
    }

    public void handlePauseRequest() {
        setPlaybackState(PlaybackStateCompat.STATE_PAUSED);
        melodyAudioManager.sendCommand("MUSIC", getLinkId() + " " + "PAUSE");
    }

    public void handlePlayRequest() {
        if (getPlaybackState() != PlaybackStateCompat.STATE_PLAYING &&
                (hasFocus || requestAudioFocus())) {
            setPlaybackState(PlaybackStateCompat.STATE_PLAYING);
            melodyAudioManager.sendCommand("MUSIC", getLinkId() + " " + "PLAY");
        }
    }

    public void handleNextRequest() {

    }

    public void handlePreviousRequest() {

    }

    public void handleStopRequest() {

    }

    void destroy() {
        if (hasFocus) {
            abandonAudioFocus();
        }
        setPlaybackState(PlaybackStateCompat.STATE_STOPPED);
        mediaSession.setActive(false);
        mediaSession.release();
    }

    void onReceivedMetadata(String data) {
        String key = data.substring(0, data.indexOf(":"));
        String value = data.substring(data.indexOf(":") + 1, data.length());

        MediaMetadataCompat.Builder metadataBuilder =
                new MediaMetadataCompat.Builder(mediaSession.getController().getMetadata());

        switch(key) {
            case "ALBUM":
                metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM, value);
                setMetadata(metadataBuilder.build());
                break;
            case "ARTIST":
                metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, value);
                setMetadata(metadataBuilder.build());
                break;
            case "TITLE":
                metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, value);
                setMetadata(metadataBuilder.build());
                break;
        }

    }

    void onAVRCPReceived(String avrcpType, String[] extras) {
        switch (avrcpType) {
            case MelodyAudioUartInterface.ResponseKeys.AVRCP_MEDIA:
                onReceivedMetadata(TextUtils.join(" ", extras));
                break;
            case MelodyAudioUartInterface.ResponseKeys.AVRCP_PAUSE:
                setPlaybackState(PlaybackStateCompat.STATE_PAUSED);
                break;
            case MelodyAudioUartInterface.ResponseKeys.AVRCP_PLAY:
                if (!hasFocus) {
                    requestAudioFocus();
                }
                setPlaybackState(PlaybackStateCompat.STATE_PLAYING);

        }
    }

    private void handleAudioFocusGained() {

    }

    private void handleAudioFocusLost() {

    }

    private void handleMute() {

    }

    private void handleDuck() {

    }

    private boolean requestAudioFocus() {
        int focusResponse = audioManager.requestAudioFocus(
                this,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
        );

        if (focusResponse == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            hasFocus = true;
            return true;
        } else {
            hasFocus = false;
            return false;
        }
    }

    @Override
    public void onAudioFocusChange(int newFocus) {
        switch (newFocus) {
            case AudioManager.AUDIOFOCUS_GAIN:
                handleAudioFocusGained();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                handleMute();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                handleDuck();
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                handlePauseRequest();
                abandonAudioFocus();
                break;
        }
    }

    private void abandonAudioFocus() {
        audioManager.abandonAudioFocus(this);
        hasFocus = false;
    }
}
