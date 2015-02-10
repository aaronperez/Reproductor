package com.izv.reproductor;

import android.net.Uri;

/**
 * Created by Aaron on 09/02/2015.
 */
public class Cancion {
    private String titulo,autor;
    private Uri uri;
    private long duracion;

    public Cancion(String titulo, String autor, Uri uri, long duracion) {
        this.titulo = titulo;
        this.autor = autor;
        this.uri = uri;
        this.duracion = duracion;

    }

    public long getDuracion() {
        return duracion;
    }

    public void setDuracion(long duracion) {
        this.duracion = duracion;
    }

    public String getAutor() {
        return autor;
    }

    public void setAutor(String autor) {
        this.autor = autor;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public Uri getUri() {
        return uri;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }
}
