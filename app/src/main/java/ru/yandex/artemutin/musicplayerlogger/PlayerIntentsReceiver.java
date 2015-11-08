package ru.yandex.artemutin.musicplayerlogger;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import java.sql.Date;

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
            cursor.close();
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

        @Override
        public int hashCode() {
            int result = track.hashCode();
            result = 31 * result + artist.hashCode();
            result = 31 * result + album.hashCode();
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Track track1 = (Track) o;

            if (!track.equals(track1.track)) return false;
            if (!artist.equals(track1.artist)) return false;
            return album.equals(track1.album);

        }
    }

    //constants
    private final static long DUR_EPS = 5000L;//5 seconds
    private final static String PREFS_NAME = "PlayerIntentsReceiverPreferences";
    private final static String LOG_PREF = "Logger";


    private LogDBHelper helper;
    private Context context;
    private TimePrefs timePrefs;//unix time


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
        if (lastTrack.moveToFirst() ){
            Track t = new Track(lastTrack);
            t.duration = timePrefs.getDuration();
            if (t.duration <= 0){
                throw new RuntimeException("Duration is zero or lesser");
            }

            return t;

        }else{
            return null;
        }
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
        getDBw().update(LogDBHelper.LOG_TB_NAME, vals, "id = " + String.valueOf(previousTrack.db_id), null);
    }

    private class TimePrefs{
        private long timeWasPlaying;
        private long startTimeStamp;
        private long duration;
        private SharedPreferences timePrefs;

        TimePrefs(){
            timePrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

            setStartTimeStamp(timePrefs.getLong("startTimeStamp", 0));
            setTimeWasPlaying(timePrefs.getLong("timeWasPlaying", 0));
            setDuration(timePrefs.getLong("duration", 0));
        }

        TimePrefs(boolean empty){
            timePrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        }

        void fixTimeWasPlaying(){
            this.setTimeWasPlaying(timeWasPlaying +
                    (startTimeStamp > 0 ? System.currentTimeMillis() - startTimeStamp : 0) );//addition, if the track was stopped
        }

        private void saveTime(boolean saveTimeStamp) {
            SharedPreferences.Editor editor = timePrefs.edit();
            //this time has a problem - it can be changed by user
            //from the other hand System.nanoTime is valid only during a boot
            //TODO: make it right way, not depending on reboot and user time change
            if (saveTimeStamp){
                editor.putLong("startTimeStamp", System.currentTimeMillis());
            }else{
                editor.remove("startTimeStamp");
            }
            editor.putLong("timeWasPlaying", timeWasPlaying);
            if (duration == 0L){
                throw new RuntimeException("Track must have a duration.");
            }
            editor.putLong("duration", duration);
            editor.apply();
        }

        long getTimeWasPlaying() {
            return timeWasPlaying;
        }

        void setTimeWasPlaying(long timeWasPlaying) {
            this.timeWasPlaying = timeWasPlaying;
        }

        long getStartTimeStamp() {
            return startTimeStamp;
        }

        void setStartTimeStamp(long startTimeStamp) {
            this.startTimeStamp = startTimeStamp;
        }

        long getDuration() {
            return duration;
        }

        void setDuration(long duration) {
            this.duration = duration;
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(LOG_PREF, "Intent was received");
        /////////////////////////////////////////

        //IMPORTANT INIT BEFORE ANY FURTHER ACTION
        this.context = context;//possibly incorrect
        this.helper = new LogDBHelper(context);
        this.timePrefs = new TimePrefs();

        /////////////////////////////////////////

        boolean isPlaying = intent.getBooleanExtra("playing", false);

        Track previousTrack = getPreviousTrack();

        if (!isPlaying) {
            Log.v(LOG_PREF, "Track not playing");

            if (previousTrack == null) {
                Log.w(LOG_PREF, "No tracks were played and no one playing now.");
                return; //no tracks were played, neither playing now
            }else{
                if (previousTrack.status != Track.PLAYING){
                    //track was stopped already
                    Log.v(LOG_PREF, "Track was already stopped.");
                    return;
                }
                Log.v(LOG_PREF, "Assume track was paused.");
                //updateLastTrackState(previousTrack, Track.PLAYED);
                TimePrefs t = new TimePrefs();
                t.fixTimeWasPlaying();
                //save time without current timestamp
                t.saveTime(false);
            }
            return;
        }
        if (isPlaying ) {
            Track currentTrack = new Track(intent);

            if (currentTrack.duration == 0L) {
                //TODO: find duration by song id
                throw new UnsupportedOperationException("Incoming intent must contain non-null duration");
            }

            if (previousTrack == null) {
                //very rare, but still valid state
                Log.v(LOG_PREF, "There was no previous track");

                timePrefs.setDuration(currentTrack.duration);
                timePrefs.saveTime(true);
                saveCurrentTrack(currentTrack, Track.PLAYING);
                return;
            }

            if (currentTrack.equals(previousTrack)) {
               //was paused or smth
                Log.v(LOG_PREF, "Track was paused somehow.");
                timePrefs.saveTime(true);
            }else{
                //summary time of being played
                Log.v(LOG_PREF, "Previous and current track not match.");
                timePrefs.fixTimeWasPlaying();
                long timeWasPlaying = timePrefs.getTimeWasPlaying();
                if (timeWasPlaying < 0){
                    throw new RuntimeException("Incorrect time diff.");
                }

                if (previousTrack.status == Track.PLAYING){//if last track saved in PLAYING state only
                    Log.v(LOG_PREF, "Previous track was in PLAYING state.");
                    if (previousTrack.duration - timeWasPlaying > DUR_EPS){
                        //track was skipped
                        //modify previous track log
                        Log.v(LOG_PREF, "Previous track was skipped.");
                        updateLastTrackState(previousTrack, Track.SKIPPED);
                    }else if(Math.abs(previousTrack.duration - timeWasPlaying) < DUR_EPS){
                        //track was completed normally
                        Log.v(LOG_PREF, "Previous track was peacefully complete.");
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
                timePrefs = new TimePrefs(true);
                timePrefs.setDuration(currentTrack.duration);
                timePrefs.saveTime(true);
            }

        }


        }
    }


