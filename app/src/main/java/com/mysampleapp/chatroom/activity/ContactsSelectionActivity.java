package com.mysampleapp.chatroom.activity;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.ContactsContract;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobile.AWSConfiguration;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.PaginatedScanList;
import com.amazonaws.models.nosql.UserProfileDO;
import com.mysampleapp.R;
import com.mysampleapp.chatroom.ChatUserProfileManager;
import com.mysampleapp.chatroom.adapter.ContactsAdapter;

import java.util.ArrayList;

public class ContactsSelectionActivity extends AppCompatActivity implements  ContactsAdapter.CapabilityCheckClickedDelegate{

    // Request code for READ_CONTACTS. It can be any number > 0.
    private static final int PERMISSIONS_REQUEST_READ_CONTACTS = 100;

    private ListView addressBookListView;
    private Button selectBtn;
    private StringBuilder checkedContacts;

    private ContactsAdapter contactsAdapter;

    private  CognitoCachingCredentialsProvider credentialsProvider;
    private PaginatedScanList<UserProfileDO> awsUserExist;
    private ArrayList<PaginatedScanList<UserProfileDO>> recipientUsers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);

        //initialize xml views
        initialize();

        setupToolbar();

        contactsAdapter = new ContactsAdapter(this);
        //read contact permission
        checkReadContactPermission();

        recipientUsers = new ArrayList<>();

        // Obtain a reference to the CognitoCachingCredentialsProvider.
        credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                AWSConfiguration.AMAZON_COGNITO_IDENTITY_POOL_ID, // Identity Pool ID
                AWSConfiguration.AMAZON_DYNAMODB_REGION   // Region
        );

    }

    private void initialize(){

        addressBookListView  = (ListView) findViewById(R.id.book_list_View);
        selectBtn = (Button) findViewById(R.id.selectButton);
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

        title.setText(R.string.title_activity_contacts);
        forwardButton.setVisibility(View.INVISIBLE);
        backButton.setVisibility(View.INVISIBLE);
    }


    public void selectButtonClicked(View view){

        checkedContacts = new StringBuilder();
        if( contactsAdapter.phNoList.size()>0) {
            for (int i = 0; i < contactsAdapter.getCount(); i++) {

                if (contactsAdapter.mCheckStates.get(i) == true) {

                    checkedContacts.append(contactsAdapter.idList.get(i).toString() + ":" + contactsAdapter.phNoList.get(i).toString() + ":" + contactsAdapter.nameList.get(i).toString());
                    checkedContacts.append("/n");

                }
            }
            Intent intent = new Intent(ContactsSelectionActivity.this, CreateChatRoomActivity.class);
            intent.putExtra("ContactList", checkedContacts.toString());
            startActivity(intent);
            finish();
        }
    }

    private void addAddressBook() {

        Cursor cur = this.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
        while (cur.moveToNext())
        {
            String name=cur.getString(cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            String number = cur.getString(cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            String id = cur.getString(cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID));

            number = number.replaceAll("[^0-9\\+]", "");
            contactsAdapter.add(id,number,name);

        }
        cur.close();

        addressBookListView.setAdapter(contactsAdapter);
    }


    @TargetApi(23)
    private void checkReadContactPermission(){

        // Check the SDK version and whether the permission is already granted or not.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, PERMISSIONS_REQUEST_READ_CONTACTS);
            //After this point you wait for callback in onRequestPermissionsResult(int, String[], int[]) overriden method
        } else {
            // Android version is lesser than 6.0 or the permission is already granted.
            addAddressBook();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_READ_CONTACTS) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission is granted
                addAddressBook();
            } else {
                Toast.makeText(this, "Until you grant the permission, we cannot display the names", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onCapabilityCheckClicked(int position, boolean isChecked) {
        contactsAdapter.mCheckStates.put(position, isChecked);
        if(isChecked == true){
            isUserRegistered(position);
        }
    }

    public void isUserRegistered(final int position) {

        final ChatUserProfileManager userProfileManager = new ChatUserProfileManager();

        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String msg = "";
                try {
                    //load current user chat rooms
                    awsUserExist  = userProfileManager.awsUserExist(credentialsProvider, contactsAdapter.phNoList.get(position));
                } catch (AmazonServiceException ex) {
                    msg = ex.getLocalizedMessage();
                    Log.e("CustomError", "---> " + ex.getLocalizedMessage());
                }catch (Exception ex){
                    msg = ex.getMessage();
                }
                return msg;
            }

            @Override
            protected void onPostExecute(String msg)
            {
              if(awsUserExist.size() == 0){
                  new AlertDialog.Builder(ContactsSelectionActivity.this)
                          .setTitle(R.string.app_name)
                          .setMessage(contactsAdapter.nameList.get(position)+" is not registered")
                          .setPositiveButton(android.R.string.ok, null)
                          .show();
                  contactsAdapter.mCheckStates.put(position, false);
              }contactsAdapter.notifyDataSetChanged();
            }
        }.execute();
    }
}
