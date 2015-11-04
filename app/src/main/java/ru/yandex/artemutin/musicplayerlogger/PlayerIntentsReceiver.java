package ru.yandex.artemutin.musicplayerlogger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import java.util.HashMap;

public class PlayerIntentsReceiver extends BroadcastReceiver {
    public PlayerIntentsReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("Logger", "Intent was received");
        boolean isPlaying = intent.getBooleanExtra("playing", false);
        HashMap<String , String> songInfo = null;
        Bundle extras = intent.getExtras();
        if (isPlaying && extras != null){
            songInfo = new HashMap<String, String>();
            //Search for minimum info about playing song
            try {
                songInfo.put("track", extras.getString("track"));
                songInfo.put("album", extras.getString("album"));
                songInfo.put("artist", extras.getString("artist"));
            }catch (Exception e){
                throw new RuntimeException("Your music player is not provide correct info for Music Player Logger");
            }
            long duration = extras.getLong("duration");
            if (duration == 0L){
                //TODO: find duration by song id
                //throw warning
            }
        }

        Log.d("Logger", isPlaying ? "playing":"not playing");
        if (isPlaying){
            Log.d("Logger", songInfo.get("track"));
            Log.d("Logger", songInfo.get("album"));
            Log.d("Logger", songInfo.get("artist"));
        }
    }
}
