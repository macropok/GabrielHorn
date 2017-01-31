package com.devotify.gabrielhorn.activity;

import android.app.Application;

import com.devotify.gabrielhorn.R;
import com.devotify.gabrielhorn.model.Post;
import com.devotify.gabrielhorn.model.RetailLocation;
import com.devotify.gabrielhorn.model.Reward;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParsePush;
import com.parse.SaveCallback;
/*import com.sromku.simple.fb.Permission;
import com.sromku.simple.fb.SimpleFacebook;
import com.sromku.simple.fb.SimpleFacebookConfiguration;*/

public class GabrielHornApplication extends Application
{
    public static final String CHANNEL_NAME = "PushGabrielHorn";
/*    private Permission[] permissions = new Permission[]{
            Permission.PUBLISH_ACTION
    };*/

    @Override
    public void onCreate()
    {
        super.onCreate();

        ParseObject.registerSubclass(Post.class);
        ParseObject.registerSubclass(Reward.class);
        ParseObject.registerSubclass(RetailLocation.class);

        Parse.enableLocalDatastore(getApplicationContext());
        Parse.initialize(this, "b0iV7zeWFXFN0BcAd5gv3OjAYjWQXtbI5rsdJmU3", "UKSHdFci5UMlEmawuAfSPVEbBKGfVsy35B4K34C8");
        ParsePush.subscribeInBackground(CHANNEL_NAME, new SaveCallback()
        {
            @Override
            public void done(ParseException e)
            {
                if (e != null)
                {
                    e.printStackTrace();
                }
            }
        });

        /*SimpleFacebookConfiguration configuration = new SimpleFacebookConfiguration.Builder()
                .setAppId(getString(R.string.fb_app_id))
                .setAppSecret("b48db84a4ecf15595f479bd965ef6024")
                .setAskForAllPermissionsAtOnce(true)
                .setNamespace("gabrielhorn")
                .setPermissions(permissions)
                .build();

        SimpleFacebook.setConfiguration(configuration);
*/

    }
}
