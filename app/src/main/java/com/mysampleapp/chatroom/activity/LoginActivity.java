package com.mysampleapp.chatroom.activity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobile.AWSConfiguration;
import com.amazonaws.mobile.AWSMobileClient;
import com.amazonaws.mobile.push.PushManager;
import com.amazonaws.mobile.user.IdentityManager;
import com.amazonaws.mobile.user.IdentityProvider;
import com.amazonaws.mobile.user.signin.FacebookSignInProvider;
import com.amazonaws.mobile.user.signin.GoogleSignInProvider;
import com.amazonaws.mobile.user.signin.SignInManager;
import com.amazonaws.mobile.user.signin.SignInProvider;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.mysampleapp.R;
import com.mysampleapp.chatroom.ChatUserProfileManager;
import com.mysampleapp.chatroom.adapter.ConversationAdapter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LoginActivity extends Activity {

    private final static String LOG_TAG = LoginActivity.class.getSimpleName();
    /** Permission Request Code (Must be < 256). */
    private static final int GET_ACCOUNTS_PERMISSION_REQUEST_CODE = 93;

    public static final String MyPREFERENCES = "MyPrefs" ;
    private ConversationAdapter conversationAdapter;

    /** The Google OnClick listener, since we must override it to get permissions on Marshmallow and above. */
    private View.OnClickListener googleOnClickListener;

    /** The identity manager used to keep track of the current user account. */
    private IdentityManager identityManager;
    private SignInManager signInManager;
    private PushManager pushManager;
    private CognitoCachingCredentialsProvider cachingCredentialsProvider;

    private QueryResult isUser;

    /**
     * SignInResultsHandler handles the final result from sign in. Making it static is a best
     * practice since it may outlive the SplashActivity's life span.
     */
    private class SignInResultsHandler implements IdentityManager.SignInResultsHandler {
        /**
         * Receives the successful sign-in result and starts the main activity.
         * @param provider the identity provider used for sign-in.
         */
        @Override
        public void onSuccess(final IdentityProvider provider) {
            Log.d(LOG_TAG, String.format("User sign-in with %s succeeded",
                    provider.getDisplayName()));


            // Obtain a reference to the CognitoCachingCredentialsProvider.
            cachingCredentialsProvider = new CognitoCachingCredentialsProvider(
                    LoginActivity.this,
                    AWSConfiguration.AMAZON_COGNITO_IDENTITY_POOL_ID, // Identity Pool ID
                    AWSConfiguration.AMAZON_DYNAMODB_REGION   // Region
            );


            // Its used to retrieve data
            SharedPreferences preferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
            String name = preferences.getString("userID", "");

            if(name.equals("")){

                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("userID", cachingCredentialsProvider.getCachedIdentityId());
                editor.commit();

            }else if( !name.equals( cachingCredentialsProvider.getCachedIdentityId())){

                conversationAdapter.imageLoader.clearCache();
                conversationAdapter.notifyDataSetChanged();
            }


            Toast.makeText(LoginActivity.this, String.format("Sign-in with %s succeeded.",
                    provider.getDisplayName()), Toast.LENGTH_LONG).show();

            // Load user name and image.
            AWSMobileClient.defaultMobileClient()
                    .getIdentityManager().loadUserInfoAndImage(provider, new Runnable() {
                @Override
                public void run() {
                    //check user exist or not in dynamoDB
                    final ChatUserProfileManager userProfileManager = new ChatUserProfileManager();

                    new AsyncTask<Void, Void, String>() {
                        @Override
                        protected String doInBackground(Void... params) {
                            String msg = "";
                            try {
                                // register device first to ensure we have a push endpoint.
                                pushManager.registerDevice();
                                isUser = userProfileManager.isUserExist(cachingCredentialsProvider);

                                Log.i("AWS", msg);

                            } catch (AmazonServiceException ex) {
                                msg = ex.getLocalizedMessage();
                                Log.e("CustomError", "---> " + ex.getLocalizedMessage());
                            }
                            return msg;
                        }

                        @Override
                        protected void onPostExecute(String msg) {
                            if (isUser != null) {
                                if (isUser.getItems().size() == 0) {

                                    //get phone number
                                    LayoutInflater li = LayoutInflater.from(LoginActivity.this);
                                    View promptsView = li.inflate(R.layout.prompt, null);
                                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(LoginActivity.this);
                                    alertDialogBuilder.setView(promptsView);
                                    final EditText userInput = (EditText) promptsView.findViewById(R.id.editTextDialogUserInput);
                                    // set dialog message
                                    alertDialogBuilder
                                            .setCancelable(false)
                                            .setPositiveButton(R.string.ok,
                                                    new DialogInterface.OnClickListener() {
                                                        public void onClick(final DialogInterface dialog, int id) {

                                                            new AsyncTask<Void, Void, String>() {
                                                                @Override
                                                                protected String doInBackground(Void... params) {
                                                                    String msg = "";
                                                                    try {
                                                                        // create hash set
                                                                        Set<String> endpointSet = new HashSet<String>();
                                                                        endpointSet.add(pushManager.getEndpointArn());
                                                                        String number = userInput.getText().toString();
                                                                        number = number.replaceAll("[^0-9\\+]", "");
                                                                        String ph = number;
                                                                        //add user profile information in UserProfile table
                                                                        userProfileManager.addUserProfile(endpointSet, ph, identityManager, cachingCredentialsProvider);

                                                                    } catch (AmazonServiceException ex) {
                                                                        msg = ex.getLocalizedMessage();
                                                                        Log.e("CustomError", "---> " + ex.getLocalizedMessage());
                                                                    }
                                                                    return msg;
                                                                }

                                                                @Override
                                                                protected void onPostExecute(String msg) {
                                                                    startActivity(new Intent(LoginActivity.this, DashBoardActivity.class)
                                                                            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                                                                    finish();
                                                                    dialog.cancel();
                                                                }
                                                            }.execute();
                                                        }
                                                    })
                                            .setNegativeButton(R.string.cancel,
                                                    new DialogInterface.OnClickListener() {
                                                        public void onClick(DialogInterface dialog, int id) {
                                                            dialog.cancel();
                                                        }
                                                    });

                                    // create alert dialog
                                    AlertDialog alertDialog = alertDialogBuilder.create();
                                    // show it
                                    alertDialog.show();
                                } else {

                                    final String phNo = isUser.getItems().get(0).get("phone").getS();
                                    List<String> endpointArn = isUser.getItems().get(0).get("pushTargetArn").getSS();
                                    final Set<String> endpointSet = new HashSet<String>();
                                    endpointSet.add(pushManager.getEndpointArn());
                                    for (int i = 0; i < endpointArn.size(); i++) {
                                        endpointSet.add(endpointArn.get(i));
                                    }

                                    new AsyncTask<Void, Void, String>() {
                                        @Override
                                        protected String doInBackground(Void... params) {
                                            String msg = "";
                                            try {
                                                //add user profile information in UserProfile table
                                                userProfileManager.addUserProfile(endpointSet, phNo, identityManager, cachingCredentialsProvider);

                                            } catch (AmazonServiceException ex) {
                                                msg = ex.getLocalizedMessage();
                                                Log.e("CustomError", "---> " + ex.getLocalizedMessage());
                                            }
                                            return msg;
                                        }

                                        @Override
                                        protected void onPostExecute(String msg) {
                                            Log.d(LOG_TAG, "Launching DashBoard Activity...");
                                            startActivity(new Intent(LoginActivity.this, DashBoardActivity.class)
                                                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                                        }
                                    }.execute();
                                }
                            }
                        }
                    }.execute();
                }
            });

            // The sign-in manager is no longer needed once signed in.
            SignInManager.dispose();
        }

        /**
         * Recieves the sign-in result indicating the user canceled and shows a toast.
         * @param provider the identity provider with which the user attempted sign-in.
         */
        @Override
        public void onCancel(final IdentityProvider provider) {
            Log.d(LOG_TAG, String.format("User sign-in with %s canceled.",
                    provider.getDisplayName()));

            Toast.makeText(LoginActivity.this, String.format("Sign-in with %s canceled.",
                    provider.getDisplayName()), Toast.LENGTH_LONG).show();
        }

        /**
         * Receives the sign-in result that an error occurred signing in and shows a toast.
         * @param provider the identity provider with which the user attempted sign-in.
         * @param ex the exception that occurred.
         */
        @Override
        public void onError(final IdentityProvider provider, final Exception ex) {
            Log.e(LOG_TAG, String.format("User Sign-in failed for %s : %s",
                    provider.getDisplayName(), ex.getMessage()), ex);

            final AlertDialog.Builder errorDialogBuilder = new AlertDialog.Builder(LoginActivity.this);
            errorDialogBuilder.setTitle("Sign-In Error");
            errorDialogBuilder.setMessage(
                    String.format("Sign-in with %s failed.\n%s", provider.getDisplayName(), ex.getMessage()));
            errorDialogBuilder.setNeutralButton("Ok", null);
            errorDialogBuilder.show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        signInManager = SignInManager.getInstance(this);

        signInManager.setResultsHandler(this, new SignInResultsHandler());

        // Initialize sign-in buttons.
        signInManager.initializeSignInButton(FacebookSignInProvider.class,
                this.findViewById(R.id.fb_login_button));

        googleOnClickListener =
                signInManager.initializeSignInButton(GoogleSignInProvider.class, findViewById(R.id.g_login_button));

        if (googleOnClickListener != null) {
            // if the onClick listener was null, initializeSignInButton will have removed the view.
            this.findViewById(R.id.g_login_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View view) {
                    final Activity thisActivity = LoginActivity.this;
                    if (ContextCompat.checkSelfPermission(thisActivity,
                            Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(LoginActivity.this,
                                new String[]{Manifest.permission.GET_ACCOUNTS},
                                GET_ACCOUNTS_PERMISSION_REQUEST_CODE);
                        return;
                    }

                    // call the Google onClick listener.
                    googleOnClickListener.onClick(view);
                }
            });
        }

        //call identityManager
        identityManager = AWSMobileClient.defaultMobileClient().getIdentityManager();
        //call pushManager for getting TargetArn
        pushManager = AWSMobileClient.defaultMobileClient().getPushManager();

        conversationAdapter = new ConversationAdapter(this);

    }


    @Override
    public void onRequestPermissionsResult(final int requestCode,
                                           final String permissions[], final int[] grantResults) {
        if (requestCode == GET_ACCOUNTS_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                this.findViewById(R.id.g_login_button).callOnClick();
            } else {
                Log.i(LOG_TAG, "Permissions not granted for Google sign-in. :(");
            }
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        signInManager.handleActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // pause/resume Mobile Analytics collection
        AWSMobileClient.defaultMobileClient().handleOnResume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        // pause/resume Mobile Analytics collection
        AWSMobileClient.defaultMobileClient().handleOnPause();
    }
}
