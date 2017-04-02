package com.mysampleapp.chatroom.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobile.AWSConfiguration;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.PaginatedScanList;
import com.mysampleapp.R;
import com.mysampleapp.chatroom.ChatRoomManager;
import com.mysampleapp.chatroom.ChatUserProfileManager;
import com.mysampleapp.chatroom.adapter.ContactsAdapter;
import com.amazonaws.models.nosql.UserProfileDO;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

public class CreateChatRoomActivity extends AppCompatActivity {

    private EditText groupName;
    private Button addChatRoom;
    private ListView usersListView;

    private ContactsAdapter contactsAdapter;

    private PaginatedScanList<UserProfileDO> awsRecipientUsers;
    private  CognitoCachingCredentialsProvider credentialsProvider;

    private String contactList;
    private String randomUUIDString;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_create_chatroom);

        //initialize xml views
        initialize();

        setupToolbar(0);

        // Obtain a reference to the CognitoCachingCredentialsProvider.
         credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                AWSConfiguration.AMAZON_COGNITO_IDENTITY_POOL_ID, // Identity Pool ID
                AWSConfiguration.AMAZON_DYNAMODB_REGION   // Region
        );
        contactsAdapter = new ContactsAdapter(this);

        Bundle bundle = getIntent().getExtras();

        if (bundle != null) {
            contactList = bundle.getString("ContactList");
              if(contactList.length()>0){
                  String[] tokens = contactList.split("/n");
                  for (String str : tokens)
                  {
                      String[] parts = str.split(":");
                      String id = parts[0];
                      String ph = parts[1];
                      String name = parts[2];
                      contactsAdapter.add(id, ph, name);
                  }
                  usersListView.setAdapter(contactsAdapter);
                  setupToolbar(1);
              }
        }
    }

    private void initialize(){

        groupName = (EditText) findViewById(R.id.group_name_editText);
        addChatRoom = (Button) findViewById(R.id.add_chatRoom_btn);
        usersListView = (ListView) findViewById(R.id.listView);

    }


    private void setupToolbar(int i) {

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

        title.setText(R.string.title_activity_createChatRoom);

      if(i == 0){
            forwardButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(CreateChatRoomActivity.this, ContactsSelectionActivity.class);
                    startActivity(intent);
                    finish();
                }
            });
      }else{
        forwardButton.setVisibility(View.INVISIBLE);
      }
    }

    public void createChatRoom(View view){
          if( contactsAdapter.phNoList.size()>0){

              new AsyncTask<Void, Void, String>() {
                  @Override
                  protected String doInBackground(Void... params) {
                      String msg = "";
                      try {
                          ChatUserProfileManager userProfileManager = new ChatUserProfileManager();
                          awsRecipientUsers =  userProfileManager.loadUsersWithPhoneList(credentialsProvider, contactsAdapter.phNoList.toArray());

                      } catch (AmazonServiceException ex) {
                          msg = ex.getLocalizedMessage();
                          Log.e("CustomError", "---> " + ex.getLocalizedMessage());
                      }catch (Exception ex){
                          msg = ex.getMessage();
                      }
                      return msg;
                  }

                  @Override
                  protected void onPostExecute(String msg) {
                   if(awsRecipientUsers != null){
                       if( awsRecipientUsers.size() != 0){

                           new AsyncTask<Void, Void, String>() {
                               @Override
                               protected String doInBackground(Void... params) {
                                   String msg = "";
                                   try {
                                       // For current date
                                       Calendar cur_cal = Calendar.getInstance();
                                       Date dt = cur_cal.getTime();
                                       SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                                       dateFormat.setTimeZone(TimeZone.getTimeZone("en_US_POSIX"));

                                       //for chat room id
                                       UUID uuid = UUID.randomUUID();
                                       randomUUIDString = uuid.toString();

                                       ChatRoomManager chatRoomManager = new ChatRoomManager();
                                       // create hash set
                                       Set<String> endpointSet = new HashSet<String>();
                                       for(UserProfileDO userProfileDO : awsRecipientUsers){
                                           endpointSet.add(userProfileDO.getUserId());
                                       }
                                       chatRoomManager.saveNewChatRoom(dateFormat.format(dt), credentialsProvider, groupName.getText().toString(), endpointSet, randomUUIDString);

                                   } catch (AmazonServiceException ex) {
                                       msg = ex.getLocalizedMessage();
                                       Log.e("CustomError", "---> " + ex.getLocalizedMessage());
                                   }catch (Exception ex){
                                       msg = ex.getMessage();
                                   }
                                   return msg;
                               }

                               @Override
                               protected void onPostExecute(String msg) {
                                   Intent intent = new Intent(CreateChatRoomActivity.this, ChatActivity.class);
                                   intent.putExtra("ID",randomUUIDString);
                                   intent.putExtra("Flag",false);
                                   startActivity(intent);
                                   finish();

                               }
                           }.execute();

                       }
                   }
                  }
              }.execute();

          }else {
              new AlertDialog.Builder(CreateChatRoomActivity.this)
                      .setTitle(R.string.error)
                      .setMessage(R.string.error_message)
                      .setPositiveButton(android.R.string.ok, null)
                      .show();
          }
    }
}
