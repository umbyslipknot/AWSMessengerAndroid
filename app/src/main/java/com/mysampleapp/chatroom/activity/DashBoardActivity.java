package com.mysampleapp.chatroom.activity;


import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobile.AWSConfiguration;
import com.amazonaws.mobile.AWSMobileClient;
import com.amazonaws.mobile.user.IdentityManager;
import com.amazonaws.mobile.user.IdentityManager.SignInResultsHandler;
import com.amazonaws.mobile.user.IdentityProvider;
import com.amazonaws.mobile.user.signin.SignInManager;
import com.amazonaws.mobile.user.signin.SignInProvider;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.PaginatedScanList;
import com.mysampleapp.PushListenerService;
import com.mysampleapp.R;
import com.mysampleapp.SplashActivity;
import com.mysampleapp.chatroom.ChatRoomManager;
import com.mysampleapp.chatroom.adapter.ChatRoomListAdapter;
import com.amazonaws.models.nosql.ChatRoomDO;

public class DashBoardActivity extends AppCompatActivity{

    private final static String LOG_TAG = DashBoardActivity.class.getSimpleName();

    private SignInManager signInManager;
    /** The CognitoCachingCredentialsProvider  used to keep track of the current user account. */
    private  CognitoCachingCredentialsProvider credentialsProvider;

    private PaginatedScanList<ChatRoomDO> awsChatRoomsList;

    private ListView chatListView;
    private ChatRoomListAdapter chatRoomListAdapter;
    private boolean userSignIn;
    private ProgressBar progressBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_dash_board);

        //initialize xml views
        initialize();
        heartBeat(this);
        //add toolbar
        setupToolbar();

        View v = new View(this);
        v.setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, 1));
        chatListView.addFooterView(v);

        final Thread thread = new Thread(new Runnable() {
            public void run() {
                signInManager = SignInManager.getInstance(DashBoardActivity.this);

                final SignInProvider provider = signInManager.getPreviouslySignedInProvider();

                // if the user was already previously in to a provider.
                if (provider != null) {
                    // asynchronously handle refreshing credentials and call our handler.
                    signInManager.refreshCredentialsWithProvider(DashBoardActivity.this,
                            provider, new IdentityManager.SignInResultsHandler() {
                                @Override
                                public void onSuccess(IdentityProvider provider) {
                                    Log.d(LOG_TAG, String.format("User sign-in with previous %s provider succeeded",
                                            provider.getDisplayName()));

                                    // Obtain a reference to the CognitoCachingCredentialsProvider.
                                    credentialsProvider = new CognitoCachingCredentialsProvider(
                                            DashBoardActivity.this,
                                            AWSConfiguration.AMAZON_COGNITO_IDENTITY_POOL_ID, // Identity Pool ID
                                            AWSConfiguration.AMAZON_DYNAMODB_REGION   // Region
                                    );
                                    // The sign-in manager is no longer needed once signed in.
                                    SignInManager.dispose();

                                    AWSMobileClient.defaultMobileClient()
                                            .getIdentityManager()
                                            .loadUserInfoAndImage(provider, new Runnable() {
                                                @Override
                                                public void run() {
                                                    chatRoomListAdapter = new ChatRoomListAdapter(DashBoardActivity.this);
                                                    addChatRoomList();
                                                    userSignIn = true;
                                                }
                                            });

                                }
                                @Override
                                public void onCancel(IdentityProvider provider) {

                                }

                                @Override
                                public void onError(IdentityProvider provider, Exception ex) {

                                }
                            });
                }
            }
        });
        thread.start();

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!AWSMobileClient.defaultMobileClient().getIdentityManager().isUserSignedIn()) {
            // In the case that the activity is restarted by the OS after the application
            // is killed we must redirect to the splash activity to handle the sign-in flow.
            Intent intent = new Intent(this, SplashActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return;
        }

        final AWSMobileClient awsMobileClient = AWSMobileClient.defaultMobileClient();

        // pause/resume Mobile Analytics collection
        awsMobileClient.handleOnResume();

        if(userSignIn == true){
            addChatRoomList();
        }

        //register notification receiver
        LocalBroadcastManager.getInstance(this).registerReceiver(notificationReceiver,
                new IntentFilter(PushListenerService.ACTION_SNS_NOTIFICATION));

    }

    @Override
    protected void onPause() {
        super.onPause();
        // unregister notification receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(notificationReceiver);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        // Handle action bar item clicks here excluding the home button.

        return super.onOptionsItemSelected(item);
    }


    private final BroadcastReceiver notificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            final Bundle data = intent.getBundleExtra(PushListenerService.INTENT_SNS_NOTIFICATION_DATA);
            new AlertDialog.Builder(DashBoardActivity.this)
                    .setTitle(R.string.push_demo_title)
                    .setMessage(data.getString("message"))
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //call conversation activity
                            Intent intent = new Intent(DashBoardActivity.this,ChatActivity.class);
                            intent.putExtra("ID",data.getString("chatRoomId"));
                            startActivity(intent);
                        }
                    })
                    .show();

        }
    };

    private void initialize(){

        chatListView  = (ListView) findViewById(R.id.chat_list_View);
        progressBar = (ProgressBar) findViewById(R.id.progress);

    }


    private void setupToolbar() {

        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setCustomView(R.layout.chat_room_actionbar);
        View view =getSupportActionBar().getCustomView();
        Toolbar parent =(Toolbar) view.getParent();
        parent.setContentInsetsAbsolute(0,0);

        final ImageButton settingsButton = (ImageButton)view.findViewById(R.id.action_bar_settings);
        TextView title = (TextView) view.findViewById(R.id.lblTitle);
        //go to create chat room screen
        ImageButton forwardButton = (ImageButton)view.findViewById(R.id.action_bar_forward);

        title.setText(R.string.title_activity_chat_room);

        forwardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DashBoardActivity.this, CreateChatRoomActivity.class);
                startActivity(intent);
            }
        });

        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DashBoardActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });
    }


    public void addChatRoomList() {

        final ChatRoomManager chatRoomManager = new ChatRoomManager();
        if(chatRoomListAdapter.getCount()>0){
            chatRoomListAdapter.removeAll();
            chatRoomListAdapter.notifyDataSetChanged();
        }


        new AsyncTask<Void, Void, String>() {
            protected void onPreExecute()
            {
                progressBar.setVisibility(View.VISIBLE);
            }
                @Override
            protected String doInBackground(Void... params) {
                String msg = "";
                try {
                    //load current user chat rooms
                    awsChatRoomsList  = chatRoomManager.loadUserChatRooms(credentialsProvider);
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
                    progressBar.setVisibility(View.GONE);
                    showChatRooms();
                }
            }.execute();
    }


    private void showChatRooms(){

        if(awsChatRoomsList.size() != 0){
            //change and set date format in ChatRoomDO
            for ( ChatRoomDO chatRoom : awsChatRoomsList) {
                String changeDate = chatRoomListAdapter.setDateWithNewFormat(chatRoom.getCreatedAt());
                chatRoom.setCreatedAt(changeDate);
            }

            for ( ChatRoomDO chatRoom : awsChatRoomsList) {
                chatRoomListAdapter.add(chatRoom);
            }

            chatRoomListAdapter.sort();
            chatListView.setAdapter(chatRoomListAdapter);
            chatRoomListAdapter.notifyDataSetChanged();

            chatListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                    //call conversation activity
                    Intent intent = new Intent(DashBoardActivity.this, ChatActivity.class);
                    intent.putExtra("ID", chatRoomListAdapter.getId(position));
                    intent.putExtra("Flag", false);
                    startActivity(intent);
                }
            });
        }
    }

    private void heartBeat(Context context){
        context.sendBroadcast(new Intent("com.google.android.intent.action.GTALK_HEARTBEAT"));
        context.sendBroadcast(new Intent("com.google.android.intent.action.MCS_HEARTBEAT"));
    }
}
