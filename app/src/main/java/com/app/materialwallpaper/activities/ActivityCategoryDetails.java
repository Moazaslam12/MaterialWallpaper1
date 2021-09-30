package com.app.materialwallpaper.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.AdSize;
import com.facebook.ads.InterstitialAdListener;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.LoadAdError;
import com.app.materialwallpaper.BuildConfig;
import com.app.materialwallpaper.R;
import com.app.materialwallpaper.adapters.AdapterWallpaper;
import com.app.materialwallpaper.callbacks.CallbackWallpaper;
import com.app.materialwallpaper.models.Category;
import com.app.materialwallpaper.models.Wallpaper;
import com.app.materialwallpaper.rests.ApiInterface;
import com.app.materialwallpaper.rests.RestAdapter;
import com.app.materialwallpaper.utils.AdsPref;
import com.app.materialwallpaper.utils.AudienceNetworkInitializeHelper;
import com.app.materialwallpaper.utils.Constant;
import com.app.materialwallpaper.utils.DBHelper;
import com.app.materialwallpaper.utils.ItemOffsetDecoration;
import com.app.materialwallpaper.utils.SharedPref;
import com.app.materialwallpaper.utils.Tools;
import com.startapp.sdk.ads.banner.Banner;
import com.startapp.sdk.ads.banner.BannerListener;
import com.startapp.sdk.adsbase.StartAppAd;
import com.unity3d.ads.UnityAds;
import com.unity3d.services.banners.BannerErrorInfo;
import com.unity3d.services.banners.BannerView;
import com.unity3d.services.banners.UnityBannerSize;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.app.materialwallpaper.utils.Constant.ADMOB;
import static com.app.materialwallpaper.utils.Constant.AD_STATUS_ON;
import static com.app.materialwallpaper.utils.Constant.EXTRA_OBJC;
import static com.app.materialwallpaper.utils.Constant.FAN;
import static com.app.materialwallpaper.utils.Constant.FILTER_ALL;
import static com.app.materialwallpaper.utils.Constant.FILTER_LIVE;
import static com.app.materialwallpaper.utils.Constant.ORDER_FEATURED;
import static com.app.materialwallpaper.utils.Constant.ORDER_LIVE;
import static com.app.materialwallpaper.utils.Constant.ORDER_POPULAR;
import static com.app.materialwallpaper.utils.Constant.ORDER_RANDOM;
import static com.app.materialwallpaper.utils.Constant.ORDER_RECENT;
import static com.app.materialwallpaper.utils.Constant.STARTAPP;
import static com.app.materialwallpaper.utils.Constant.UNITY;
import static com.app.materialwallpaper.utils.Constant.UNITY_ADS_BANNER_HEIGHT;
import static com.app.materialwallpaper.utils.Constant.UNITY_ADS_BANNER_WIDTH;

public class ActivityCategoryDetails extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AdapterWallpaper adapterWallpaper;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ShimmerFrameLayout lyt_shimmer;
    private Call<CallbackWallpaper> callbackCall = null;
    private int post_total = 0;
    private int failed_page = 0;
    List<Wallpaper> items = new ArrayList<>();
    Category category;
    private String single_choice_selected;
    SharedPref sharedPref;
    private FrameLayout adContainerView;
    private AdView adView;
    com.facebook.ads.AdView fanAdView;
    BannerView bottomBanner;
    RelativeLayout bottomBannerView;
    private InterstitialAd adMobInterstitialAd;
    private com.facebook.ads.InterstitialAd fanInterstitialAd;
    private StartAppAd startAppAd;
    DBHelper dbHelper;
    AdsPref adsPref;
    int counter = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tools.getTheme(this);
        sharedPref = new SharedPref(this);
        adsPref = new AdsPref(this);
        if (adsPref.getAdStatus().equals(AD_STATUS_ON) && adsPref.getAdType().equals(FAN)) {
            AudienceNetworkInitializeHelper.initialize(this);
        }
        if (sharedPref.getIsDarkTheme()) {
            Tools.darkNavigation(this);
        } else {
            Tools.lightNavigation(this);
        }
        setContentView(R.layout.activity_category_details);
        Tools.getRtlDirection(this);
        dbHelper = new DBHelper(this);
        sharedPref.setDefaultSortWallpaper();
        category = (Category) getIntent().getSerializableExtra(EXTRA_OBJC);

        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        lyt_shimmer = findViewById(R.id.shimmer_view_container);
        initShimmerLayout();

        recyclerView = findViewById(R.id.recyclerView);
        ItemOffsetDecoration itemDecoration = new ItemOffsetDecoration(this, R.dimen.grid_space_wallpaper);
        recyclerView.addItemDecoration(itemDecoration);
        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(sharedPref.getWallpaperColumns(), StaggeredGridLayoutManager.VERTICAL));
        recyclerView.setHasFixedSize(true);

        //set data and list adapter
        adapterWallpaper = new AdapterWallpaper(this, recyclerView, items);
        recyclerView.setAdapter(adapterWallpaper);

        // on item list clicked
        adapterWallpaper.setOnItemClickListener((v, obj, position) -> {
            Intent intent = new Intent(getApplicationContext(), ActivityWallpaperDetail.class);
            intent.putExtra(Constant.POSITION, position);
            Bundle bundle = new Bundle();
            bundle.putSerializable(Constant.ARRAY_LIST, (Serializable) items);
            intent.putExtra(Constant.BUNDLE, bundle);
            intent.putExtra(EXTRA_OBJC, obj);
            startActivity(intent);

            showInterstitialAdNetwork();
        });

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView v, int state) {
                super.onScrollStateChanged(v, state);
                if (state == v.SCROLL_STATE_DRAGGING || state == v.SCROLL_STATE_SETTLING) {

                } else {

                }
            }
        });

        // detect when scroll reach bottom
        adapterWallpaper.setOnLoadMoreListener(current_page -> {
            if (post_total > adapterWallpaper.getItemCount() && current_page != 0) {
                int next_page = current_page + 1;
                requestAction(next_page);
            } else {
                adapterWallpaper.setLoaded();
            }
        });

        // on swipe list
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (callbackCall != null && callbackCall.isExecuted()) callbackCall.cancel();
            adapterWallpaper.resetListData();
            if (Tools.isConnect(this)) {
                dbHelper.deleteWallpaperByCategory(DBHelper.TABLE_CATEGORY_DETAIL, category.category_id);
            }
            requestAction(1);
        });

        requestAction(1);
        loadBannerAdNetwork();
        loadInterstitialAdNetwork();
        setupToolbar();
        onOptionMenuClicked();

    }

    public void setupToolbar() {
        final Toolbar toolbar = findViewById(R.id.toolbar);
        if (sharedPref.getIsDarkTheme()) {
            Tools.darkToolbar(this, toolbar);
        } else {
            Tools.lightToolbar(this, toolbar);
        }
        final TextView title_toolbar = findViewById(R.id.title_toolbar);
        setSupportActionBar(toolbar);
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            title_toolbar.setText("" + category.category_name);
        }
    }

    private void displayApiResult(final List<Wallpaper> wallpapers) {
        adapterWallpaper.insertData(wallpapers);
        swipeProgress(false);
        if (wallpapers.size() == 0) {
            showNoItemView(true);
        }
    }

    private void requestListPostApi(final int page_no) {

        ApiInterface apiInterface = RestAdapter.createAPI();
        if (sharedPref.getCurrentSortWallpaper() == 0) {
            callbackCall = apiInterface.getCategoryDetails(page_no, Constant.LOAD_MORE, category.category_id, FILTER_ALL, ORDER_RECENT);
        } else if (sharedPref.getCurrentSortWallpaper() == 1) {
            callbackCall = apiInterface.getCategoryDetails(page_no, Constant.LOAD_MORE, category.category_id, FILTER_ALL, ORDER_FEATURED);
        } else if (sharedPref.getCurrentSortWallpaper() == 2) {
            callbackCall = apiInterface.getCategoryDetails(page_no, Constant.LOAD_MORE, category.category_id, FILTER_ALL, ORDER_POPULAR);
        } else if (sharedPref.getCurrentSortWallpaper() == 3) {
            callbackCall = apiInterface.getCategoryDetails(page_no, Constant.LOAD_MORE, category.category_id, FILTER_ALL, ORDER_RANDOM);
        } else if (sharedPref.getCurrentSortWallpaper() == 4) {
            callbackCall = apiInterface.getCategoryDetails(page_no, Constant.LOAD_MORE, category.category_id, FILTER_LIVE, ORDER_LIVE);
        }
        callbackCall.enqueue(new Callback<CallbackWallpaper>() {
            @Override
            public void onResponse(Call<CallbackWallpaper> call, Response<CallbackWallpaper> response) {
                CallbackWallpaper resp = response.body();
                if (resp != null && resp.status.equals("ok")) {
                    post_total = resp.count_total;
                    displayApiResult(resp.posts);
                    if (page_no == 1)
                        dbHelper.truncateTableWallpaper(DBHelper.TABLE_CATEGORY_DETAIL);
                    dbHelper.addListWallpaper(resp.posts, DBHelper.TABLE_CATEGORY_DETAIL);
                } else {
                    onFailRequest(page_no);
                }
            }

            @Override
            public void onFailure(Call<CallbackWallpaper> call, Throwable t) {
                swipeProgress(false);
                loadDataFromDatabase(call, page_no);
            }
        });
    }

    private void loadDataFromDatabase(Call<CallbackWallpaper> call, final int page_no) {
        List<Wallpaper> posts = dbHelper.getAllWallpaperByCategory(DBHelper.TABLE_CATEGORY_DETAIL, category.category_id);
        adapterWallpaper.insertData(posts);
        if (posts.size() == 0) {
            if (!call.isCanceled()) onFailRequest(page_no);
        }
    }

    private void onFailRequest(int page_no) {
        failed_page = page_no;
        adapterWallpaper.setLoaded();
        swipeProgress(false);
        if (Tools.isConnect(this)) {
            showFailedView(true, getString(R.string.failed_text));
        } else {
            showFailedView(true, getString(R.string.failed_text));
        }
    }

    private void requestAction(final int page_no) {
        showFailedView(false, "");
        showNoItemView(false);
        if (page_no == 1) {
            swipeProgress(true);
        } else {
            adapterWallpaper.setLoading();
        }
        new Handler(Looper.getMainLooper()).postDelayed(() -> requestListPostApi(page_no), Constant.DELAY_TIME);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        swipeProgress(false);
        if (callbackCall != null && callbackCall.isExecuted()) {
            callbackCall.cancel();
        }
        lyt_shimmer.stopShimmer();
    }

    private void showFailedView(boolean show, String message) {
        View lyt_failed = findViewById(R.id.lyt_failed);
        ((TextView) findViewById(R.id.failed_message)).setText(message);
        if (show) {
            recyclerView.setVisibility(View.GONE);
            lyt_failed.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            lyt_failed.setVisibility(View.GONE);
        }
        findViewById(R.id.failed_retry).setOnClickListener(view -> requestAction(failed_page));
    }

    private void showNoItemView(boolean show) {
        View lyt_no_item = findViewById(R.id.lyt_no_item);
        ((TextView) findViewById(R.id.no_item_message)).setText(R.string.msg_no_item);
        if (show) {
            recyclerView.setVisibility(View.GONE);
            lyt_no_item.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            lyt_no_item.setVisibility(View.GONE);
        }
    }

    private void swipeProgress(final boolean show) {
        if (!show) {
            swipeRefreshLayout.setRefreshing(show);
            lyt_shimmer.setVisibility(View.GONE);
            lyt_shimmer.stopShimmer();
            return;
        }
        swipeRefreshLayout.post(() -> {
            swipeRefreshLayout.setRefreshing(show);
            lyt_shimmer.setVisibility(View.VISIBLE);
            lyt_shimmer.startShimmer();
        });
    }

    public void initShimmerLayout() {
        View view_shimmer_2_columns = findViewById(R.id.view_shimmer_2_columns);
        View view_shimmer_3_columns = findViewById(R.id.view_shimmer_3_columns);
        if (sharedPref.getWallpaperColumns() == 3) {
            view_shimmer_2_columns.setVisibility(View.GONE);
            view_shimmer_3_columns.setVisibility(View.VISIBLE);
        } else {
            view_shimmer_2_columns.setVisibility(View.VISIBLE);
            view_shimmer_3_columns.setVisibility(View.GONE);
        }
    }

    public void onOptionMenuClicked() {

        findViewById(R.id.btn_search).setOnClickListener(view -> {
            Intent intent = new Intent(getApplicationContext(), ActivitySearch.class);
            startActivity(intent);
        });

        findViewById(R.id.btn_sort).setOnClickListener(view -> {
            String[] items = getResources().getStringArray(R.array.dialog_sort_wallpaper);
            single_choice_selected = items[sharedPref.getCurrentSortWallpaper()];
            int itemSelected = sharedPref.getCurrentSortWallpaper();
            new AlertDialog.Builder(ActivityCategoryDetails.this)
                    .setTitle(getString(R.string.title_sort))
                    .setSingleChoiceItems(items, itemSelected, (dialogInterface, i) -> single_choice_selected = items[i])
                    .setPositiveButton(R.string.dialog_option_ok, (dialogInterface, i) -> {
                        if (callbackCall != null && callbackCall.isExecuted())
                            callbackCall.cancel();
                        adapterWallpaper.resetListData();
                        if (Tools.isConnect(this)) {
                            dbHelper.deleteWallpaperByCategory(DBHelper.TABLE_CATEGORY_DETAIL, category.category_id);
                        }
                        requestAction(1);

                        if (single_choice_selected.equals(getResources().getString(R.string.menu_recent))) {
                            sharedPref.updateSortWallpaper(0);
                        } else if (single_choice_selected.equals(getResources().getString(R.string.menu_featured))) {
                            sharedPref.updateSortWallpaper(1);
                        } else if (single_choice_selected.equals(getResources().getString(R.string.menu_popular))) {
                            sharedPref.updateSortWallpaper(2);
                        } else if (single_choice_selected.equals(getResources().getString(R.string.menu_random))) {
                            sharedPref.updateSortWallpaper(3);
                        } else if (single_choice_selected.equals(getResources().getString(R.string.menu_live))) {
                            sharedPref.updateSortWallpaper(4);
                        } else {
                            sharedPref.updateSortWallpaper(0);
                        }

                        dialogInterface.dismiss();
                    })
                    .show();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    public void loadBannerAdNetwork() {
        if (adsPref.getAdStatus().equals(AD_STATUS_ON) && adsPref.getBannerAdStatusCategoryDetail() != 0) {
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
            }

            @Override
            public void onAdLoaded(Ad ad) {
                adContainer.setVisibility(View.VISIBLE);
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
        bottomBanner = new BannerView(ActivityCategoryDetails.this, adsPref.getUnityBannerPlacementId(), new UnityBannerSize(UNITY_ADS_BANNER_WIDTH, UNITY_ADS_BANNER_HEIGHT));
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

    private void loadInterstitialAdNetwork() {
        if (adsPref.getAdStatus().equals(AD_STATUS_ON) && adsPref.getAdType().equals(ADMOB)) {
            if (adsPref.getInterstitialAdClickWallpaper() != 0) {
                adMobInterstitialAd = new InterstitialAd(ActivityCategoryDetails.this);
                adMobInterstitialAd.setAdUnitId(adsPref.getAdMobInterstitialId());
                adMobInterstitialAd.loadAd(Tools.getAdRequest(ActivityCategoryDetails.this));
                adMobInterstitialAd.setAdListener(new AdListener() {
                    @Override
                    public void onAdClosed() {
                        adMobInterstitialAd.loadAd(Tools.getAdRequest(ActivityCategoryDetails.this));
                    }
                });
            }
        } else if (adsPref.getAdStatus().equals(AD_STATUS_ON) && adsPref.getAdType().equals(FAN)) {
            if (adsPref.getInterstitialAdClickWallpaper() != 0) {
                if (BuildConfig.DEBUG) {
                    fanInterstitialAd = new com.facebook.ads.InterstitialAd(ActivityCategoryDetails.this, "IMG_16_9_APP_INSTALL#" + adsPref.getFanInterstitialUnitId());
                } else {
                    fanInterstitialAd = new com.facebook.ads.InterstitialAd(ActivityCategoryDetails.this, adsPref.getFanInterstitialUnitId());
                }
                com.facebook.ads.InterstitialAdListener adListener = new InterstitialAdListener() {
                    @Override
                    public void onError(Ad ad, AdError adError) {

                    }

                    @Override
                    public void onAdLoaded(Ad ad) {

                    }

                    @Override
                    public void onAdClicked(Ad ad) {

                    }

                    @Override
                    public void onLoggingImpression(Ad ad) {

                    }

                    @Override
                    public void onInterstitialDisplayed(Ad ad) {

                    }

                    @Override
                    public void onInterstitialDismissed(Ad ad) {
                        fanInterstitialAd.loadAd();
                    }
                };

                com.facebook.ads.InterstitialAd.InterstitialLoadAdConfig loadAdConfig = fanInterstitialAd.buildLoadAdConfig().withAdListener(adListener).build();
                fanInterstitialAd.loadAd(loadAdConfig);
            }

        } else if (adsPref.getAdStatus().equals(AD_STATUS_ON) && adsPref.getAdType().equals(STARTAPP)) {
            if (adsPref.getInterstitialAdClickWallpaper() != 0) {
                startAppAd = new StartAppAd(ActivityCategoryDetails.this);
            }
        }
    }

    private void showInterstitialAdNetwork() {
        if (adsPref.getAdStatus().equals(AD_STATUS_ON) && adsPref.getAdType().equals(ADMOB)) {
            if (adsPref.getInterstitialAdClickWallpaper() != 0) {
                if (adMobInterstitialAd != null && adMobInterstitialAd.isLoaded()) {
                    if (counter == adsPref.getInterstitialAdInterval()) {
                        adMobInterstitialAd.show();
                        counter = 1;
                    } else {
                        counter++;
                    }
                }
            }
        } else if (adsPref.getAdStatus().equals(AD_STATUS_ON) && adsPref.getAdType().equals(FAN)) {
            if (adsPref.getInterstitialAdClickWallpaper() != 0) {
                if (fanInterstitialAd != null && fanInterstitialAd.isAdLoaded()) {
                    if (counter == adsPref.getInterstitialAdInterval()) {
                        fanInterstitialAd.show();
                        counter = 1;
                    } else {
                        counter++;
                    }
                }
            }
        } else if (adsPref.getAdStatus().equals(AD_STATUS_ON) && adsPref.getAdType().equals(STARTAPP)) {
            if (adsPref.getInterstitialAdClickWallpaper() != 0) {
                if (counter == adsPref.getInterstitialAdInterval()) {
                    startAppAd.showAd();
                    counter = 1;
                } else {
                    counter++;
                }
            }
        } else if (adsPref.getAdStatus().equals(AD_STATUS_ON) && adsPref.getAdType().equals(UNITY)) {
            if (adsPref.getInterstitialAdClickWallpaper() != 0) {
                if (UnityAds.isReady(adsPref.getUnityInterstitialPlacementId())) {
                    if (counter == adsPref.getInterstitialAdInterval()) {
                        UnityAds.show(ActivityCategoryDetails.this, adsPref.getUnityInterstitialPlacementId());
                        counter = 1;
                    } else {
                        counter++;
                    }
                }
            }

        }
    }

}
