package com.app.materialwallpaper.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.duolingo.open.rtlviewpager.RtlViewPager;
import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.AdSize;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomnavigation.LabelVisibilityMode;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.ump.ConsentDebugSettings;
import com.google.android.ump.ConsentForm;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;
import com.app.materialwallpaper.BuildConfig;
import com.app.materialwallpaper.Config;
import com.app.materialwallpaper.R;
import com.app.materialwallpaper.fragments.FragmentCategory;
import com.app.materialwallpaper.fragments.FragmentFavorite;
import com.app.materialwallpaper.fragments.FragmentTabLayout;
import com.app.materialwallpaper.notification.NotificationUtils;
import com.app.materialwallpaper.utils.AdsPref;
import com.app.materialwallpaper.utils.AppBarLayoutBehavior;
import com.app.materialwallpaper.utils.AudienceNetworkInitializeHelper;
import com.app.materialwallpaper.utils.GDPR;
import com.app.materialwallpaper.utils.SharedPref;
import com.app.materialwallpaper.utils.Tools;
import com.startapp.sdk.ads.banner.Banner;
import com.startapp.sdk.ads.banner.BannerListener;
import com.startapp.sdk.adsbase.StartAppAd;
import com.startapp.sdk.adsbase.StartAppSDK;
import com.unity3d.ads.IUnityAdsInitializationListener;
import com.unity3d.ads.IUnityAdsListener;
import com.unity3d.ads.UnityAds;
import com.unity3d.services.banners.BannerErrorInfo;
import com.unity3d.services.banners.BannerView;
import com.unity3d.services.banners.UnityBannerSize;

import static com.app.materialwallpaper.Config.USE_LEGACY_GDPR_EU_CONSENT;
import static com.app.materialwallpaper.utils.Constant.ADMOB;
import static com.app.materialwallpaper.utils.Constant.AD_STATUS_ON;
import static com.app.materialwallpaper.utils.Constant.FAN;
import static com.app.materialwallpaper.utils.Constant.STARTAPP;
import static com.app.materialwallpaper.utils.Constant.UNITY;
import static com.app.materialwallpaper.utils.Constant.UNITY_ADS_BANNER_HEIGHT;
import static com.app.materialwallpaper.utils.Constant.UNITY_ADS_BANNER_WIDTH;

public class MainActivity extends AppCompatActivity {

    AppBarLayout appBarLayout;
    private ViewPager viewPager;
    private RtlViewPager viewPagerRTL;
    private long exitTime = 0;
    private CoordinatorLayout coordinatorLayout;
    MenuItem prevMenuItem;
    int pager_number = 3;
    private BottomNavigationView navigation;
    private FrameLayout adContainerView;
    private AdView adView;
    com.facebook.ads.AdView fanAdView;
    AdsPref adsPref;
    BannerView bottomBanner;
    RelativeLayout bottomBannerView;
    boolean isDebugMode = BuildConfig.DEBUG;
    private ConsentInformation consentInformation;
    ConsentForm consentForm;
    Toolbar toolbar;
    SharedPref sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tools.getTheme(this);
        sharedPref = new SharedPref(this);
        if (sharedPref.getIsDarkTheme()) {
            Tools.darkNavigation(this);
        } else {
            Tools.lightNavigation(this);
        }
        adsPref = new AdsPref(this);
        if (adsPref.getAdStatus().equals(AD_STATUS_ON) && adsPref.getAdType().equals(FAN)) {
            AudienceNetworkInitializeHelper.initialize(this);
        }
        if (adsPref.getAdStatus().equals(AD_STATUS_ON) && adsPref.getAdType().equals(STARTAPP)) {
            StartAppSDK.init(MainActivity.this, adsPref.getStartappAppId(), false);
            //StartAppSDK.setTestAdsEnabled(BuildConfig.DEBUG);
        }
        if (Config.ENABLE_RTL_MODE) {
            setContentView(R.layout.activity_main_rtl);
        } else {
            setContentView(R.layout.activity_main);
        }
        Tools.getRtlDirection(this);
        coordinatorLayout = findViewById(R.id.coordinatorLayout);
        appBarLayout = findViewById(R.id.appbarLayout);
        ((CoordinatorLayout.LayoutParams) appBarLayout.getLayoutParams()).setBehavior(new AppBarLayoutBehavior());

        if (adsPref.getAdStatus().equals(AD_STATUS_ON) && adsPref.getAdType().equals(ADMOB)) {
            MobileAds.initialize(this, initializationStatus -> {
            });
            if (USE_LEGACY_GDPR_EU_CONSENT) {
                GDPR.updateConsentStatus(this);
            } else {
                updateConsentStatus();
            }
        }

        if (adsPref.getAdStatus().equals(AD_STATUS_ON) && adsPref.getAdType().equals(STARTAPP)) {
            StartAppSDK.setUserConsent(this, "pas", System.currentTimeMillis(), true);
            StartAppAd.disableSplash();
        }

        if (adsPref.getAdStatus().equals(AD_STATUS_ON) && adsPref.getAdType().equals(UNITY)) {
            UnityAds.addListener(new IUnityAdsListener() {
                @Override
                public void onUnityAdsReady(String placementId) {
                    Log.d("Unity_interstitial", placementId);
                }

                @Override
                public void onUnityAdsStart(String placementId) {

                }

                @Override
                public void onUnityAdsFinish(String placementId, UnityAds.FinishState finishState) {

                }

                @Override
                public void onUnityAdsError(UnityAds.UnityAdsError unityAdsError, String message) {

                }
            });
            UnityAds.initialize(getApplicationContext(), adsPref.getUnityGameId(), isDebugMode, new IUnityAdsInitializationListener() {
                @Override
                public void onInitializationComplete() {
                    Log.d("Unity_ads", "Initialization Complete");
                    Log.d("Unity_ads_id", adsPref.getUnityGameId());
                }

                @Override
                public void onInitializationFailed(UnityAds.UnityAdsInitializationError error, String message) {
                    Log.d("Unity_ads", "Initialization Failed: [" + error + "] " + message);
                }
            });
        }

        loadBannerAdNetwork();
        setupToolbar();

        navigation = findViewById(R.id.navigation);
        if (sharedPref.getIsDarkTheme()) {
            navigation.setBackgroundColor(getResources().getColor(R.color.colorToolbarDark));
        } else {
            navigation.setBackgroundColor(getResources().getColor(R.color.colorBackgroundLight));
        }
        navigation.setLabelVisibilityMode(LabelVisibilityMode.LABEL_VISIBILITY_LABELED);
        initViewPager();
        getAdsLog();

        NotificationUtils.oneSignalNotificationHandler(this, getIntent());

    }

    public void setupToolbar() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            getSupportActionBar().setTitle(getString(R.string.app_name));
        }

        if (!sharedPref.getIsDarkTheme()) {
            toolbar.setPopupTheme(R.style.ThemeOverlay_AppCompat_Light);
        } else {
            Tools.darkToolbar(this, toolbar);
            toolbar.getContext().setTheme(R.style.ThemeOverlay_AppCompat_Dark);
        }
    }

    @SuppressLint("NonConstantResourceId")
    public void initViewPager() {
        if (Config.ENABLE_RTL_MODE) {
            viewPagerRTL = findViewById(R.id.view_pager_rtl);
            viewPagerRTL.setAdapter(new MyAdapter(getSupportFragmentManager()));
            viewPagerRTL.setOffscreenPageLimit(pager_number);
            navigation.setOnNavigationItemSelectedListener(item -> {
                switch (item.getItemId()) {
                    case R.id.navigation_home:
                        viewPagerRTL.setCurrentItem(0);
                        return true;
                    case R.id.navigation_category:
                        viewPagerRTL.setCurrentItem(1);
                        return true;
                    case R.id.navigation_favorite:
                        viewPagerRTL.setCurrentItem(2);
                        return true;
                }
                return false;
            });

            viewPagerRTL.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

                }

                @Override
                public void onPageSelected(int position) {
                    if (prevMenuItem != null) {
                        prevMenuItem.setChecked(false);
                    } else {
                        navigation.getMenu().getItem(0).setChecked(false);
                    }
                    navigation.getMenu().getItem(position).setChecked(true);
                    prevMenuItem = navigation.getMenu().getItem(position);

                    if (viewPagerRTL.getCurrentItem() == 0) {
                        toolbar.setTitle(getResources().getString(R.string.app_name));
                    } else if (viewPagerRTL.getCurrentItem() == 1) {
                        toolbar.setTitle(getResources().getString(R.string.title_nav_category));
                    } else if (viewPagerRTL.getCurrentItem() == 2) {
                        toolbar.setTitle(getResources().getString(R.string.title_nav_favorite));
                    }

                }

                @Override
                public void onPageScrollStateChanged(int state) {

                }
            });
        } else {
            viewPager = findViewById(R.id.view_pager);
            viewPager.setAdapter(new MyAdapter(getSupportFragmentManager()));
            viewPager.setOffscreenPageLimit(pager_number);
            navigation.setOnNavigationItemSelectedListener(item -> {
                switch (item.getItemId()) {
                    case R.id.navigation_home:
                        viewPager.setCurrentItem(0);
                        return true;
                    case R.id.navigation_category:
                        viewPager.setCurrentItem(1);
                        return true;
                    case R.id.navigation_favorite:
                        viewPager.setCurrentItem(2);
                        return true;
                }
                return false;
            });

            viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

                }

                @Override
                public void onPageSelected(int position) {
                    if (prevMenuItem != null) {
                        prevMenuItem.setChecked(false);
                    } else {
                        navigation.getMenu().getItem(0).setChecked(false);
                    }
                    navigation.getMenu().getItem(position).setChecked(true);
                    prevMenuItem = navigation.getMenu().getItem(position);

                    if (viewPager.getCurrentItem() == 0) {
                        toolbar.setTitle(getResources().getString(R.string.app_name));
                    } else if (viewPager.getCurrentItem() == 1) {
                        toolbar.setTitle(getResources().getString(R.string.title_nav_category));
                    } else if (viewPager.getCurrentItem() == 2) {
                        toolbar.setTitle(getResources().getString(R.string.title_nav_favorite));
                    }

                }

                @Override
                public void onPageScrollStateChanged(int state) {

                }
            });
        }
    }

    public class MyAdapter extends FragmentPagerAdapter {

        MyAdapter(FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            if (position == 0) {
                return new FragmentTabLayout();
            } else if (position == 1) {
                return new FragmentCategory();
            } else {
                return new FragmentFavorite();
            }
        }

        @Override
        public int getCount() {
            return pager_number;
        }

    }

    @Override
    public AssetManager getAssets() {
        return getResources().getAssets();
    }

    @Override
    public void onBackPressed() {
        if (Config.ENABLE_RTL_MODE) {
            if (viewPagerRTL.getCurrentItem() != 0) {
                viewPagerRTL.setCurrentItem((0), true);
            } else {
                exitApp();
            }
        } else {
            if (viewPager.getCurrentItem() != 0) {
                viewPager.setCurrentItem((0), true);
            } else {
                exitApp();
            }
        }
    }

    public void exitApp() {
        if ((System.currentTimeMillis() - exitTime) > 2000) {
            Snackbar.make(coordinatorLayout, getString(R.string.snackbar_exit), Snackbar.LENGTH_SHORT).show();
            exitTime = System.currentTimeMillis();
        } else {
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_search) {
            if (viewPager.getCurrentItem() == 1) {
                Toast.makeText(this, "search category", Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(getApplicationContext(), ActivitySearch.class);
                startActivity(intent);
            }
        } else if (item.getItemId() == R.id.menu_settings) {
            Intent intent = new Intent(getApplicationContext(), ActivitySettings.class);
            startActivity(intent);
        } else if (item.getItemId() == R.id.menu_rate) {
            final String package_name = BuildConfig.APPLICATION_ID;
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + package_name)));
            } catch (android.content.ActivityNotFoundException anfe) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + package_name)));
            }
        } else if (item.getItemId() == R.id.menu_more) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.play_more_apps))));
        } else if (item.getItemId() == R.id.menu_about) {
            aboutDialog();
        }
        return super.onOptionsItemSelected(item);
    }

    public void aboutDialog() {
        LayoutInflater layoutInflaterAndroid = LayoutInflater.from(MainActivity.this);
        View view = layoutInflaterAndroid.inflate(R.layout.dialog_about, null);

        TextView txt_app_version = view.findViewById(R.id.txt_app_version);
        txt_app_version.setText(getString(R.string.msg_about_version) + " " + BuildConfig.VERSION_NAME);

        final AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
        alert.setView(view);
        alert.setPositiveButton(R.string.dialog_option_ok, (dialog, which) -> dialog.dismiss());
        alert.show();
    }

    public void updateConsentStatus() {
        if (BuildConfig.DEBUG) {
            ConsentDebugSettings debugSettings = new ConsentDebugSettings.Builder(this)
                    .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_NOT_EEA)
                    .addTestDeviceHashedId("TEST-DEVICE-HASHED-ID")
                    .build();
            ConsentRequestParameters params = new ConsentRequestParameters.Builder().setConsentDebugSettings(debugSettings).build();
            consentInformation = UserMessagingPlatform.getConsentInformation(this);
            consentInformation.requestConsentInfoUpdate(this, params, () -> {
                        if (consentInformation.isConsentFormAvailable()) {
                            loadForm();
                        }
                    },
                    formError -> {
                    });
        } else {
            ConsentRequestParameters params = new ConsentRequestParameters.Builder().build();
            consentInformation = UserMessagingPlatform.getConsentInformation(this);
            consentInformation.requestConsentInfoUpdate(this, params, () -> {
                        if (consentInformation.isConsentFormAvailable()) {
                            loadForm();
                        }
                    },
                    formError -> {
                    });
        }
    }

    public void loadForm() {
        UserMessagingPlatform.loadConsentForm(this, consentForm -> {
                    MainActivity.this.consentForm = consentForm;
                    if (consentInformation.getConsentStatus() == ConsentInformation.ConsentStatus.REQUIRED) {
                        consentForm.show(MainActivity.this, formError -> {
                            loadForm();
                        });
                    }
                },
                formError -> {
                }
        );
    }

    public void loadBannerAdNetwork() {
        if (adsPref.getAdStatus().equals(AD_STATUS_ON) && adsPref.getBannerAdStatusHome() != 0) {
            switch (adsPref.getAdType()) {
                case ADMOB:
                    loadAdMobBannerAd();
                    break;
                case FAN:
                    loadFanBannerAd();
                    break;
                case STARTAPP:
                    loadStartAppBannerAd();
                    break;
                case UNITY:
                    loadUnityBannerAd();
                    break;
            }
        }
    }

    public void loadAdMobBannerAd() {
        adContainerView = findViewById(R.id.admob_banner_view_container);
        adContainerView.post(() -> {
            adView = new AdView(this);
            adView.setAdUnitId(adsPref.getAdMobBannerId());
            adContainerView.removeAllViews();
            adContainerView.addView(adView);
            adView.setAdSize(Tools.getAdSize(this));
            adView.loadAd(Tools.getAdRequest(this));
            adView.setAdListener(new AdListener() {
                @Override
                public void onAdLoaded() {
                    // Code to be executed when an ad finishes loading.
                    adContainerView.setVisibility(View.VISIBLE);
                }

                @Override
                public void onAdFailedToLoad(LoadAdError adError) {
                    // Code to be executed when an ad request fails.
                    adContainerView.setVisibility(View.GONE);
                }

                @Override
                public void onAdOpened() {
                    // Code to be executed when an ad opens an overlay that
                    // covers the screen.
                }

                @Override
                public void onAdClicked() {
                    // Code to be executed when the user clicks on an ad.
                }

                @Override
                public void onAdClosed() {
                    // Code to be executed when the user is about to return
                    // to the app after tapping on an ad.
                }
            });
        });
    }

    private void loadFanBannerAd() {
        if (BuildConfig.DEBUG) {
            fanAdView = new com.facebook.ads.AdView(this, "IMG_16_9_APP_INSTALL#" + adsPref.getFanBannerUnitId(), AdSize.BANNER_HEIGHT_50);
        } else {
            fanAdView = new com.facebook.ads.AdView(this, adsPref.getFanBannerUnitId(), AdSize.BANNER_HEIGHT_50);
        }
        LinearLayout adContainer = findViewById(R.id.fan_banner_view_container);
        // Add the ad view to your activity layout
        adContainer.addView(fanAdView);
        com.facebook.ads.AdListener adListener = new com.facebook.ads.AdListener() {
            @Override
            public void onError(Ad ad, AdError adError) {
                adContainer.setVisibility(View.GONE);
                Log.d("FAN_ERROR", "Error: " + adError.getErrorMessage());
            }

            @Override
            public void onAdLoaded(Ad ad) {
                adContainer.setVisibility(View.VISIBLE);
                Log.d("FAN_SUCCESS", "Success");
            }

            @Override
            public void onAdClicked(Ad ad) {

            }

            @Override
            public void onLoggingImpression(Ad ad) {

            }
        };
        com.facebook.ads.AdView.AdViewLoadConfig loadAdConfig = fanAdView.buildLoadAdConfig().withAdListener(adListener).build();
        fanAdView.loadAd(loadAdConfig);
    }

    private void loadStartAppBannerAd() {
        RelativeLayout bannerLayout = findViewById(R.id.startapp_banner_view_container);
        Banner banner = new Banner(this, new BannerListener() {
            @Override
            public void onReceiveAd(View banner) {
                bannerLayout.setVisibility(View.VISIBLE);
            }

            @Override
            public void onFailedToReceiveAd(View banner) {
                bannerLayout.setVisibility(View.GONE);
            }

            @Override
            public void onImpression(View view) {

            }

            @Override
            public void onClick(View banner) {
            }
        });
        bannerLayout.addView(banner);
    }

    private void loadUnityBannerAd() {
        bottomBanner = new BannerView(MainActivity.this, adsPref.getUnityBannerPlacementId(), new UnityBannerSize(UNITY_ADS_BANNER_WIDTH, UNITY_ADS_BANNER_HEIGHT));
        bottomBanner.setListener(new BannerView.IListener() {
            @Override
            public void onBannerLoaded(BannerView bannerView) {
                bottomBannerView.setVisibility(View.VISIBLE);
                Log.d("Unity_banner", "ready");
            }

            @Override
            public void onBannerClick(BannerView bannerView) {

            }

            @Override
            public void onBannerFailedToLoad(BannerView bannerView, BannerErrorInfo bannerErrorInfo) {
                Log.d("SupportTest", "Banner Error" + bannerErrorInfo);
                bottomBannerView.setVisibility(View.GONE);
            }

            @Override
            public void onBannerLeftApplication(BannerView bannerView) {

            }
        });
        bottomBannerView = findViewById(R.id.unity_banner_view_container);
        bottomBannerView.addView(bottomBanner);
        bottomBanner.load();
    }

    @Override
    protected void onDestroy() {
        if (adsPref.getAdStatus().equals(AD_STATUS_ON) && adsPref.getAdType().equals(FAN)) {
            if (fanAdView != null) {
                fanAdView.destroy();
            }
        }
        super.onDestroy();
    }

    public void getAdsLog() {
        Log.d("Native_ad", "" + adsPref.getBannerAdStatusHome());
    }

}