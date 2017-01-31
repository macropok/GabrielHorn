package com.devotify.gabrielhorn.activity;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Telephony;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.devotify.gabrielhorn.R;
import com.devotify.gabrielhorn.adapter.HomePageSwipeAdapter;
import com.devotify.gabrielhorn.fragments.AboutFragment;
import com.devotify.gabrielhorn.fragments.AddPostFragment;
import com.devotify.gabrielhorn.fragments.EditLocationFragment;
import com.devotify.gabrielhorn.fragments.FragmentMore;
import com.devotify.gabrielhorn.fragments.PostDetailsFragment;
import com.devotify.gabrielhorn.fragments.PostsFragment;
import com.devotify.gabrielhorn.fragments.PrivacyPolicyFragment;
import com.devotify.gabrielhorn.fragments.TabContainerFragment;
import com.devotify.gabrielhorn.fragments.TermsAndConditionsFragment;
import com.devotify.gabrielhorn.fragments.VisitSiteFragment;
import com.devotify.gabrielhorn.interfaces.ActivityResultListener;
import com.devotify.gabrielhorn.interfaces.LogInStateListener;
import com.devotify.gabrielhorn.interfaces.RegisterActivityResultListener;
import com.devotify.gabrielhorn.model.LocalUser;
import com.devotify.gabrielhorn.model.Post;
import com.devotify.gabrielhorn.utility.AsyncCallback;
import com.devotify.gabrielhorn.utility.FontUtils;
import com.devotify.gabrielhorn.utility.Fonts;
import com.facebook.FacebookException;
import com.facebook.FacebookOperationCanceledException;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.widget.FacebookDialog;
import com.facebook.widget.WebDialog;
import com.parse.ParseAnalytics;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseUser;
/*import com.sromku.simple.fb.SimpleFacebook;
import com.sromku.simple.fb.entities.Feed;
import com.sromku.simple.fb.listeners.OnPublishListener;*/

import java.util.ArrayList;
import java.util.Collection;

public class MainActivity extends ActionBarActivity implements PostsFragment.FragmentPostItemClickedListener,
        FragmentMore.MoreItemClickedListener, LogInStateListener, RegisterActivityResultListener, PostDetailsFragment.SpecificShareListener

{
    public static final long LOCATION_ALARM_DURATION = (long) (30 * 10 * 1000), SPLASH_SCREEN_DURATION = 5 * 1000;
    private TabContainerFragment mTabContainerFragment;
    private ArrayList<ActivityResultListener> mActivityResultListeners = new ArrayList<>();
    private long startSplashTime;

    //private SimpleFacebook mSimpleFacebook;

    private UiLifecycleHelper uiHelper;
    private boolean canShareDialog;
    String shareName;

    String shareDescription;

    String appShareUrl;
    ParseFile generalShareImage;

    public static boolean isPostDetailFrame = false;



    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.splash_screen);

        startSplashTime = System.currentTimeMillis();
        FontUtils.initialize(this, new String[]{Fonts.LIGHT});
        ParseAnalytics.trackAppOpened(getIntent());
        getAppCompanyInfo(new AsyncCallback<Boolean>()
        {
            @Override
            public void onOperationCompleted(Boolean result)
            {
                if (result)
                {
                    initLocationAlarm(MainActivity.this);

                    long dt = System.currentTimeMillis() - startSplashTime;
                    long timeToWait = SPLASH_SCREEN_DURATION - dt;
                    timeToWait = timeToWait > 0 ? timeToWait : 0;

                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            initUI();
                        }
                    }, timeToWait);
                }
                else
                {
                    Toast.makeText(MainActivity.this, "Error. Please check your network connection.", Toast.LENGTH_SHORT).show();
                }
            }
        });


        //FB Post
        uiHelper = new UiLifecycleHelper(this, null);
        uiHelper.onCreate(savedInstanceState);
    }


    public void initUI()
    {
        getSupportActionBar().show();
        setContentView(R.layout.activity_main);

        mTabContainerFragment = new TabContainerFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, mTabContainerFragment).commit();

        int titleId = getResources().getIdentifier("action_bar_title", "id", "android");
        TextView actionBarTitleTextView = (TextView) findViewById(titleId);
        FontUtils.getInstance().overrideFonts(actionBarTitleTextView, Fonts.LIGHT);
        isPostDetailFrame = false;
    }

    public static void initLocationAlarm(Context context)
    {
        Intent locationAlarmIntent = new Intent(context, BackgroundNotificationService.class);
        PendingIntent pendingLocationAlarmIntent = PendingIntent.getService(context, 0,
                locationAlarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), LOCATION_ALARM_DURATION, pendingLocationAlarmIntent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.main, menu);
        updateAddPostButton(menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        updateAddPostButton(menu);
        return super.onPrepareOptionsMenu(menu);
    }

    public void updateAddPostButton(Menu menu)
    {
        MenuItem addPostItem = menu.findItem(R.id.action_add_post);
        if (addPostItem != null)
        {
            ParseUser currentUser = ParseUser.getCurrentUser();
            addPostItem.setVisible(currentUser != null && currentUser.getBoolean("isAdmin"));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.action_share:

                onShareAppMenuClicked(null,null, null);
                break;
            case R.id.action_add_post:
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, AddPostFragment.newInstance()).
                        addToBackStack(null).commit();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void getAppCompanyInfo(final AsyncCallback<Boolean> onCompanyInitialized)
    {
        LocalUser.initialize(this, new AsyncCallback<Boolean>()
        {
            @Override
            public void onOperationCompleted(Boolean result)
            {
                onCompanyInitialized.onOperationCompleted(result);
            }
        });
    }

    @Override
    public void onPostClicked(Post post)
    {
        isPostDetailFrame = true;
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, PostDetailsFragment.newInstance(post)).addToBackStack(null)
                .commit();
    }

    @Override
    public void onVisitWebMenuClicked()
    {
        VisitSiteFragment visitSiteFragment = VisitSiteFragment.newInstance();
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, visitSiteFragment).addToBackStack(null)
                .commit();
    }

    @Override
    public void onSpecificShare(String title,String message, String imageUrl)
    {
        onShareAppMenuClicked(title, message, imageUrl);
    }

    private String shareContent, imageUrl, shareUrl,shareTitle;

    @Override
    public void onShareAppMenuClicked(String title, String content, final String image)
    {

        if(isPostDetailFrame && (content == null))
            return;

        if(title == null)
        {
            shareTitle = "Sharing" + getString(R.string.app_name) +" App";
        }
        else
        {
            shareTitle =  title;
        }

        if (content == null)
        {
            shareContent = "I would like to share the" + getString(R.string.app_name) +
                    " App with you. Stay up to date with exclusive releases, events, and rewards";
        }
        else
        {
            shareContent = content;
        }

        if (image == null)
        {
            ParseObject parentObject = LocalUser.getInstance().getParentCompany();
            shareUrl = parentObject.getString("appShareUrl");
            imageUrl = parentObject.getParseFile("generalShareImageUrl") != null ? parentObject.getParseFile("generalShareImageUrl").getUrl() : null;
        }
        else
        {
            imageUrl = image;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Share");
        builder.setItems(new String[]{
                "Facebook", "Email", "Text"
        }, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialogInterface, int clickedPosition)
            {
                switch (clickedPosition)
                {
                    case 0:
                        shareToFacebook(shareTitle,shareContent, shareUrl, imageUrl);
                        break;
                    case 1:
                        shareToEmail(shareTitle,shareContent, shareUrl, imageUrl);
                        break;
                    case 2:
                        shareToSms(shareTitle,shareContent, shareUrl, imageUrl);
                        break;
                }
            }
        });

        builder.show();
    }

    public void shareToFacebook(final String shareTitle, final String shareText, final String shareUrl, final String imageUrl)
    {

        //Old codes
        /*final Feed feed = new Feed.Builder().setMessage(getString(R.string.app_name)).setName(getString(R.string.app_name))
                .setDescription(shareText).setLink(shareUrl).build();

        SimpleFacebook.getInstance().publish(feed, true, new OnPublishListener()
        {
            @Override
            public void onException(Throwable throwable)
            {
                throwable.printStackTrace();
            }

            @Override
            public void onFail(String reason)
            {
                Log.e("GabrielHorn", reason);
            }

            @Override
            public void onThinking()
            {
                Log.e("GabrielHorn", "Thinking");
            }

            @Override
            public void onComplete(String response)
            {
                Log.e("GabrielHorn", response);
            }
        });*/


        /*if (FacebookDialog.canPresentShareDialog(getApplicationContext(),
                FacebookDialog.ShareDialogFeature.SHARE_DIALOG)) {
            // Publish the post using the Share Dialog
            FacebookDialog shareDialog = new FacebookDialog.ShareDialogBuilder(this)
                    .setLink(shareUrl)
                    .setDescription(shareText)
                    .setPicture(imageUrl)
                    .build();
            uiHelper.trackPendingDialogCall(shareDialog.present());
            canShareDialog = true;
        } else {
            // Fallback. For example, publish the post using the Feed Dialog

            canShareDialog = false;
            Session.openActiveSession(this, true,this);
        }*/



        Session.openActiveSession(this, true,new Session.StatusCallback() {
            @Override
            public void call(Session session, SessionState sessionState, Exception e) {

                if (session.isOpened()) {

                    publishFeedDialog(shareTitle,shareText,shareUrl,imageUrl);

                }
            }
        });
       
    }

    private void publishFeedDialog(String name,String shareContent,String shareUrl,String imageUrl) {
        Bundle params = new Bundle();
        params.putString("name",name);
        //params.putString("caption", "Build great social apps and get more installs.");
        params.putString("description", shareContent);
        params.putString("link", shareUrl);
        params.putString("picture", imageUrl);

        WebDialog feedDialog = (
                new WebDialog.FeedDialogBuilder(this,
                        Session.getActiveSession(),
                        params))
                .setOnCompleteListener(new WebDialog.OnCompleteListener() {

                    @Override
                    public void onComplete(Bundle values,
                                           FacebookException error) {
                        if (error == null) {
                            // When the story is posted, echo the success
                            // and the post Id.
                            final String postId = values.getString("post_id");
                            if (postId != null) {
                                Toast.makeText(MainActivity.this,
                                        "Posted story, id: "+postId,
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                // User clicked the Cancel button
                                Toast.makeText(getApplicationContext(),
                                        "Publish cancelled",
                                        Toast.LENGTH_SHORT).show();
                            }
                        } else if (error instanceof FacebookOperationCanceledException) {
                            // User clicked the "x" button
                            Toast.makeText(getApplicationContext(),
                                    "Publish cancelled",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            // Generic, ex: network error
                            Toast.makeText(getApplicationContext(),
                                    "Error posting story",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                })
                .build();
        feedDialog.show();
    }



    public void shareToEmail(final String shareTitle, String shareText, String shareUrl, String imageUrl)
    {
        Intent emailIntent = new Intent(Intent.ACTION_SEND, Uri.fromParts("mailto", "", null));

        String htmlContent = "<h1>" + shareText + "</h1>" +
                "<a href=\"" + shareUrl + "\"> App Link </a>" +
                "<img src=\"" + imageUrl + "\" />";

        emailIntent.setType("image/png");
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "From the " + getString(R.string.app_name) + " app");
        emailIntent.putExtra(Intent.EXTRA_TEXT, shareTitle + "\n\n" + shareText + "\n\n"
                + shareUrl + "\n\n" + imageUrl);
        emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(imageUrl));
        startActivity(Intent.createChooser(emailIntent, "Email"));
    }

    public void shareToSms(final String shareTitle, String shareText, String shareUrl, String imageUrl)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) //At least KitKat
        {
            String defaultSmsPackageName = Telephony.Sms.getDefaultSmsPackage(this); //Need to change the build to API 19

            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.setType("text/plain");
            sendIntent.putExtra(Intent.EXTRA_TEXT, shareText + "\n\n" + shareUrl + "\n\n" + imageUrl);

            if (defaultSmsPackageName != null)//Can be null in case that there is no default, then the user would be able to choose any app that support this intent.
            {
                sendIntent.setPackage(defaultSmsPackageName);
            }

            startActivity(sendIntent);
        }
        else
        {
            Intent sendIntent = new Intent(Intent.ACTION_VIEW);
            sendIntent.setData(Uri.parse("sms:"));
            sendIntent.putExtra("sms_body", shareTitle + "\n\n" + shareText + "\n\n" + shareUrl + "\n\n" + imageUrl);
            startActivity(sendIntent);
        }
    }

    @Override
    public void onTermsConditionMenuClicked()
    {
        TermsAndConditionsFragment termsAndConditionsFragment = new TermsAndConditionsFragment();
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, termsAndConditionsFragment).addToBackStack(null).commit();
    }

    @Override
    public void onPrivacyPolicyMenuClicked()
    {
        PrivacyPolicyFragment policyFragment = PrivacyPolicyFragment.newInstance();
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, policyFragment).addToBackStack(null).commit();
    }

    @Override
    public void onRewardsClicked()
    {
        mTabContainerFragment.getHomePager().setCurrentItem(HomePageSwipeAdapter.POS_REWARDS);
    }

    @Override
    public void onEditStoreLocationClicked()
    {
        EditLocationFragment editLocationFragment = EditLocationFragment.newInstance();
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, editLocationFragment).addToBackStack(null).commit();
    }

    @Override
    public void onAboutAppMenuClicked()
    {
        AboutFragment aboutFragment = AboutFragment.newInstance();
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, aboutFragment).addToBackStack(null).commit();
    }

    @Override
    public void onCallUsMenuClicked()
    {
        String phn = LocalUser.getInstance().getParentCompany().getString("phoneNumber");
        if (!phn.equals(""))
        {
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + phn));
            startActivity(callIntent);
        }
    }

    @Override
    public void onEmailUsMenuClicked()
    {
        String email = LocalUser.getInstance().getParentCompany().getString("email");
        if (!email.equals(""))
        {
            Intent intentMail = new Intent(Intent.ACTION_SEND);
            intentMail.putExtra(Intent.EXTRA_EMAIL, new String[]{email});

            intentMail.setType("message/rfc822");
            startActivity(Intent.createChooser(intentMail, "Choose an Email client :"));
        }
    }

    @Override
    public void onLogInToggled(boolean requestLogin)
    {
        supportInvalidateOptionsMenu();
        mTabContainerFragment.getHomePageSwipeAdapter().notifyDataSetChanged();

        if (requestLogin)
            mTabContainerFragment.getHomePager().setCurrentItem(HomePageSwipeAdapter.POS_REWARDS);
    }

    @Override
    public void registerActivityResultListener(ActivityResultListener listener)
    {
        mActivityResultListeners.add(listener);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        /*mSimpleFacebook.onActivityResult(this, requestCode, resultCode, data);
        for (ActivityResultListener listener : mActivityResultListeners)
        {
            listener.onActivityResult(requestCode, resultCode, data);
        }
        super.onActivityResult(requestCode, resultCode, data);*/


        //FB Integration for URI helper

        Session.getActiveSession().onActivityResult(this, requestCode,resultCode, data);

        uiHelper.onActivityResult(requestCode, resultCode, data, new FacebookDialog.Callback() {
            @Override
            public void onError(FacebookDialog.PendingCall pendingCall, Exception error, Bundle data) {
                Log.e("Activity", String.format("Error: %s", error.toString()));
            }

            @Override
            public void onComplete(FacebookDialog.PendingCall pendingCall, Bundle data) {
                Log.i("Activity", "Success!");

            }
        });

    }


    //FB integration for URI helper
    //Start


    @Override
    protected void onResume() {
        super.onResume();
        uiHelper.onResume();
        //mSimpleFacebook = SimpleFacebook.getInstance(this);
    }



    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        uiHelper.onSaveInstanceState(outState);
    }

    @Override
    public void onPause() {
        super.onPause();
        uiHelper.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        uiHelper.onDestroy();
    }



    private void cacheFacebookSession() {
        // get session if cached session exist.
        Log.i("SignInManager","Entering cacheFacebookSession");
        Session facebookSession = Session
                .openActiveSessionFromCache(this);

        Log.i("SignInManager","Exiting cacheFacebookSession");
    }

    //End


    private boolean isSubsetOf(Collection<String> subset, Collection<String> superset)
    {
        for (String string : subset)
        {
            if (!superset.contains(string))
            {
                return false;
            }
        }
        return true;
    }


}