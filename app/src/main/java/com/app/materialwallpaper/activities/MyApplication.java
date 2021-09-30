package com.app.materialwallpaper.activities;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.multidex.MultiDex;

import com.facebook.ads.AudienceNetworkAds;
import com.onesignal.OneSignal;
import com.app.materialwallpaper.utils.AdsPref;

import static com.app.materialwallpaper.utils.Constant.AD_STATUS_ON;
import static com.app.materialwallpaper.utils.Constant.FAN;

public class MyApplication extends Application {

    private static MyApplication mInstance;
    public SharedPreferences preferences;
    Activity activity;
    public String prefName = "news";
    AdsPref adsPref;

    public MyApplication() {
        mInstance = this;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        adsPref = new AdsPref(this);
        if (adsPref.getAdStatus().equals(AD_STATUS_ON) && adsPref.getAdType().equals(FAN)) {
            AudienceNetworkAds.initialize(this);
        }
        mInstance = this;

        // OneSignal Initialization
        OneSignal.startInit(this)
                .inFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification)
                .unsubscribeWhenNotificationsAreDisabled(true)
                .init();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    public static synchronized MyApplication getInstance() {
        return mInstance;
    }

}
