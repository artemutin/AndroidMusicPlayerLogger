package ru.yandex.artemutin.musicplayerlogger;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
    static class Track{
        final static int PLAYING = 0, PLAYED = 1, SKIPPED = 2;

        static int getTrackStatus(int val) {
            if (val < 0 || val > 2) {
                throw new RuntimeException("Track status is incorrect");
            }
            return val;
        }


        String track, artist, album;
        int status;
        Date datetime;
        long duration;

        /**
         * constructor from database row
         * @param cursor
         */
        Track(Cursor cursor){
            track = cursor.getString(1);
            album = cursor.getString(2);
            artist = cursor.getString(3);
            datetime = new Date(cursor.getLong(4));
            status = Track.getTrackStatus(cursor.getInt(5));
        }

        /**
         * constructor from incoming intent
         * @param intent
         */
        Track(Intent intent) {
            Bundle extras = intent.getExtras();
            track = extras.getString("track");
            album = extras.getString("album");
            artist = extras.getString("artist");
            duration = extras.getLong("duration");
            datetime = new Date(new java.util.Date().getTime());
            status = Track.PLAYING;
        }

        /**
         * convert track to vals for a sql insert
         * contains only track info, no time and state
         * @return
         */
        ContentValues getColumnValues() {
           ContentValues vals = new ContentValues();
           vals.put("track", track);
            vals.put("album",album);
            vals.put("artist",artist);
            return vals;
        }

        public boolean equals(Track a) {
            return track == a.track && album == a.album && artist == a.artist;
        }
    }

    private LogDBHelper helper;
    private Context context;
    private final static String PREFS_NAME = "PlayerIntentsReceiverPreferences";
    private long startTimeStamp, timeWasPlaying;//unix time

    private SQLiteDatabase getDBr(){
        return helper.getReadableDatabase();
    }

    private SQLiteDatabase getDBw(){
        return helper.getWritableDatabase();
    }

    private Track getPreviousTrack(){
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

    private void saveCurrentTrack(Track track, int status) {
        ContentValues vals = track.getColumnValues();
        vals.put("datetime", System.currentTimeMillis());
        vals.put("status", status);
        getDBw().insert(LogDBHelper.LOG_TB_NAME, null, vals);
    }

    private void saveTime() {
        SharedPreferences timePrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = timePrefs.edit();
        //this time has a problem - it can be changed by user
        //from the other hand System.nanoTime is valid only during a boot
        //TODO: make it right way, not depending on reboot and user time change
        editor.putLong("startTimeStamp", System.currentTimeMillis());
        editor.putLong("timeWasPlaying", timeWasPlaying);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("Logger", "Intent was received");
        /////////////////////////////////////////

        //IMPORTANT INIT BEFORE ANY FURTHER ACTION
        this.context = context;//possibly incorrect
        this.helper = new LogDBHelper(context);
        this.timeWasPlaying = 0L;

        /////////////////////////////////////////

        boolean isPlaying = intent.getBooleanExtra("playing", false);


        if (!isPlaying) {
            Track lastTrack = getPreviousTrack();
            if (lastTrack == null) {
                return; //no tracks were played, neither playing now
            }
        }
        if (isPlaying ) {
            Track currentTrack = new Track(intent);
            if (currentTrack.duration == 0L) {
                //TODO: find duration by song id
                throw new UnsupportedOperationException("Incoming intent must contain non-null duration");
            }
            Track previousTrack = getPreviousTrack();
            if (previousTrack == null) {
                //very rare, but still valid state
                saveTime();
                saveCurrentTrack(currentTrack, Track.PLAYING);
            }
            if (currentTrack.equals(previousTrack)) {
                //tracks are equal
            }

        }


        }
    }


