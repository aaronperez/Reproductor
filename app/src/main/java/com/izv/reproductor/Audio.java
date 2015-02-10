package com.izv.reproductor;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.Random;


public class Audio extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, AudioManager.OnAudioFocusChangeListener,
        MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnSeekCompleteListener{

    private MediaPlayer mp;

    public enum Estados{
        idle,
        initialized,
        preparing,
        prepared,
        started,
        paused,
        completed,
        stopped,
        end,
        error
    };
    public static Estados estado;
    private String rutaCancion=null;
    private boolean reproducir;
    private static int dato=0;
    public static boolean aleatorio=false;
    Intent intentSeek;
    int mediaPosition;
    int mediaMax;
    private static int songEnded;
    private final Handler handler = new Handler();

    public static final String PLAY = "play";
    public static final String STOP = "stop";
    public static final String ADD = "add";
    public static final String PAUSE = "pause";
    public static final String NEXT = "next";
    public static final String PREV = "prev";
    public static final String ALEATORIO = "aleatorio";
    public static final String BROADCAST_ACTION = "izv.com.reproductor.seekprogress";


    /********************************/
    /*  Interfaz prepared/listener  */
    /********************************/
    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        estado = Estados.prepared;
        if(reproducir) {
            mp.start();
            estado = Estados.started;
            Log.v("START", "true");
        }
    }


    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        estado = Estados.completed;
        Log.v("estado", estado.toString());
        if(Main.canciones.size()>0 && (aleatorio || dato<Main.canciones.size()-1)){
            if(aleatorio){
                dato= new Random().nextInt(Main.canciones.size());
            }else if(dato<Main.canciones.size()-1){
                dato++;
            }
            add(dato);
            play();
            Log.v("size", Main.canciones.size()+"");
        }else{
            stop();
        }
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                mp.setVolume(1.0f, 1.0f);
                play();
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                mp.setVolume(0.0f, 0.0f);
                break;
        }
    }

    /************************************/
    /********   Metodos SeekBar ******* */

    private void setupHandler() {
        handler.removeCallbacks(sendUpdatesToUI);
        handler.postDelayed(sendUpdatesToUI, 1000); // 1 second
    }

    private Runnable sendUpdatesToUI = new Runnable() {
        public void run() {

            LogMediaPosition();

            handler.postDelayed(this, 1000); // 2 seconds

        }
    };

    private void LogMediaPosition() {
        if (mp.isPlaying()) {
            mediaPosition = mp.getCurrentPosition();
            mediaMax = mp.getDuration();
            intentSeek.putExtra("counter", String.valueOf(mediaPosition));
            intentSeek.putExtra("mediamax", String.valueOf(mediaMax));
            intentSeek.putExtra("song_ended", String.valueOf(songEnded));
            sendBroadcast(intentSeek);
        }
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {

    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        if (!mp.isPlaying()){
            play();
        }
    }

    // --Receive seekbar position if it has been changed by the user in the
    // activity
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateSeekPos(intent);
        }
    };

    // Update seek position from Activity
    public void updateSeekPos(Intent intent) {
        int seekPos = intent.getIntExtra("seekpos", 0);
        if (mp.isPlaying()) {
            handler.removeCallbacks(sendUpdatesToUI);
            mp.seekTo(seekPos);
            setupHandler();
        }

    }


    /*****************/
    /*  CONSTRUCTOR  */
    /*****************/
    public Audio(){

}
    /***************************/
    /*  Metodos sobreescritos  */
    /***************************/
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        registerReceiver(broadcastReceiver, new IntentFilter(Main.BROADCAST_SEEKBAR));
        AudioManager am = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        int r = am.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if(r == AudioManager.AUDIOFOCUS_REQUEST_GRANTED){
            mp = new MediaPlayer();
            mp.setOnPreparedListener(this);
            mp.setOnCompletionListener(this);
            mp.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
            mp.setOnBufferingUpdateListener(this);
            mp.setOnSeekCompleteListener(this);
            intentSeek = new Intent(BROADCAST_ACTION);
            estado = Estados.idle;
        }else{
            stopSelf();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v("onStartCommad", dato+"");
        String action = intent.getAction();
        dato = intent.getIntExtra("cancion",dato);
        if(action.equals(ALEATORIO)){
            if(aleatorio)aleatorio=false;
            else aleatorio=true;
        }
        if(action.equals(PLAY)){
            play();
        }else if(action.equals(ADD)){
            add(dato);
        }else if(action.equals(STOP)){
            stop();
        }else if(action.equals(PAUSE)){
            pause();
        }else if(action.equals(NEXT)){
            next();
        }else if(action.equals(PREV)){
            prev();
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        mp.reset();
        mp.release();
        mp = null;
        super.onDestroy();
    }

    /********************************/
    /*        Metodos privados      */
    /********************************/
    private void play(){
        Log.v("play", dato+"");
        if(rutaCancion!=null){
            reproducir = true;
            /*if(estado == Estados.error){
                estado = Estados.idle;
            }
            if(estado == Estados.idle){
                try {
                    mp.setDataSource(rutaCancion);
                    estado = Estados.initialized;
                } catch (IOException e) {
                    estado = Estados.error;
                }
            }*/
            if(estado== Estados.initialized || estado == Estados.stopped){
                mp.prepareAsync();
                estado = Estados.preparing;
            }else if(estado == Estados.preparing){
                //mp.start();
            }
            if(estado == Estados.started){
                //mp.start();
            }

            if(estado == Estados.paused || estado == Estados.prepared || estado == Estados.completed){
                mp.start();
                estado = Estados.started;
            }
        }

    }

    private void pause(){
        if(estado == Estados.started){
            mp.pause();
            estado = Estados.paused;
        }
    }

    private void stop(){
        if(estado == Estados.prepared || estado == Estados.started || estado == Estados.completed || estado == Estados.paused){
            mp.seekTo(0);
            mp.stop();
            mp.reset();
            estado = Estados.stopped;
        }
        reproducir = false;
    }

    private void add(int n){
        Log.v("add", dato+"");
        this.rutaCancion=Main.canciones.get(n).getUri().toString();
        try {
            stop();
            mp.setDataSource(getApplicationContext(), Main.canciones.get(n).getUri());
            estado = Estados.initialized;
        } catch (IOException e) {
            e.printStackTrace();
        }
        play();
    }

    private void next(){
        Log.v("next", dato+"");
        if(Main.canciones.size()>0){
            if(aleatorio){
                dato= new Random().nextInt(Main.canciones.size());
                Log.v("nextAl", dato+"");
            }else if(dato<Main.canciones.size()-1){
                dato++;
                Log.v("next++", dato+"");
            }
            add(dato);
            play();
        }else{
            tostada(R.string.nocancion);
        }
    }

    private void prev(){
        if(Main.canciones.size()>0){
            if(aleatorio){
                dato= new Random().nextInt(Main.canciones.size());
            }else if(dato>0){
                dato--;
            }
            add(dato);
            play();
        }else{
            tostada(R.string.nocancion);
        }
    }


    /* Mostramos un mensaje flotante a partir de un recurso string*/
    private void tostada(int s){
        Toast.makeText(this, getText(s), Toast.LENGTH_SHORT).show();
    }

}
