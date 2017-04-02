package com.mysampleapp.chatroom.adapter;

import android.content.Context;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;
import com.mysampleapp.R;
import com.mysampleapp.chatroom.activity.ContactsSelectionActivity;
import com.mysampleapp.chatroom.activity.CreateChatRoomActivity;
import java.util.ArrayList;

/**
 * Created on 4/19/2016.
 */
public class ContactsAdapter extends BaseAdapter

{
    LayoutInflater mInflater;

    public ArrayList<String> nameList;
    public ArrayList<String> phNoList;
    public ArrayList<String> idList;
    public SparseBooleanArray mCheckStates;
    private boolean flag;
    public CapabilityCheckClickedDelegate delegate;

    public ContactsAdapter(ContactsSelectionActivity contactsSelectionActivity) {

        nameList = new  ArrayList<String>();
        phNoList = new  ArrayList<String>();
        idList = new  ArrayList<String>();
        this.delegate = contactsSelectionActivity;
        mCheckStates = new SparseBooleanArray(idList.size());
        flag = true;
        mInflater = (LayoutInflater) contactsSelectionActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    }

    public ContactsAdapter(CreateChatRoomActivity createChatRoomActivity) {

        nameList = new  ArrayList<String>();
        phNoList = new  ArrayList<String>();
        idList = new  ArrayList<String>();
        mInflater = (LayoutInflater) createChatRoomActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void add(String id, String number, String name) {

        idList.add(id);
        phNoList.add(number);
        nameList.add(name);

        notifyDataSetChanged();

    }

    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return idList.size();
    }

    @Override
    public Object getItem(int position) {
        // TODO Auto-generated method stub
        return position;
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
        if(convertView==null) vi = mInflater.inflate(R.layout.contact_row_item, null);
        TextView name = (TextView) vi.findViewById(R.id.txtName);
        TextView phno = (TextView) vi.findViewById(R.id.txtPhNo);
        CheckBox chekbox= (CheckBox) vi.findViewById(R.id.checkBox);
        name.setText(nameList.get(position));

        if (flag) {
            phno.setText(phNoList.get(position));
            chekbox.setTag(String.valueOf(position));
            chekbox.setChecked(mCheckStates.get(position, false));
            chekbox.setOnClickListener(chkClickListener);

        }else{

            chekbox.setVisibility(View.GONE);
            phno.setVisibility(View.GONE);
        }

        return vi;
    }

    private View.OnClickListener chkClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            CheckBox chk = (CheckBox) view;
            int position = Integer.valueOf(((String) chk.getTag()));

            if (delegate != null) {
                delegate.onCapabilityCheckClicked(position, chk.isChecked());
            }
        }
    };

    public interface CapabilityCheckClickedDelegate {
        public void onCapabilityCheckClicked(int position, boolean isChecked);
    }

}
