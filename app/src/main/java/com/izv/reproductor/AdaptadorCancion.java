package com.izv.reproductor;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by Aaron on 09/02/2015.
 */
public class AdaptadorCancion extends ArrayAdapter<Cancion> {
    private Context contexto;
    private ArrayList<Cancion> canciones;
    private int recurso;
    private LayoutInflater i;

    public AdaptadorCancion(Context context, int resource, ArrayList<Cancion> objects) {
        super(context, resource, objects);
        this.contexto=context;
        this.canciones=objects;
        this.recurso=resource;
        this.i= (LayoutInflater)contexto.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public class ViewHolder{
        public TextView tvTitulo,tvAutor;
        public int posicion;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder vh= null;
        //El if entra cuando se crea el ViewHolder por primera vez
        if(convertView == null){
            convertView= i.inflate(recurso, null);
            vh = new ViewHolder();
            vh.tvAutor = (TextView)convertView.findViewById(R.id.tvAutor);
            vh.tvTitulo = (TextView)convertView.findViewById(R.id.tvTitulo);
            convertView.setTag(vh);
        }
        else{
            vh=(ViewHolder)convertView.getTag();
        }
        vh.tvTitulo.setText(canciones.get(position).getTitulo());
        vh.tvAutor.setText(canciones.get(position).getAutor());
        vh.posicion=position;
        return convertView;
    }


}
