package com.mysampleapp.chatroom.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.mysampleapp.R;

import java.util.ArrayList;

/**
 * Created on 6/14/2016.
 */
public class SettingsAdapter extends BaseAdapter {

    LayoutInflater mInflater;
    Context context;
    private ArrayList<String> data;

    public SettingsAdapter(Context context) {
        this.context = context;
        data = new ArrayList<String>();
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    }

    public void add(String value) {
        data.add(value);
    }


    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return data.size();
    }

    @Override
    public String getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub

        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        // TODO Auto-generated method stub

        View vi=convertView;
        if(convertView==null) vi = mInflater.inflate(R.layout.settings_row_item, null);

        TextView name = (TextView) vi.findViewById(R.id.txtName);
        ImageView image = (ImageView) vi.findViewById(R.id.imageView);

        image.setImageResource(R.drawable.ic_lock_power_off);
        name.setText(getItem(position));

        return vi;
    }

}
