package com.mysampleapp.chatroom.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.amazonaws.mobile.AWSMobileClient;
import com.amazonaws.mobile.user.IdentityManager;
import com.amazonaws.mobile.user.IdentityProvider;
import com.mysampleapp.R;
import com.mysampleapp.chatroom.adapter.ConversationAdapter;
import com.mysampleapp.chatroom.adapter.SettingsAdapter;

public class SettingsActivity extends AppCompatActivity{

    private ListView listView;
    private TextView  txtName;
    private ImageView imageView;

    private IdentityManager identityManager;
    private IdentityProvider identityProvider;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        //initialize xml views
        initialize();

        identityManager =
                AWSMobileClient.defaultMobileClient().getIdentityManager();
        identityProvider =
                identityManager.getCurrentIdentityProvider();

        //add toolbar
        setupToolbar();

        userProfile();
        //View Settings in List View
        settingList();

    }

    private void settingList() {

        SettingsAdapter settingsListAdapter = new SettingsAdapter(this);
        settingsListAdapter.add("Log Out");
        listView.setAdapter(settingsListAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id){
                identityManager.signOut();
                startActivity(new Intent(SettingsActivity.this, LoginActivity.class));
                finish();
            }
        });
    }

    private void initialize(){

        txtName = (TextView) findViewById(R.id.txtName);
        imageView = (ImageView) findViewById(R.id.imageView);
        listView  = (ListView) findViewById(R.id.listView);

    }

    private void setupToolbar() {

        getSupportActionBar().setDisplayOptions(android.support.v7.app.ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setCustomView(R.layout.custom_actionbar);
        View view =getSupportActionBar().getCustomView();
        Toolbar parent =(Toolbar) view.getParent();
        parent.setContentInsetsAbsolute(0,0);

        final ImageButton backButton = (ImageButton)view.findViewById(R.id.action_bar_back);
        TextView title = (TextView) view.findViewById(R.id.lblTitle);
        //go to contacts selection screen
        ImageButton forwardButton = (ImageButton)view.findViewById(R.id.action_bar_forward);

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });//closing the setOnClickListener method

        title.setText(R.string.title_activity_settings);
        forwardButton.setVisibility(View.INVISIBLE);
    }

    private void userProfile() {

        if (identityProvider == null) {
            // Not signed in
            if (Build.VERSION.SDK_INT < 22) {
                imageView.setImageBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.user));
            }
            else {
                imageView.setImageDrawable(getDrawable(R.drawable.user));
            }

            return;
        }

        final Bitmap userImage = identityManager.getUserImage();
        if (userImage != null) {
            Bitmap circleImageView = getRoundedShape(userImage);
            imageView.setImageBitmap(circleImageView);
        }

        String userName = identityManager.getUserName();
        if(userName != null){
            txtName.setText(userName);
        }
    }


    public Bitmap getRoundedShape(Bitmap bitmap) {
        Bitmap output;

        if (bitmap.getWidth() > bitmap.getHeight()) {
            output = Bitmap.createBitmap(bitmap.getHeight(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        } else {
            output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getWidth(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

        float r = 0;

        if (bitmap.getWidth() > bitmap.getHeight()) {
            r = bitmap.getHeight() / 2;
        } else {
            r = bitmap.getWidth() / 2;
        }

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawCircle(r, r, r, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return output;
    }
}
