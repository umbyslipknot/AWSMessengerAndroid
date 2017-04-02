package com.mysampleapp.chatroom.adapter;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mysampleapp.R;
import com.mysampleapp.chatroom.imageLoader.ImageLoader;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

/**
 * Created on 4/27/2016.
 */
public class ConversationAdapter extends BaseAdapter

{

    private ArrayList<HashMap<String, Object>> mapArrayList;
    private Context context;

    public static String KMsgText = "message";
    public static String KMsgType = "type";
    public static String KMsgDate = "date";
    public static String KMsgUserName = "name";
    public static String KMsgImageName = "image_name";

    public ImageLoader imageLoader;



    public ConversationAdapter(Context context) {

        this.context = context;
        mapArrayList = new ArrayList<HashMap<String, Object>>();
        imageLoader=new ImageLoader(context.getApplicationContext());
    }

    public void add(HashMap<String, Object> obj) {

        mapArrayList.add(obj);
        notifyDataSetChanged();

    }


    public String setDateWithNewFormat(String obj){

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        format.setTimeZone(TimeZone.getTimeZone("en_US_POSIX"));
        final SimpleDateFormat outputDateFormatter = new SimpleDateFormat("MMM dd, hh:mm a");
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


    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return mapArrayList.size();
    }

    @Override
    public HashMap<String, Object> getItem(int position) {
        // TODO Auto-generated method stub
        return mapArrayList.get(position);
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub

        return position;
    }

    public void removeAllData(){
        mapArrayList.clear();
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        // TODO Auto-generated method stub
        final ViewHolder holder;

        LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if (convertView == null) {
            convertView = vi.inflate(R.layout.conversation_row_item, null);
            holder = createViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        HashMap<String, Object> map = mapArrayList.get(position);


        String msgtxt = (String) map.get(KMsgText);
        String type = (String) map.get(KMsgType);
        String date = (String) map.get(KMsgDate);
        String name = (String) map.get(KMsgUserName);
        String imageUrl = (String) map.get(KMsgImageName);

        if(type != null){
            setAlignment(holder, type);

            if(imageUrl != null) {
                holder.imgView.setVisibility(View.VISIBLE);
                holder.contentWithBG.setVisibility(View.GONE);
                imageLoader.DisplayImage(imageUrl, holder.imgView, holder.progressBar);
            }else{
                holder.imgView.setVisibility(View.GONE);
                holder.progressBar.setVisibility(View.GONE);
                holder.contentWithBG.setVisibility(View.VISIBLE);
                holder.txtMessage.setText(msgtxt);
            }

            holder.txtInfo.setText(date);
            holder.txtName.setText(name);
        }

        return convertView;
    }

    private void setAlignment(ViewHolder holder, String type) {

        if (type.equals("me")) {

            holder.contentWithBG.setBackgroundResource(R.drawable.me_message_bg);

            LinearLayout.LayoutParams layoutParams =
                    (LinearLayout.LayoutParams) holder.contentWithBG.getLayoutParams();
            layoutParams.gravity = Gravity.RIGHT;
            holder.contentWithBG.setLayoutParams(layoutParams);

            RelativeLayout.LayoutParams lp =
                    (RelativeLayout.LayoutParams) holder.content.getLayoutParams();
            lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT, 0);
            lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            holder.content.setLayoutParams(lp);
            layoutParams = (LinearLayout.LayoutParams) holder.txtMessage.getLayoutParams();
            layoutParams.gravity = Gravity.RIGHT;
            holder.txtMessage.setLayoutParams(layoutParams);

            layoutParams = (LinearLayout.LayoutParams) holder.txtInfo.getLayoutParams();
            layoutParams.gravity = Gravity.RIGHT;
            layoutParams.setMargins(10,0,0,0);
            holder.txtInfo.setLayoutParams(layoutParams);

            layoutParams = (LinearLayout.LayoutParams) holder.imgView.getLayoutParams();
            layoutParams.gravity = Gravity.RIGHT;
            layoutParams.setMargins(10,0,0,0);
            holder.imgView.setLayoutParams(layoutParams);

            layoutParams = (LinearLayout.LayoutParams) holder.progressBar.getLayoutParams();
            layoutParams.gravity = Gravity.RIGHT;
            layoutParams.setMargins(10,0,0,0);
            holder.progressBar.setLayoutParams(layoutParams);

        } else {

            holder.contentWithBG.setBackgroundResource(R.drawable.other_message_bg);
            LinearLayout.LayoutParams layoutParams =
                    (LinearLayout.LayoutParams) holder.contentWithBG.getLayoutParams();
            layoutParams.gravity = Gravity.LEFT;
            holder.contentWithBG.setLayoutParams(layoutParams);

            RelativeLayout.LayoutParams lp =
                    (RelativeLayout.LayoutParams) holder.content.getLayoutParams();
            lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0);
            lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            holder.content.setLayoutParams(lp);
            layoutParams = (LinearLayout.LayoutParams) holder.txtMessage.getLayoutParams();
            layoutParams.gravity = Gravity.LEFT;
            layoutParams.setMargins(10,0,0,0);
            holder.txtMessage.setLayoutParams(layoutParams);

            layoutParams = (LinearLayout.LayoutParams) holder.txtInfo.getLayoutParams();
            layoutParams.gravity = Gravity.LEFT;
            layoutParams.setMargins(10,0,0,0);
            holder.txtInfo.setLayoutParams(layoutParams);


            layoutParams = (LinearLayout.LayoutParams) holder.imgView.getLayoutParams();
            layoutParams.gravity = Gravity.RIGHT;
            layoutParams.setMargins(10,0,0,0);
            holder.imgView.setLayoutParams(layoutParams);

            layoutParams = (LinearLayout.LayoutParams) holder.progressBar.getLayoutParams();
            layoutParams.gravity = Gravity.RIGHT;
            layoutParams.setMargins(10,0,0,0);
            holder.progressBar.setLayoutParams(layoutParams);
        }
    }

    private ViewHolder createViewHolder(View v) {

        ViewHolder holder = new ViewHolder();
        holder.txtMessage = (TextView) v.findViewById(R.id.txtMessage);
        holder.content = (LinearLayout) v.findViewById(R.id.content);
        holder.contentWithBG = (LinearLayout) v.findViewById(R.id.contentWithBackground);
        holder.txtInfo = (TextView) v.findViewById(R.id.txtInfo);
        holder.txtName = (TextView) v.findViewById(R.id.txtName);
        holder.imgView = (ImageView) v.findViewById(R.id.imgView);
        holder.progressBar = (ProgressBar) v.findViewById(R.id.progress);
        return holder;
    }

    private static class ViewHolder {

        public TextView txtMessage;
        public TextView txtInfo;
        public TextView txtName;
        public ImageView imgView;
        public LinearLayout content;
        public LinearLayout contentWithBG;
        public ProgressBar progressBar;
    }
}
