package com.mysampleapp.chatroom.activity;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobile.AWSConfiguration;
import com.amazonaws.mobile.AWSMobileClient;
import com.amazonaws.mobile.content.ContentItem;
import com.amazonaws.mobile.content.ContentProgressListener;
import com.amazonaws.mobile.content.UserFileManager;
import com.amazonaws.mobile.user.IdentityManager;
import com.amazonaws.mobile.util.ImageSelectorUtils;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.PaginatedQueryList;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.PaginatedScanList;
import com.amazonaws.regions.Region;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.mysampleapp.PushListenerService;
import com.mysampleapp.R;
import com.mysampleapp.chatroom.ChatRoomManager;
import com.mysampleapp.chatroom.ChatUserProfileManager;
import com.mysampleapp.chatroom.ConversationManager;
import com.mysampleapp.chatroom.adapter.ConversationAdapter;
import com.amazonaws.models.nosql.ChatRoomDO;
import com.amazonaws.models.nosql.ConversationDO;
import com.amazonaws.models.nosql.UserProfileDO;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

public class ChatActivity extends AppCompatActivity {

    /** The s3 Prefix at which the UserFileManager is rooted. */
    private static final String S3_PREFIX_PRIVATE = "private/";

    // Request code for READ_EXTERNAL_STORAGE. It can be any number > 0.
    private static final int PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 100;

    private PaginatedQueryList<ConversationDO> awsConversationList;
    private PaginatedScanList<UserProfileDO> awsUsersData;
    private PaginatedScanList<ChatRoomDO> awsSelectedChatRoom;

    private ConversationManager conversationManager;

    private IdentityManager identityManager;
    private CognitoCachingCredentialsProvider cachingCredentialsProvider;
    /** The user file manager. */
    private UserFileManager userFileManager;

    private ListView conversationListView;
    private EditText messageBox;
    private Button sendBtn;
    private ProgressBar progressBar;
    private ImageButton imgSelection;


    private ConversationAdapter conversationAdapter;


    private String uploadedImagePath = "";
    private String chatRoomId;

    private boolean unScaledBitmap;
    private boolean flag;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        //initialize xml views
        initialize();

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            chatRoomId = bundle.getString("ID");
            flag = bundle.getBoolean("Flag");
        }

        cachingCredentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                AWSConfiguration.AMAZON_COGNITO_IDENTITY_POOL_ID, // Identity Pool ID
                AWSConfiguration.AMAZON_S3_USER_FILES_BUCKET_REGION  // Region
        );
        identityManager = AWSMobileClient.defaultMobileClient().getIdentityManager();
        getUserInformation();

        conversationManager = new ConversationManager();
        conversationAdapter = new ConversationAdapter(ChatActivity.this);

        final String identityId = AWSMobileClient.defaultMobileClient()
                .getIdentityManager()
                .getCredentialsProvider()
                .getCachedIdentityId();

        AWSMobileClient.defaultMobileClient()
                .createUserFileManager(AWSConfiguration.AMAZON_S3_USER_FILES_BUCKET, S3_PREFIX_PRIVATE + identityId + "/", AWSConfiguration.AMAZON_S3_USER_FILES_BUCKET_REGION,
                        new UserFileManager.BuilderResultHandler() {
                            @Override
                            public void onComplete(final UserFileManager userFileManager) {
                                ChatActivity.this.userFileManager = userFileManager;

                            }
                        });

        setupToolbar();

        messageBox.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                messageBox.setFocusable(true);
                messageBox.setFocusableInTouchMode(true);
                return false;
            }
        });

        //read storage permission
        checkReadStoragePermission();

    }


    @Override
    protected void onResume() {
        super.onResume();
        // register notification receiver
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
    public void onDestroy()
    {
        if(flag) {
            Intent intent = new Intent(ChatActivity.this, DashBoardActivity.class);
            startActivity(intent);
        }
        super.onDestroy();
}


    private final BroadcastReceiver notificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            Bundle data = intent.getBundleExtra(PushListenerService.INTENT_SNS_NOTIFICATION_DATA);
            chatRoomId = data.getString("chatRoomId");
            if(chatRoomId != null){loadChat();}
        }
    };

    private void initialize(){

        sendBtn = (Button) findViewById(R.id.chatSendButton);
        imgSelection = (ImageButton) findViewById(R.id.imageSelectionButton);
        messageBox = (EditText) findViewById(R.id.messageEdit);
        conversationListView = (ListView) findViewById(R.id.messagesContainer_lstView);
        progressBar = (ProgressBar) findViewById(R.id.progress);

    }

    @TargetApi(23)
    private void checkReadStoragePermission(){

        // Check the SDK version and whether the permission is already granted or not.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
            //After this point you wait for callback in onRequestPermissionsResult(int, String[], int[]) overriden method
        } else {
            // Android version is lesser than 6.0 or the permission is already granted.

        }
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

        title.setText(R.string.title_activity_conversation);
        forwardButton.setVisibility(View.INVISIBLE);
    }


    private void scroll() {
        conversationListView.setSelection(conversationListView.getCount() - 1);
    }

    private void getUserInformation() {

        final CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                AWSConfiguration.AMAZON_COGNITO_IDENTITY_POOL_ID, // Identity Pool ID
                AWSConfiguration.AMAZON_DYNAMODB_REGION   // Region
        );
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String msg = "";
                try {
                    ChatRoomManager  chatRoomManager = new ChatRoomManager();
                    //getting chat room object with selected chat room id
                    if(chatRoomId != null){
                        awsSelectedChatRoom = chatRoomManager.getRecipientUsersList(credentialsProvider, chatRoomId);
                    }


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

                if(awsSelectedChatRoom != null)  {
                    new AsyncTask<Void, Void, String>() {
                        @Override
                        protected String doInBackground(Void... params) {
                            String msg = "";
                            try {

                                ChatUserProfileManager userProfileManager = new ChatUserProfileManager();
                                awsUsersData = userProfileManager.getUserProfileList(credentialsProvider, awsSelectedChatRoom);

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
                            if(awsUsersData != null){
                                //load conversation
                                loadChat();
                            }
                        }
                    }.execute();
                }
            }
        }.execute();
    }

    public void imageSelectionButtonClick(View view){
        final Intent intent = ImageSelectorUtils.getImageSelectionIntent();
        startActivityForResult(intent, 0);
    }

    public void onChatSendClicked(View view){

        final String[] message = new String[1];
        String imageUrl = null;
        final HashMap<String, Object> map = new HashMap<String, Object>(4);
        // For current date
        Calendar cur_cal = Calendar.getInstance();
        final Date dt = cur_cal.getTime();
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        dateFormat.setTimeZone(TimeZone.getTimeZone("en_US_POSIX"));
        UUID uuid = UUID.randomUUID();
        final String randomUUIDString = uuid.toString();
        message[0] = messageBox.getText().toString();
        if(!message[0].isEmpty() || !uploadedImagePath.isEmpty()){
            String changeDate = conversationAdapter.setDateWithNewFormat(dateFormat.format(dt));
            if(message[0].isEmpty()){
                imageUrl = uploadedImagePath;

            }
            map.put(conversationAdapter.KMsgImageName, imageUrl);
            map.put(conversationAdapter.KMsgText, message[0]);
            map.put(conversationAdapter.KMsgDate,changeDate);
            map.put(conversationAdapter.KMsgUserName, "Me");
            map.put(conversationAdapter.KMsgType, "me");
            conversationAdapter.add(map);
            conversationListView.setAdapter(conversationAdapter);
            conversationAdapter.notifyDataSetChanged();
            scroll();
            messageBox.setText("");}

        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String msg = "";
                try {
                    if(!message[0].isEmpty() || !uploadedImagePath.isEmpty()){
                    //aws call for saving data into conversation table
                    conversationManager.addChatRoomConversation(cachingCredentialsProvider, message[0], dateFormat.format(dt), chatRoomId, uploadedImagePath, randomUUIDString);}
                    Log.d("AWS", msg);

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
                if(!message[0].isEmpty() || !uploadedImagePath.isEmpty()){
                    message[0] = "";
                    uploadedImagePath = "";
                    sendPush();
                }

            }
      }.execute();
    }

    private void loadChat(){

        new AsyncTask<Void, Void, String>() {
            protected void onPreExecute()
            {
                progressBar.setVisibility(View.VISIBLE);
            }
            @Override
            protected String doInBackground(Void... params) {
                String msg = "";
                try {
                    //aws server call for get chat room conversation
                     awsConversationList = conversationManager.getChatRoomConversation(cachingCredentialsProvider, chatRoomId);

                    Log.i("AWS", msg);
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

                if(awsConversationList != null){

                    conversationAdapter.removeAllData();
                    //change date format
                    for ( ConversationDO con : awsConversationList) {
                        String changeDate = conversationAdapter.setDateWithNewFormat(con.getCreatedAt());
                        con.setCreatedAt(changeDate);
                    }


                    //object for getting list of conversation
                    for ( final ConversationDO con : awsConversationList ) {

                        HashMap<String, Object> map = new HashMap<String, Object>(4);

                        map.put(conversationAdapter.KMsgImageName, con.getImageUrlPath());
                        map.put(conversationAdapter.KMsgText, con.getMessage());
                        map.put(conversationAdapter.KMsgDate, con.getCreatedAt());

                          for(UserProfileDO userData : awsUsersData) {
                              if (userData.getUserId().equals(con.getUserId())) {

                                  if (userData.getUserId().equals(cachingCredentialsProvider.getCachedIdentityId())) {

                                      map.put(conversationAdapter.KMsgUserName, "Me");
                                      map.put(conversationAdapter.KMsgType, "me");
                                      break;
                                  } else {

                                      map.put(conversationAdapter.KMsgUserName, userData.getName());
                                      map.put(conversationAdapter.KMsgType, "other");
                                      break;
                                  }

                              } else {
                                  if (con.getUserId().equals(cachingCredentialsProvider.getCachedIdentityId())) {
                                      map.put(conversationAdapter.KMsgUserName, "Me");
                                      map.put(conversationAdapter.KMsgType, "me");
                                      break;
                                  }
                              }

                          }
                        conversationAdapter.add(map);
                    }
                }

                conversationListView.setAdapter(conversationAdapter);
                progressBar.setVisibility(View.GONE);
                conversationAdapter.notifyDataSetChanged();
                scroll();
                }
        }.execute();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == Activity.RESULT_OK) {
            uploadWithData(data);
        }
    }

    public File savebitmap(Bitmap bmp, String name) throws IOException {

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        if(!unScaledBitmap){
            bmp.compress(Bitmap.CompressFormat.JPEG, 60, bytes);
        }

        File f = null;
        //make the dir of images
        if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)){
            f =new File(android.os.Environment.getExternalStorageDirectory(),"My Sample App/Images/");
        }
        if(isStoragePermissionGranted()){
            if(!f.exists()){
                f.mkdirs();
            }
            f = new File(Environment.getExternalStorageDirectory()
                    + File.separator +"My Sample App/Images/"+name);
            if(!f.exists()){
                f.createNewFile();
                FileOutputStream fo = new FileOutputStream(f);
                fo.write(bytes.toByteArray());
     fo.close();
            }
        }
        return f;
    }

    private void uploadWithData(Intent data) {

        final Uri uri = data.getData();
        //get file path from a Uri
        final String path = ImageSelectorUtils.getFilePathFromUri(ChatActivity.this, uri);
        File resizeFile = null;
        File file = new File(path);

        Bitmap bitmap = decodeSampledBitmapFromUri(path, 200, 150);
        try {
             resizeFile = savebitmap(bitmap, file.getName());
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(resizeFile.getName() != null){
            //get an instance of User File Manager which uploads files from Amazon S3
            userFileManager.uploadContent(resizeFile, resizeFile.getName(), new ContentProgressListener() {
                @Override
                public void onSuccess(final ContentItem contentItem) {

                    CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                            getApplicationContext(),
                            AWSConfiguration.AMAZON_COGNITO_IDENTITY_POOL_ID, // Identity Pool ID
                            AWSConfiguration.AMAZON_S3_USER_FILES_BUCKET_REGION  // Region
                    );

                    final AmazonS3 s3 =
                            new AmazonS3Client(credentialsProvider);
                    s3.setRegion(Region.getRegion(AWSConfiguration.AMAZON_S3_USER_FILES_BUCKET_REGION));

                    final String identityId = AWSMobileClient.defaultMobileClient()
                            .getIdentityManager()
                            .getCredentialsProvider()
                            .getCachedIdentityId();

                    //full path of upload image
                    final URL presignedUrl = s3.generatePresignedUrl(AWSConfiguration.AMAZON_S3_USER_FILES_BUCKET, S3_PREFIX_PRIVATE + identityId + "/" + contentItem.getFilePath(),
                            new Date(new Date().getTime() + 60 * 60 * 1000));

                    String[] urlParts = presignedUrl.toString().split("\\?");

                    uploadedImagePath = urlParts[0];

                    sendBtn.callOnClick();

                    Log.d("URL", uploadedImagePath);
                }

                @Override
                public void onProgressUpdate(final String fileName, final boolean isWaiting,
                                             final long bytesCurrent, final long bytesTotal) {
                    Log.d("URL", fileName);
                }

                @Override
                public void onError(final String fileName, final Exception ex) {

                    Log.e("CustomError", "---> " + ex.getLocalizedMessage());
                }
            });
        }
    }

    private void sendPush(){

        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String msg = "";
                try {

                    CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                            getApplicationContext(),
                            AWSConfiguration.AMAZON_COGNITO_IDENTITY_POOL_ID, // Identity Pool ID
                            AWSConfiguration.AMAZON_SNS_REGION  // Region
                    );

                    AmazonSNSClient snsClient = new AmazonSNSClient(credentialsProvider.getCredentials());
                    snsClient.setRegion(Region.getRegion(AWSConfiguration.AMAZON_SNS_REGION));
                    for (UserProfileDO userData : awsUsersData) {
                        String currentUserID = credentialsProvider.getCachedIdentityId();
                        String userID = userData.getUserId();
                        if(!userID.equals(currentUserID)){
                            Set<String> targetArn = userData.getPushTargetArn();
                            // create an iterator
                            Iterator targetArnIterator = targetArn.iterator();
                            for( ;targetArnIterator.hasNext();){
                                PublishRequest publishRequest = new PublishRequest();
                                publishRequest.setMessageStructure("json");
                                publishRequest.setTargetArn(targetArnIterator.next().toString());
                                String sender = identityManager.getCurrentIdentityProvider().getUserName();
                                String defaultMessage = String.format("\"default\": \"Sent By %s\",\n", sender);
                                String gcmMessage = String.format("\"GCM\":\"{\\\"data\\\":{\\\"message\\\":\\\"Message sent by %s\\\",\\\"chatRoomId\\\":\\\"%s\\\"}}\",\n",sender,chatRoomId);
                                String apnsMessage = String.format("\"APNS\":\"{\\\"aps\\\":{\\\"alert\\\":\\\"Message sent by %s\\\"},\\\"chatRoomId\\\":\\\"%s\\\"}\",\n",sender,chatRoomId);
                                String apnsSANDBOXMessage = String.format("\"APNS_SANDBOX\":\"{\\\"aps\\\":{\\\"alert\\\":\\\"Message sent by %s\\\"},\\\"chatRoomId\\\":\\\"%s\\\"}\"\n",sender,chatRoomId);
                                String message = String.format("{\n" + defaultMessage + gcmMessage + apnsMessage + apnsSANDBOXMessage + "}");
                                publishRequest.setMessage(message);
                                try{
                                    PublishResult publishResult = snsClient.publish(publishRequest);
                                    Log.i("AWS", publishResult.getMessageId());
                                } catch (AmazonServiceException ex) {
                                    msg = ex.getLocalizedMessage();
                                    Log.e("CustomError", "---> " + ex.getLocalizedMessage());
                                }
                            }
                        }
                    }
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

                loadChat();
            }
        }.execute();
    }


    public Bitmap decodeSampledBitmapFromUri(String path, int newWidth, int newHeight) {

        Bitmap scaledBitmap = null;
            // Part 1: Decode image
            Bitmap unscaledBitmap = ScalingUtilities.decodeFile(path, newWidth, newHeight, ScalingUtilities.ScalingLogic.FIT);

            if (!(unscaledBitmap.getWidth() <= newWidth && unscaledBitmap.getHeight() <= newHeight)) {
                // Part 2: Scale image
                scaledBitmap = ScalingUtilities.createScaledBitmap(unscaledBitmap, newWidth, newHeight, ScalingUtilities.ScalingLogic.FIT);
            } else {
                unscaledBitmap.recycle();
            }

            if (scaledBitmap != null) {
                unScaledBitmap = false;
                return scaledBitmap;
            } else {
                unScaledBitmap = true;
                return unscaledBitmap;
            }
    }
    public  boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {

                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults[0]== PackageManager.PERMISSION_GRANTED){
            //resume tasks needing this permission
        }
    }
}
