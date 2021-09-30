package com.app.materialwallpaper.utils;

import android.content.Context;
import android.util.Log;

import com.facebook.ads.AdSettings;
import com.facebook.ads.AudienceNetworkAds;
import com.app.materialwallpaper.BuildConfig;

/**
 * Sample class that shows how to call initialize() method of Audience Network SDK.
 */
public class AudienceNetworkInitializeHelper implements AudienceNetworkAds.InitListener {

    private final Context mContext;

    private AudienceNetworkInitializeHelper(Context context) {
        mContext = context;
    }

    /**
     * It's recommended to call this method from Application.onCreate(). Otherwise you can call it
     * from all Activity.onCreate() methods for Activities that contain ads.
     *
     * @param context Application or Activity.
     */
    public static void initialize(Context context) {
        if (!AudienceNetworkAds.isInitialized(context)) {
            if (BuildConfig.DEBUG) {
                AdSettings.turnOnSDKDebugger(context);
            }

            AudienceNetworkAds.buildInitSettings(context)
                    .withInitListener(new AudienceNetworkInitializeHelper(context))
                    .initialize();
        }
    }

    @Override
    public void onInitialized(AudienceNetworkAds.InitResult result) {
        Log.d(AudienceNetworkAds.TAG, result.getMessage());
    }
}