package com.app.materialwallpaper.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.InterstitialAdListener;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.InterstitialAd;
import com.app.materialwallpaper.BuildConfig;
import com.app.materialwallpaper.Config;
import com.app.materialwallpaper.R;
import com.app.materialwallpaper.activities.ActivityCategoryDetails;
import com.app.materialwallpaper.adapters.AdapterCategory;
import com.app.materialwallpaper.callbacks.CallbackCategory;
import com.app.materialwallpaper.models.Category;
import com.app.materialwallpaper.rests.ApiInterface;
import com.app.materialwallpaper.rests.RestAdapter;
import com.app.materialwallpaper.utils.AdsPref;
import com.app.materialwallpaper.utils.Constant;
import com.app.materialwallpaper.utils.DBHelper;
import com.app.materialwallpaper.utils.ItemOffsetDecoration;
import com.app.materialwallpaper.utils.Tools;
import com.startapp.sdk.adsbase.StartAppAd;
import com.unity3d.ads.UnityAds;

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

public class FragmentCategory extends Fragment {

    private View root_view;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ShimmerFrameLayout lyt_shimmer;
    private AdapterCategory adapterCategory;
    private Call<CallbackCategory> callbackCall = null;
    private InterstitialAd adMobInterstitialAd;
    private com.facebook.ads.InterstitialAd fanInterstitialAd;
    private StartAppAd startAppAd;
    private AdsPref adsPref;
    DBHelper dbHelper;
    int counter = 1;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        root_view = inflater.inflate(R.layout.fragment_category, container, false);

        dbHelper = new DBHelper(getActivity());
        adsPref = new AdsPref(getActivity());
        loadInterstitialAdNetwork();

        swipeRefreshLayout = root_view.findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        lyt_shimmer = root_view.findViewById(R.id.shimmer_view_container);

        recyclerView = root_view.findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        int padding = getResources().getDimensionPixelOffset(R.dimen.grid_space_wallpaper);
        recyclerView.setPadding(padding, padding, padding, padding);

        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(Config.DEFAULT_CATEGORY_COLUMN, LinearLayoutManager.VERTICAL));
        ItemOffsetDecoration itemDecoration = new ItemOffsetDecoration(getActivity(), R.dimen.grid_space_wallpaper);
        recyclerView.addItemDecoration(itemDecoration);
        recyclerView.setHasFixedSize(true);

        //set data and list adapter
        adapterCategory = new AdapterCategory(getActivity(), new ArrayList<>());
        recyclerView.setAdapter(adapterCategory);

        // on item list clicked
        adapterCategory.setOnItemClickListener((v, obj, position) -> {
            Intent intent = new Intent(getActivity(), ActivityCategoryDetails.class);
            intent.putExtra(EXTRA_OBJC, obj);
            startActivity(intent);

            showInterstitialAdNetwork();
        });

        // on swipe list
        swipeRefreshLayout.setOnRefreshListener(() -> {
            adapterCategory.resetListData();
            requestAction();
        });

        requestAction();

        return root_view;
    }

    private void displayApiResult(final List<Category> categories) {
        adapterCategory.setListData(categories);
        swipeProgress(false);
        if (categories.size() == 0) {
            showNoItemView(true);
        }
    }

    private void requestCategoriesApi() {
        ApiInterface apiInterface = RestAdapter.createAPI();
        callbackCall = apiInterface.getCategories();
        callbackCall.enqueue(new Callback<CallbackCategory>() {
            @Override
            public void onResponse(Call<CallbackCategory> call, Response<CallbackCategory> response) {
                CallbackCategory resp = response.body();
                if (resp != null && resp.status.equals("ok")) {
                    displayApiResult(resp.categories);
                    dbHelper.truncateTableCategory(DBHelper.TABLE_CATEGORY);
                    dbHelper.addListCategory(resp.categories, DBHelper.TABLE_CATEGORY);
                } else {
                    onFailRequest();
                }
            }

            @Override
            public void onFailure(Call<CallbackCategory> call, Throwable t) {
                swipeProgress(false);
                List<Category> posts = dbHelper.getAllCategory(DBHelper.TABLE_CATEGORY);
                adapterCategory.setListData(posts);
                if (posts.size() == 0) {
                    if (!call.isCanceled()) onFailRequest();
                }
            }

        });
    }

    private void onFailRequest() {
        swipeProgress(false);
        if (Tools.isConnect(getActivity())) {
            showFailedView(true, getString(R.string.failed_text));
        } else {
            showFailedView(true, getString(R.string.failed_text));
        }
    }

    private void requestAction() {
        showFailedView(false, "");
        swipeProgress(true);
        showNoItemView(false);
        new Handler(Looper.getMainLooper()).postDelayed(this::requestCategoriesApi, Constant.DELAY_TIME);
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

    private void showFailedView(boolean flag, String message) {
        View lyt_failed = root_view.findViewById(R.id.lyt_failed_category);
        ((TextView) root_view.findViewById(R.id.failed_message)).setText(message);
        if (flag) {
            recyclerView.setVisibility(View.GONE);
            lyt_failed.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            lyt_failed.setVisibility(View.GONE);
        }
        root_view.findViewById(R.id.failed_retry).setOnClickListener(view -> requestAction());
    }

    private void showNoItemView(boolean show) {
        View lyt_no_item = root_view.findViewById(R.id.lyt_no_item_category);
        ((TextView) root_view.findViewById(R.id.no_item_message)).setText(R.string.no_category_found);
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

    private void loadInterstitialAdNetwork() {
        if (adsPref.getAdStatus().equals(AD_STATUS_ON) && adsPref.getAdType().equals(ADMOB)) {
            if (adsPref.getInterstitialAdClickWallpaper() != 0) {
                adMobInterstitialAd = new InterstitialAd(getActivity());
                adMobInterstitialAd.setAdUnitId(adsPref.getAdMobInterstitialId());
                adMobInterstitialAd.loadAd(Tools.getAdRequest(getActivity()));
                adMobInterstitialAd.setAdListener(new AdListener() {
                    @Override
                    public void onAdClosed() {
                        adMobInterstitialAd.loadAd(Tools.getAdRequest(getActivity()));
                    }
                });
            }
        } else if (adsPref.getAdStatus().equals(AD_STATUS_ON) && adsPref.getAdType().equals(FAN)) {
            if (adsPref.getInterstitialAdClickWallpaper() != 0) {
                if (BuildConfig.DEBUG) {
                    fanInterstitialAd = new com.facebook.ads.InterstitialAd(getActivity(), "IMG_16_9_APP_INSTALL#" + adsPref.getFanInterstitialUnitId());
                } else {
                    fanInterstitialAd = new com.facebook.ads.InterstitialAd(getActivity(), adsPref.getFanInterstitialUnitId());
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
                startAppAd = new StartAppAd(getActivity());
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
            if (UnityAds.isReady(adsPref.getUnityInterstitialPlacementId())) {
                if (counter == adsPref.getInterstitialAdInterval()) {
                    UnityAds.show(getActivity(), adsPref.getUnityInterstitialPlacementId());
                    counter = 1;
                } else {
                    counter++;
                }

            }

        }
    }

}
