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
         * @param db_id is for using in update status
         */
        long db_id;

        /**
         * constructor from database row
         * @param cursor
         */
        Track(Cursor cursor){
            db_id = cursor.getLong(0);
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

    //constants
    private final static long DUR_EPS = 5000L;//5 seconds
    private final static String PREFS_NAME = "PlayerIntentsReceiverPreferences";
    private final static String LOG_PREF = "Logger";


    private LogDBHelper helper;
    private Context context;
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

    private void updateLastTrackState(Track previousTrack, int status){
        ContentValues vals = previousTrack.getColumnValues();
        vals.put("datetime", System.currentTimeMillis());
        vals.put("status", status);
        //TODO: in question, need i use all cols or only updated
        getDBw().update(LogDBHelper.LOG_TB_NAME, vals, "id = " + String.valueOf(previousTrack.db_id), null );
    }

    private void saveTime(Track track) {
        SharedPreferences timePrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = timePrefs.edit();
        //this time has a problem - it can be changed by user
        //from the other hand System.nanoTime is valid only during a boot
        //TODO: make it right way, not depending on reboot and user time change
        editor.putLong("startTimeStamp", System.currentTimeMillis());
        editor.putLong("timeWasPlaying", timeWasPlaying);
        if (track.duration == 0L){
            throw new RuntimeException("Track must have a duration.");
        }
        editor.putLong("duration", track.duration);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(LOG_PREF, "Intent was received");
        /////////////////////////////////////////

        //IMPORTANT INIT BEFORE ANY FURTHER ACTION
        this.context = context;//possibly incorrect
        this.helper = new LogDBHelper(context);
        this.timeWasPlaying = 0L;

        /////////////////////////////////////////

        boolean isPlaying = intent.getBooleanExtra("playing", false);
        Track previousTrack = getPreviousTrack();

        if (!isPlaying) {
            Track lastTrack = getPreviousTrack();
            if (lastTrack == null) {
                Log.w(LOG_PREF, "No tracks were played and no one playing now.");
                return; //no tracks were played, neither playing now
            }else{
                //possibly it is true stop. No skip state anyway
                updateLastTrackState(previousTrack, Track.PLAYED);
            }
        }
        if (isPlaying ) {
            Track currentTrack = new Track(intent);
            if (currentTrack.duration == 0L) {
                //TODO: find duration by song id
                throw new UnsupportedOperationException("Incoming intent must contain non-null duration");
            }

            if (previousTrack == null) {
                //very rare, but still valid state
                saveTime(currentTrack);
                saveCurrentTrack(currentTrack, Track.PLAYING);
            }

            SharedPreferences timePrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            long startTimeStamp = timePrefs.getLong("startTimeStamp", 0);
            timeWasPlaying = timePrefs.getLong("timeWasPlaying", 0);
            previousTrack.duration = timePrefs.getLong("duration", 0);
            if (currentTrack.equals(previousTrack)) {
               //was paused or smth

            }else{
                //summary time of being played
                timeWasPlaying = System.currentTimeMillis() - startTimeStamp + timeWasPlaying;
                if (timeWasPlaying < 0){
                    throw new RuntimeException("Incorrect time diff.");
                }

                if (previousTrack.status == Track.PLAYING){//if last track saved in PLAYING state only
                    if (previousTrack.duration - timeWasPlaying > DUR_EPS){
                        //track was skipped
                        //modify previous track log
                        updateLastTrackState(previousTrack, Track.SKIPPED);
                    }else if(Math.abs(previousTrack.duration - timeWasPlaying) < DUR_EPS){
                        //track was completed normally
                        updateLastTrackState(previousTrack, Track.PLAYED);
                    }else if(timeWasPlaying - previousTrack.duration > DUR_EPS){
                        //track played longer, than duration
                        //Means some stop, without proper intent here.
                        Log.w(LOG_PREF, "Music played longer than duration");
                        updateLastTrackState(previousTrack, Track.PLAYED);

                    }

                }
                //save current track in db
                saveCurrentTrack(currentTrack, Track.PLAYING);
                //save time in SharedPrefs
                saveTime(currentTrack);
            }

        }


        }
    }


