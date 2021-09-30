package com.app.materialwallpaper.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.security.ProviderInstaller;
import com.app.materialwallpaper.Config;
import com.app.materialwallpaper.R;
import com.app.materialwallpaper.callbacks.CallbackAds;
import com.app.materialwallpaper.models.AdStatus;
import com.app.materialwallpaper.models.Ads;
import com.app.materialwallpaper.rests.RestAdapter;
import com.app.materialwallpaper.utils.AdsPref;
import com.app.materialwallpaper.utils.SharedPref;
import com.app.materialwallpaper.utils.Tools;

import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivitySplash extends AppCompatActivity {

    Boolean isCancelled = false;
    private ProgressBar progressBar;
    long nid = 0;
    String url = "";
    ImageView img_splash;
    Call<CallbackAds> callbackCall = null;
    AdsPref adsPref;
    Ads ads;
    AdStatus ad_status;
    SharedPref sharedPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tools.transparentStatusBarNavigation(ActivitySplash.this);
        initializeSSLContext(ActivitySplash.this);
        setContentView(R.layout.activity_splash);
        Tools.getRtlDirection(this);
        sharedPref = new SharedPref(this);
        adsPref = new AdsPref(this);

        img_splash = findViewById(R.id.img_splash);
        if (sharedPref.getIsDarkTheme()) {
            img_splash.setImageResource(R.drawable.splash_dark);
        } else {
            img_splash.setImageResource(R.drawable.splash_default);
        }

        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);

        if (getIntent().hasExtra("nid")) {
            nid = getIntent().getLongExtra("nid", 0);
            url = getIntent().getStringExtra("external_link");
        }

        if (Tools.isConnect(this)) {
            requestAds();
        } else {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
                finish();
            }, Config.SPLASH_TIME + 1000);
        }

    }

    private void requestAds() {
        this.callbackCall = RestAdapter.createAPI().getAds();
        this.callbackCall.enqueue(new Callback<CallbackAds>() {
            public void onResponse(Call<CallbackAds> call, Response<CallbackAds> response) {
                CallbackAds resp = response.body();
                if (resp != null && resp.status.equals("ok")) {
                    ads = resp.ads;
                    ad_status = resp.ads_status;
                    if (ads.last_update_ads.equals(adsPref.getLastUpdateAds())) {
                        Log.d("AD_NETWORK", "Ads Data has been updated");
                    } else {
                        adsPref.saveAds(ads.ad_status, ads.ad_type, ads.admob_publisher_id, ads.admob_app_id, ads.admob_banner_unit_id, ads.admob_interstitial_unit_id, ads.admob_native_unit_id, ads.fan_banner_unit_id, ads.fan_interstitial_unit_id, ads.fan_native_unit_id, ads.startapp_app_id, ads.unity_game_id, ads.unity_banner_placement_id, ads.unity_interstitial_placement_id, ads.interstitial_ad_interval, ads.native_ad_interval, ads.native_ad_index, ads.last_update_ads);
                        Log.d("AD_NETWORK", "Ads Data saved");
                    }

                    if (ad_status.last_update_ads_status.equals(adsPref.getLastUpdateAdStatus())) {
                        Log.d("AD_NETWORK_STATUS", "Ads Status has been updated");
                    } else {
                        adsPref.saveAdStatus(ad_status.banner_ad_on_home_page, ad_status.banner_ad_on_search_page, ad_status.banner_ad_on_wallpaper_detail, ad_status.banner_ad_on_wallpaper_by_category, ad_status.interstitial_ad_on_click_wallpaper, ad_status.interstitial_ad_on_wallpaper_detail, ad_status.native_ad_on_wallpaper_list, ad_status.native_ad_on_exit_dialog, ad_status.last_update_ads_status);
                        Log.d("AD_NETWORK_STATUS", "Ads Status saved");
                    }

                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        startActivity(intent);
                        finish();
                    }, Config.SPLASH_TIME);
                } else {
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        startActivity(intent);
                        finish();
                    }, Config.SPLASH_TIME + 1000);
                }
            }

            public void onFailure(Call<CallbackAds> call, Throwable th) {
                Log.e("onFailure", "" + th.getMessage());
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(intent);
                    finish();
                }, Config.SPLASH_TIME);
            }
        });
    }

    /**
     * Initialize SSL
     * @param mContext
     */
    public static void initializeSSLContext(Context mContext){
        try {
            SSLContext.getInstance("TLSv1.2");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        try {
            ProviderInstaller.installIfNeeded(mContext.getApplicationContext());
        } catch (GooglePlayServicesRepairableException e) {
            e.printStackTrace();
        } catch (GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
        }
    }

}
