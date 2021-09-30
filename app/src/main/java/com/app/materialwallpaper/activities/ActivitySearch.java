package com.app.materialwallpaper.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.AdSize;
import com.facebook.ads.InterstitialAdListener;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.material.snackbar.Snackbar;
import com.app.materialwallpaper.BuildConfig;
import com.app.materialwallpaper.R;
import com.app.materialwallpaper.adapters.AdapterSearch;
import com.app.materialwallpaper.adapters.AdapterWallpaper;
import com.app.materialwallpaper.callbacks.CallbackWallpaper;
import com.app.materialwallpaper.models.Wallpaper;
import com.app.materialwallpaper.rests.ApiInterface;
import com.app.materialwallpaper.rests.RestAdapter;
import com.app.materialwallpaper.utils.AdsPref;
import com.app.materialwallpaper.utils.AudienceNetworkInitializeHelper;
import com.app.materialwallpaper.utils.Constant;
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
import static com.app.materialwallpaper.utils.Constant.STARTAPP;
import static com.app.materialwallpaper.utils.Constant.UNITY;
import static com.app.materialwallpaper.utils.Constant.UNITY_ADS_BANNER_HEIGHT;
import static com.app.materialwallpaper.utils.Constant.UNITY_ADS_BANNER_WIDTH;

public class ActivitySearch extends AppCompatActivity {

    private EditText et_search;
    private RecyclerView recyclerView;
    private AdapterWallpaper adapterWallpaper;
    private RecyclerView recyclerSuggestion;
    private AdapterSearch mAdapterSuggestion;
    private LinearLayout lyt_suggestion;
    private ImageButton bt_clear;
    private Call<CallbackWallpaper> callbackCall = null;
    private ShimmerFrameLayout lyt_shimmer;
    private int post_total = 0;
    private int failed_page = 0;
    String tags = "";
    List<Wallpaper> items = new ArrayList<>();
    private FrameLayout adContainerView;
    private AdView adView;
    com.facebook.ads.AdView fanAdView;
    BannerView bottomBanner;
    RelativeLayout bottomBannerView;
    private InterstitialAd adMobInterstitialAd;
    private com.facebook.ads.InterstitialAd fanInterstitialAd;
    private StartAppAd startAppAd;
    SharedPref sharedPref;
    AdsPref adsPref;
    CoordinatorLayout parent_view;
    int counter = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tools.getTheme(this);
        adsPref = new AdsPref(this);
        sharedPref = new SharedPref(this);
        if (adsPref.getAdStatus().equals(AD_STATUS_ON) && adsPref.getAdType().equals(FAN)) {
            AudienceNetworkInitializeHelper.initialize(this);
        }
        if (sharedPref.getIsDarkTheme()) {
            Tools.darkNavigation(this);
        } else {
            Tools.lightNavigation(this);
        }
        setContentView(R.layout.activity_search);
        Tools.getRtlDirection(this);
        parent_view = findViewById(R.id.coordinatorLayout);
        initComponent();
        initShimmerLayout();
        setupToolbar();
        loadBannerAdNetwork();
        loadInterstitialAdNetwork();
    }

    private void initComponent() {
        lyt_suggestion = findViewById(R.id.lyt_suggestion);
        et_search = findViewById(R.id.et_search);
        bt_clear = findViewById(R.id.bt_clear);
        bt_clear.setVisibility(View.GONE);
        lyt_shimmer = findViewById(R.id.shimmer_view_container);
        recyclerView = findViewById(R.id.recyclerView);

        recyclerView = findViewById(R.id.recyclerView);
        ItemOffsetDecoration itemDecoration = new ItemOffsetDecoration(this, R.dimen.grid_space_wallpaper);
        recyclerView.addItemDecoration(itemDecoration);
        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(sharedPref.getWallpaperColumns(), StaggeredGridLayoutManager.VERTICAL));
        recyclerView.setHasFixedSize(true);

        recyclerSuggestion = findViewById(R.id.recyclerSuggestion);
        recyclerSuggestion.setLayoutManager(new LinearLayoutManager(this));
        recyclerSuggestion.setHasFixedSize(true);

        et_search.addTextChangedListener(textWatcher);
        if (getIntent().hasExtra("tags")) {
            tags = getIntent().getStringExtra("tags");
            hideKeyboard();
            searchActionTags(1);
        } else {
            et_search.requestFocus();
            swipeProgress(false);
        }

        //set data and list adapter
        adapterWallpaper = new AdapterWallpaper(this, recyclerView, items);
        recyclerView.setAdapter(adapterWallpaper);
        adapterWallpaper.setOnItemClickListener((view, obj, position) -> {
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
                searchAction(next_page);
            } else {
                adapterWallpaper.setLoaded();
            }
        });

        //set data and list adapter suggestion
        mAdapterSuggestion = new AdapterSearch(this);
        recyclerSuggestion.setAdapter(mAdapterSuggestion);
        showSuggestionSearch();
        mAdapterSuggestion.setOnItemClickListener((view, viewModel, pos) -> {
            et_search.setText(viewModel);
            lyt_suggestion.setVisibility(View.GONE);
            hideKeyboard();
            searchAction(1);
        });

        bt_clear.setOnClickListener(view -> et_search.setText(""));

        et_search.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                if (et_search.getText().toString().equals("")) {
                    Snackbar.make(parent_view, getString(R.string.msg_search_input), Snackbar.LENGTH_SHORT).show();
                    hideKeyboard();
                    swipeProgress(false);
                } else {
                    adapterWallpaper.resetListData();
                    hideKeyboard();
                    searchAction(1);
                }
                return true;
            }
            return false;
        });

        et_search.setOnTouchListener((view, motionEvent) -> {
            showSuggestionSearch();
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            return false;
        });

    }

    public void setupToolbar() {
        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (sharedPref.getIsDarkTheme()) {
            Tools.darkToolbar(this, toolbar);
        } else {
            Tools.lightToolbar(this, toolbar);
        }
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setTitle("");
        }
    }

    TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void onTextChanged(CharSequence c, int i, int i1, int i2) {
            if (c.toString().trim().length() == 0) {
                bt_clear.setVisibility(View.GONE);
            } else {
                bt_clear.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void beforeTextChanged(CharSequence c, int i, int i1, int i2) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
        }
    };

    private void requestSearchApi(final int page_no, final String query) {
        ApiInterface apiInterface = RestAdapter.createAPI();
        callbackCall = apiInterface.getSearch(page_no, Constant.LOAD_MORE, query, Constant.ORDER_RECENT);
        callbackCall.enqueue(new Callback<CallbackWallpaper>() {
            @Override
            public void onResponse(Call<CallbackWallpaper> call, Response<CallbackWallpaper> response) {
                CallbackWallpaper resp = response.body();
                if (resp != null && resp.status.equals("ok")) {
                    post_total = resp.count_total;
                    adapterWallpaper.insertData(resp.posts);
                    if (resp.posts.size() == 0) showNotFoundView(true);
                } else {
                    onFailRequest(page_no);
                }
                swipeProgress(false);
            }

            @Override
            public void onFailure(Call<CallbackWallpaper> call, Throwable t) {
                onFailRequest(page_no);
                swipeProgress(false);
            }

        });
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

    private void searchAction(final int page_no) {
        lyt_suggestion.setVisibility(View.GONE);
        showFailedView(false, "");
        showNotFoundView(false);
        final String query = et_search.getText().toString().trim();
        if (!query.equals("")) {
            if (page_no == 1) {
                swipeProgress(true);
            } else {
                adapterWallpaper.setLoading();
            }
            new Handler(Looper.getMainLooper()).postDelayed(() -> requestSearchApi(page_no, query), Constant.DELAY_TIME);
        } else {
            Snackbar.make(parent_view, getString(R.string.msg_search_input), Snackbar.LENGTH_SHORT).show();
            swipeProgress(false);
        }
    }

    private void searchActionTags(final int page_no) {
        lyt_suggestion.setVisibility(View.GONE);
        showFailedView(false, "");
        showNotFoundView(false);
        et_search.setText(tags);
        final String query = et_search.getText().toString().trim();
        if (!query.equals("")) {
            if (page_no == 1) {
                swipeProgress(true);
            } else {
                adapterWallpaper.setLoading();
            }
            new Handler(Looper.getMainLooper()).postDelayed(() -> requestSearchApi(page_no, query), Constant.DELAY_TIME);
        } else {
            Snackbar.make(parent_view, getString(R.string.msg_search_input), Snackbar.LENGTH_SHORT).show();
            swipeProgress(false);
        }
    }

    private void showSuggestionSearch() {
        mAdapterSuggestion.refreshItems();
        lyt_suggestion.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
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
        findViewById(R.id.failed_retry).setOnClickListener(view -> searchAction(failed_page));
    }

    private void showNotFoundView(boolean show) {
        View lyt_no_item = findViewById(R.id.lyt_no_item);
        ((TextView) findViewById(R.id.no_item_message)).setText(R.string.no_search_found);
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
            lyt_shimmer.setVisibility(View.GONE);
            lyt_shimmer.stopShimmer();
            return;
        } else {
            lyt_shimmer.setVisibility(View.VISIBLE);
            lyt_shimmer.startShimmer();
        }
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

    @Override
    public void onBackPressed() {
        if (getIntent().hasExtra("tags")) {
            super.onBackPressed();
        } else {
            if (et_search.length() > 0) {
                et_search.setText("");
            } else {
                super.onBackPressed();
            }
        }
    }

    public void loadBannerAdNetwork() {
        if (adsPref.getAdStatus().equals(AD_STATUS_ON) && adsPref.getBannerAdStatusSearch() != 0) {
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
        bottomBanner = new BannerView(ActivitySearch.this, adsPref.getUnityBannerPlacementId(), new UnityBannerSize(UNITY_ADS_BANNER_WIDTH, UNITY_ADS_BANNER_HEIGHT));
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
                adMobInterstitialAd = new InterstitialAd(ActivitySearch.this);
                adMobInterstitialAd.setAdUnitId(adsPref.getAdMobInterstitialId());
                adMobInterstitialAd.loadAd(Tools.getAdRequest(ActivitySearch.this));
                adMobInterstitialAd.setAdListener(new AdListener() {
                    @Override
                    public void onAdClosed() {
                        adMobInterstitialAd.loadAd(Tools.getAdRequest(ActivitySearch.this));
                    }
                });
            }
        } else if (adsPref.getAdStatus().equals(AD_STATUS_ON) && adsPref.getAdType().equals(FAN)) {
            if (adsPref.getInterstitialAdClickWallpaper() != 0) {
                if (BuildConfig.DEBUG) {
                    fanInterstitialAd = new com.facebook.ads.InterstitialAd(ActivitySearch.this, "IMG_16_9_APP_INSTALL#" + adsPref.getFanInterstitialUnitId());
                } else {
                    fanInterstitialAd = new com.facebook.ads.InterstitialAd(ActivitySearch.this, adsPref.getFanInterstitialUnitId());
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
                startAppAd = new StartAppAd(ActivitySearch.this);
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
                        UnityAds.show(ActivitySearch.this, adsPref.getUnityInterstitialPlacementId());
                        counter = 1;
                    } else {
                        counter++;
                    }
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

}
