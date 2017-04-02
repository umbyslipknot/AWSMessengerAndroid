package com.mysampleapp.chatroom.adapter;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.amazonaws.models.nosql.ChatRoomDO;
import com.mysampleapp.R;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created on 4/28/2016.
 */

public class ChatRoomListAdapter extends BaseAdapter {

    LayoutInflater mInflater;
    Context context;
    private ArrayList<ChatRoomDO> data;

    public ChatRoomListAdapter(Context context) {
        this.context = context;
        data = new ArrayList<ChatRoomDO>();
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    }

    public void add(ChatRoomDO obj) {
        data.add(obj);
        notifyDataSetChanged();
    }

    public void sort(){

        final SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Collections.sort(data, new Comparator<ChatRoomDO>() {
            public int compare(ChatRoomDO obj2, ChatRoomDO obj1) {
                try {

                    return format.parse((String) obj1.getCreatedAt()).compareTo(format.parse((String) obj2.getCreatedAt()));

                } catch (ParseException e) {
                    e.printStackTrace();
                }
                return 0;
            }
        });
    }

    public String setDateWithNewFormat(String obj){

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        format.setTimeZone(TimeZone.getTimeZone("en_US_POSIX"));

        final SimpleDateFormat outputDateFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        //for current date and time
        Calendar cur_cal = Calendar.getInstance();
        TimeZone tz = cur_cal.getTimeZone();
        outputDateFormatter.setTimeZone(tz);

        try {
            Date date = format.parse(String.valueOf(obj));
            String formattedDate = outputDateFormatter.format(date);
            return formattedDate;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getId(int position){
        return getItem(position).getChatRoomId();
    }

    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return data.size();
    }

    @Override
    public ChatRoomDO getItem(int position) {
        // TODO Auto-generated method stub
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
        if(convertView==null) vi = mInflater.inflate(R.layout.dashboard_row_item, null);

        TextView name = (TextView) vi.findViewById(R.id.txtName);
        TextView date = (TextView) vi.findViewById(R.id.txtDate);
        ImageView imageView = (ImageView) vi.findViewById(R.id.imageView);

        if (Build.VERSION.SDK_INT < 22) {
            imageView.setImageBitmap(BitmapFactory.decodeResource(context.getResources(), R.drawable.user));
        }
        else {
            imageView.setImageDrawable(context.getDrawable(R.drawable.user));
        }

        imageView.setImageBitmap(BitmapFactory.decodeResource(context.getResources(), R.drawable.user));
        ChatRoomDO obj = getItem(position);
        name.setText(obj.getName());

        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        try {
            Date date1 = format.parse(String.valueOf(obj.getCreatedAt()));
            SimpleDateFormat outputDateFormatter = new SimpleDateFormat("dd/MM/yyyy");
            String formattedDate = outputDateFormatter.format(date1);
            date.setText(formattedDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return vi;
    }

    public void removeAll() {
        data.clear();
    }
}
