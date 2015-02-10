package com.izv.reproductor;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


public class Main extends Activity implements SeekBar.OnSeekBarChangeListener {

    private static final int GETMUSICA = 1;
    private TextView tvCancion;
    private ImageButton bPlay, bAleatorio, bGrabar;
    // Listview y Adaptador
    private AdaptadorCancion ad;
    private ListView lv;
    public static ArrayList<Cancion> canciones;
    // Grabadora
    private boolean grabando=false;
    private MediaRecorder recorder = null;
    // Seekbar
    private SeekBar sb;
    private int seekMax;
    private static int songEnded = 0;
    boolean mBroadcastIsRegistered;
    public static final String BROADCAST_SEEKBAR = "com.izv.reproductor.sendseekbar";
    private Intent intentSeek;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        tvCancion = (TextView)findViewById(R.id.tvCancion);
        bPlay = (ImageButton)findViewById(R.id.ibAccion);
        bAleatorio = (ImageButton)findViewById(R.id.ibAleatorio);
        bGrabar = (ImageButton)findViewById(R.id.ibGrabar);
        sb = (SeekBar) findViewById(R.id.seekBar);
        sb.setOnSeekBarChangeListener(this);
        initcomponents();
    }

    @Override
    protected void onDestroy() {
        pararServicio();
        super.onDestroy();
    }

    private void initcomponents(){
        canciones=new ArrayList<>();
        ad = new AdaptadorCancion(this, R.layout.elemento, canciones);
        lv = (ListView) findViewById(R.id.lvPrincipal);
        lv.setAdapter(ad);
        registerForContextMenu(lv);
        cargarCanciones();
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int posicion, long id) {
                stop();
                tvCancion.setText(canciones.get(posicion).getAutor()+" - "+canciones.get(posicion).getTitulo());
                Intent intent = new Intent(Main.this, Audio.class);
                intent.putExtra("cancion", posicion);
                intent.setAction(Audio.ADD);
                startService(intent);
                bPlay.setBackground(getResources().getDrawable(R.drawable.pause));
                registerReceiver(broadcastReceiver, new IntentFilter(Audio.BROADCAST_ACTION));
                mBroadcastIsRegistered = true;
            }
        });
        // Iniciamos SeekBar
        intentSeek = new Intent(BROADCAST_SEEKBAR);
        sb = (SeekBar) findViewById(R.id.seekBar);
        sb.setOnSeekBarChangeListener(this);
    }

    // -- Broadcast Receiver to update position of seekbar from service --
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent serviceIntent) {
            updateUI(serviceIntent);
        }
    };

    private void updateUI(Intent serviceIntent) {
        String counter = serviceIntent.getStringExtra("counter");
        String mediamax = serviceIntent.getStringExtra("mediamax");
        String strSongEnded = serviceIntent.getStringExtra("song_ended");
        int seekProgress = Integer.parseInt(counter);
        seekMax = Integer.parseInt(mediamax);
        songEnded = Integer.parseInt(strSongEnded);
        sb.setMax(seekMax);
        sb.setProgress(seekProgress);
        if (songEnded == 1) {
        }
    }

    private void cargarCanciones(){
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);
        if (musicCursor != null && musicCursor.moveToFirst()) {
            int titleColumn = musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex(android.provider.MediaStore.Audio.Media.ARTIST);
            int durationColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
            do {
                long id = musicCursor.getLong(idColumn);
                String title = musicCursor.getString(titleColumn);
                String artist = musicCursor.getString(artistColumn);
                long duration= musicCursor.getLong(durationColumn);
                Uri track=getUriCancion(id);
                Log.v("uri", track.toString());
                canciones.add(new Cancion(title, artist, track, duration));
            }
            while (musicCursor.moveToNext());
            musicCursor.close();
        }
    }

    private Uri getUriCancion(long id){
        long cancion = id;
        Uri uri = ContentUris.withAppendedId(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cancion);
        return uri;
    }

    public void play(View v){
        if(!Audio.estado.equals(Audio.Estados.started)){
            Intent intent = new Intent(this, Audio.class);
            intent.setAction(Audio.PLAY);
            startService(intent);
            bPlay.setBackground(getResources().getDrawable(R.drawable.pause));
        }else if(Audio.estado.equals(Audio.Estados.started)){
            Intent intent = new Intent(this, Audio.class);
            intent.setAction(Audio.PAUSE);
            startService(intent);
            bPlay.setBackground(getResources().getDrawable(R.drawable.play));
        }
    }

    public void stop(){
        Intent intent = new Intent(this, Audio.class);
        intent.setAction(Audio.STOP);
        startService(intent);
    }

    public void next(View v){
        Intent intent = new Intent(this, Audio.class);
        intent.setAction(Audio.NEXT);
        startService(intent);
        bPlay.setBackground(getResources().getDrawable(R.drawable.pause));
    }

    public void prev(View v){
        Intent intent = new Intent(this, Audio.class);
        intent.setAction(Audio.PREV);
        startService(intent);
        bPlay.setBackground(getResources().getDrawable(R.drawable.pause));
    }

    public void aleatorio(View v){
        Intent intent = new Intent(this, Audio.class);
        intent.setAction(Audio.ALEATORIO);
        startService(intent);
        if(!Audio.aleatorio){
            bAleatorio.setBackground(getResources().getDrawable(R.drawable.repeat));
            tostada(R.string.aleatorio);
        }else{
            bAleatorio.setBackground(getResources().getDrawable(R.drawable.random));
            tostada(R.string.noaleatorio);
        }
    }

    public void grabar(View v){
        if(grabando){
            bGrabar.setBackground(getResources().getDrawable(R.drawable.rec));
            grabando=false;
            if (recorder != null) {
                recorder.stop();
                recorder.release();
                tostada(R.string.stop);
            }
        }else{
            bGrabar.setBackground(getResources().getDrawable(R.drawable.stop));
            grabando=true;
            tostada(R.string.rec);
            String archivo = "voice " + new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date()) + ".mp3";
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), archivo);
            if (recorder != null) {
                recorder.release();
            }
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_WB);
            recorder.setOutputFile(file.getAbsolutePath());
            try {
                recorder.prepare();
                recorder.start();
            } catch (IOException e) { }
        }
    }

    public void pararServicio(){
        stopService(new Intent(this, Audio.class));
    }

    public void onActivityResult(int requestCode,int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == GETMUSICA) {
                Uri uri = data.getData();
                tvCancion.setText(uri.getPath());
                Intent intent = new Intent(this, Audio.class);
                intent.putExtra("cancion", uri.getPath());
                intent.setAction(Audio.ADD);
                startService(intent);
            }
        }
    }

    /* Mostramos un mensaje flotante a partir de un recurso string*/
    private void tostada(int s){
        Toast.makeText(this, getText(s), Toast.LENGTH_SHORT).show();
    }

    /* *********  Métodos SeekBar  *****************  */
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            if (fromUser) {
                int seekPos = seekBar.getProgress();
                intentSeek.putExtra("seekpos", seekPos);
                sendBroadcast(intentSeek);
            }
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
