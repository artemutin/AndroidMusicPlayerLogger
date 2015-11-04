package ru.yandex.artemutin.musicplayerlogger;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.util.Log;
import java.sql.Date;

import java.util.HashMap;

public class PlayerIntentsReceiver extends BroadcastReceiver {
    public PlayerIntentsReceiver() {
    }
    private class Track{
        String track, artist, album;
        LogDBHelper.TrackStatus status;
        Date datetime;

        public Track(Cursor cursor){
            track = cursor.getString(1);
            album = cursor.getString(2);
            artist = cursor.getString(3);
            datetime = new Date(cursor.getLong(4));
            switch (cursor.getInt(5)){
                case 0: status = LogDBHelper.TrackStatus.PLAYING; break;
                case 1: status = LogDBHelper.TrackStatus.PLAYED; break;
                case 2: status = LogDBHelper.TrackStatus.SKIPPED; break;
                default: throw new RuntimeException("Incorrect track status");
            }
        }
    }
    private LogDBHelper helper;
    private Context context;
    private long startTimeStamp;//unix time

    private SQLiteDatabase getDBr(){
        return helper.getReadableDatabase();
    }

    private SQLiteDatabase getDBw(){
        return helper.getWritableDatabase();
    }

    private Track getLastTrack(){
        Cursor lastTrack = getDBr().query("track_log"
                , null//all columns
                , null//all rows
                , null//no selection args
                , null//no grouping
                , null//no having
                , "id DESC"
                , "1"//first row
        );
        return lastTrack == null ? null : new Track(lastTrack);
    }

    private Track getCurrentTrack(Intent intent) {
        Bundle extras = intent.getExtras();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("Logger", "Intent was received");
        //IMPORTANT INIT BEFORE ANY ACTION
        this.context = context;//possibly incorrect
        this.helper = new LogDBHelper(context);
        /////////////////////////////////////////

        boolean isPlaying = intent.getBooleanExtra("playing", false);


        if (!isPlaying) {
            Track lastTrack = getLastTrack();
            if (lastTrack == null) {
                return; //no tracks were played, neither playing now
            }
        }
            if (isPlaying && extras != null) {
                HashMap<String, String> songInfo = new HashMap<String, String>();
                //Search for minimum info about playing song
                try {
                    songInfo.put("track", extras.getString("track"));
                    songInfo.put("album", extras.getString("album"));
                    songInfo.put("artist", extras.getString("artist"));
                } catch (Exception e) {
                    throw new RuntimeException("Your music player is not provide correct info for Music Player Logger");
                }
                long duration = extras.getLong("duration");
                if (duration == 0L) {
                    //TODO: find duration by song id
                    //throw warning
                }
            }

            Log.d("Logger", isPlaying ? "playing" : "not playing");
            if (isPlaying) {
                Log.d("Logger", songInfo.get("track"));
                Log.d("Logger", songInfo.get("album"));
                Log.d("Logger", songInfo.get("artist"));
            }
        }
    }
}

