package com.app.materialwallpaper.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
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
import com.app.materialwallpaper.R;
import com.app.materialwallpaper.activities.ActivityWallpaperDetail;
import com.app.materialwallpaper.adapters.AdapterWallpaper;
import com.app.materialwallpaper.callbacks.CallbackWallpaper;
import com.app.materialwallpaper.models.Wallpaper;
import com.app.materialwallpaper.rests.ApiInterface;
import com.app.materialwallpaper.rests.RestAdapter;
import com.app.materialwallpaper.utils.AdsPref;
import com.app.materialwallpaper.utils.Constant;
import com.app.materialwallpaper.utils.DBHelper;
import com.app.materialwallpaper.utils.ItemOffsetDecoration;
import com.app.materialwallpaper.utils.SharedPref;
import com.app.materialwallpaper.utils.Tools;
import com.startapp.sdk.adsbase.StartAppAd;
import com.unity3d.ads.UnityAds;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.app.materialwallpaper.utils.Constant.ADMOB;
import static com.app.materialwallpaper.utils.Constant.AD_STATUS_ON;
import static com.app.materialwallpaper.utils.Constant.FAN;
import static com.app.materialwallpaper.utils.Constant.STARTAPP;
import static com.app.materialwallpaper.utils.Constant.UNITY;

public class FragmentWallpaper extends Fragment {

    private static final String ARG_ORDER = "order";
    private static final String ARG_FILTER = "filter";
    View root_view;
    private RecyclerView recyclerView;
    private AdapterWallpaper adapterWallpaper;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ShimmerFrameLayout lyt_shimmer;
    private Call<CallbackWallpaper> callbackCall = null;
    private int post_total = 0;
    private int failed_page = 0;
    private InterstitialAd adMobInterstitialAd;
    private com.facebook.ads.InterstitialAd fanInterstitialAd;
    private StartAppAd startAppAd;
    private SharedPref sharedPref;
    private AdsPref adsPref;
    List<Wallpaper> items = new ArrayList<>();
    String order, filter;
    DBHelper dbHelper;
    int counter = 1;

    public FragmentWallpaper() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        order = getArguments() != null ? getArguments().getString(ARG_ORDER) : "";
        filter = getArguments() != null ? getArguments().getString(ARG_FILTER) : "";
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        root_view = inflater.inflate(R.layout.fragment_wallpaper, container, false);
        setHasOptionsMenu(true);

        dbHelper = new DBHelper(getActivity());
        sharedPref = new SharedPref(getActivity());
        adsPref = new AdsPref(getActivity());

        swipeRefreshLayout = root_view.findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        lyt_shimmer = root_view.findViewById(R.id.shimmer_view_container);
        initShimmerLayout();

        loadInterstitialAdNetwork();

        recyclerView = root_view.findViewById(R.id.recyclerView);
        ItemOffsetDecoration itemDecoration = new ItemOffsetDecoration(getActivity(), R.dimen.grid_space_wallpaper);
        recyclerView.addItemDecoration(itemDecoration);
        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(sharedPref.getWallpaperColumns(), StaggeredGridLayoutManager.VERTICAL));
        recyclerView.setHasFixedSize(true);

        //set data and list adapter
        adapterWallpaper = new AdapterWallpaper(getActivity(), recyclerView, items);
        recyclerView.setAdapter(adapterWallpaper);

        // on item list clicked
        adapterWallpaper.setOnItemClickListener((v, obj, position) -> {
            Intent intent = new Intent(getActivity(), ActivityWallpaperDetail.class);
            intent.putExtra(Constant.POSITION, position);
            Bundle bundle = new Bundle();
            bundle.putSerializable(Constant.ARRAY_LIST, (Serializable) items);
            intent.putExtra(Constant.BUNDLE, bundle);
            intent.putExtra(Constant.EXTRA_OBJC, obj);
            startActivity(intent);

            showInterstitialAdNetwork();
        });

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView v, int state) {
                super.onScrollStateChanged(v, state);
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
            if (Tools.isConnect(getActivity())) {
                dbHelper.deleteAll(DBHelper.TABLE_RECENT);
            }
            requestAction(1);
        });

        requestAction(1);

        return root_view;
    }

    public static FragmentWallpaper newInstance(String order, String filter) {
        FragmentWallpaper fragment = new FragmentWallpaper();
        Bundle args = new Bundle();
        args.putString(ARG_ORDER, order);
        args.putString(ARG_FILTER, filter);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
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
        callbackCall = apiInterface.getWallpapers(page_no, Constant.LOAD_MORE, filter, order);
        callbackCall.enqueue(new Callback<CallbackWallpaper>() {
            @Override
            public void onResponse(Call<CallbackWallpaper> call, Response<CallbackWallpaper> response) {
                CallbackWallpaper resp = response.body();
                if (resp != null && resp.status.equals("ok")) {
                    post_total = resp.count_total;
                    displayApiResult(resp.posts);
                    switch (order) {
                        case Constant.ORDER_RECENT:
                            if (page_no == 1)
                                dbHelper.truncateTableWallpaper(DBHelper.TABLE_RECENT);
                            dbHelper.addListWallpaper(resp.posts, DBHelper.TABLE_RECENT);
                            break;
                        case Constant.ORDER_FEATURED:
                            if (page_no == 1)
                                dbHelper.truncateTableWallpaper(DBHelper.TABLE_FEATURED);
                            dbHelper.addListWallpaper(resp.posts, DBHelper.TABLE_FEATURED);
                            break;
                        case Constant.ORDER_POPULAR:
                            if (page_no == 1)
                                dbHelper.truncateTableWallpaper(DBHelper.TABLE_POPULAR);
                            dbHelper.addListWallpaper(resp.posts, DBHelper.TABLE_POPULAR);
                            break;
                        case Constant.ORDER_RANDOM:
                            if (page_no == 1)
                                dbHelper.truncateTableWallpaper(DBHelper.TABLE_RANDOM);
                            dbHelper.addListWallpaper(resp.posts, DBHelper.TABLE_RANDOM);
                            break;
                        case Constant.ORDER_LIVE:
                            if (page_no == 1)
                                dbHelper.truncateTableWallpaper(DBHelper.TABLE_GIF);
                            dbHelper.addListWallpaper(resp.posts, DBHelper.TABLE_GIF);
                            break;
                    }
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
        switch (order) {
            case Constant.ORDER_RECENT: {
                List<Wallpaper> posts = dbHelper.getAllWallpaper(DBHelper.TABLE_RECENT);
                adapterWallpaper.insertData(posts);
                if (posts.size() == 0) {
                    if (!call.isCanceled()) onFailRequest(page_no);
                }
                break;
            }
            case Constant.ORDER_FEATURED: {
                List<Wallpaper> posts = dbHelper.getAllWallpaper(DBHelper.TABLE_FEATURED);
                adapterWallpaper.insertData(posts);
                if (posts.size() == 0) {
                    if (!call.isCanceled()) onFailRequest(page_no);
                }
                break;
            }
            case Constant.ORDER_POPULAR: {
                List<Wallpaper> posts = dbHelper.getAllWallpaper(DBHelper.TABLE_POPULAR);
                adapterWallpaper.insertData(posts);
                if (posts.size() == 0) {
                    if (!call.isCanceled()) onFailRequest(page_no);
                }
                break;
            }
            case Constant.ORDER_RANDOM: {
                List<Wallpaper> posts = dbHelper.getAllWallpaper(DBHelper.TABLE_RANDOM);
                adapterWallpaper.insertData(posts);
                if (posts.size() == 0) {
                    if (!call.isCanceled()) onFailRequest(page_no);
                }
                break;
            }
            case Constant.ORDER_LIVE: {
                List<Wallpaper> posts = dbHelper.getAllWallpaper(DBHelper.TABLE_GIF);
                adapterWallpaper.insertData(posts);
                if (posts.size() == 0) {
                    if (!call.isCanceled()) onFailRequest(page_no);
                }
                break;
            }
        }
    }

    private void onFailRequest(int page_no) {
        failed_page = page_no;
        adapterWallpaper.setLoaded();
        swipeProgress(false);
        if (Tools.isConnect(getActivity())) {
            showFailedView(true, getString(R.string.failed_text));
        } else {
            showFailedView(true, getString(R.string.failed_text));
        }
    }

    public void requestAction(final int page_no) {
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
        View lyt_failed = root_view.findViewById(R.id.lyt_failed);
        ((TextView) root_view.findViewById(R.id.failed_message)).setText(message);
        if (show) {
            recyclerView.setVisibility(View.GONE);
            lyt_failed.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            lyt_failed.setVisibility(View.GONE);
        }
        root_view.findViewById(R.id.failed_retry).setOnClickListener(view -> requestAction(failed_page));
    }

    private void showNoItemView(boolean show) {
        View lyt_no_item = root_view.findViewById(R.id.lyt_no_item);
        ((TextView) root_view.findViewById(R.id.no_item_message)).setText(R.string.msg_no_item);
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
        View view_shimmer_2_columns = root_view.findViewById(R.id.view_shimmer_2_columns);
        View view_shimmer_3_columns = root_view.findViewById(R.id.view_shimmer_3_columns);
        if (sharedPref.getWallpaperColumns() == 3) {
            view_shimmer_2_columns.setVisibility(View.GONE);
            view_shimmer_3_columns.setVisibility(View.VISIBLE);
        } else {
            view_shimmer_2_columns.setVisibility(View.VISIBLE);
            view_shimmer_3_columns.setVisibility(View.GONE);
        }
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
            if (adsPref.getInterstitialAdClickWallpaper() != 0) {
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

    @Override
    public void onResume() {
        super.onResume();
    }

}