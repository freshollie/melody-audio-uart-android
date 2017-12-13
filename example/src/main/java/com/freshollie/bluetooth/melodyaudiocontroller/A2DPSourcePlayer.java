package com.freshollie.bluetooth.melodyaudiocontroller;

import android.content.Context;

/**
 * Created by freshollie on 13.12.17.
 */

public class A2DPSourcePlayer {
    int linkId;
    public A2DPSourcePlayer(Context context, int linkId) {
        this.linkId = linkId;
    }

    public void handlePlaystateChange() {

    }

    public void handleNewMetaData() {

    }

    public void requestAudioFocus() {

    }
}
