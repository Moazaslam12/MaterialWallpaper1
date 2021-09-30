package com.app.materialwallpaper.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.WallpaperManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.beloo.widget.chipslayoutmanager.ChipsLayoutManager;
import com.beloo.widget.chipslayoutmanager.SpacingItemDecoration;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.AdSize;
import com.facebook.ads.InterstitialAdListener;
import com.github.chrisbanes.photoview.PhotoView;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.app.materialwallpaper.BuildConfig;
import com.app.materialwallpaper.Config;
import com.app.materialwallpaper.R;
import com.app.materialwallpaper.adapters.AdapterTags;
import com.app.materialwallpaper.callbacks.CallbackDetail;
import com.app.materialwallpaper.models.Wallpaper;
import com.app.materialwallpaper.rests.ApiInterface;
import com.app.materialwallpaper.rests.RestAdapter;
import com.app.materialwallpaper.utils.AdsPref;
import com.app.materialwallpaper.utils.AudienceNetworkInitializeHelper;
import com.app.materialwallpaper.utils.Constant;
import com.app.materialwallpaper.utils.DBHelper;
import com.app.materialwallpaper.utils.SharedPref;
import com.app.materialwallpaper.utils.Tools;
import com.startapp.sdk.ads.banner.Banner;
import com.startapp.sdk.ads.banner.BannerListener;
import com.startapp.sdk.adsbase.StartAppAd;
import com.unity3d.ads.UnityAds;
import com.unity3d.services.banners.BannerErrorInfo;
import com.unity3d.services.banners.BannerView;
import com.unity3d.services.banners.UnityBannerSize;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.app.materialwallpaper.utils.Constant.ADMOB;
import static com.app.materialwallpaper.utils.Constant.AD_STATUS_ON;
import static com.app.materialwallpaper.utils.Constant.BASE_IMAGE_URL;
import static com.app.materialwallpaper.utils.Constant.FAN;
import static com.app.materialwallpaper.utils.Constant.STARTAPP;
import static com.app.materialwallpaper.utils.Constant.UNITY;
import static com.app.materialwallpaper.utils.Constant.UNITY_ADS_BANNER_HEIGHT;
import static com.app.materialwallpaper.utils.Constant.UNITY_ADS_BANNER_WIDTH;

public class ActivityNotificationDetail extends AppCompatActivity {

    Wallpaper wallpaper;
    Toolbar toolbar;
    ActionBar actionBar;
    private String single_choice_selected;
    CoordinatorLayout parent_view;
    private BottomSheetDialog mBottomSheetDialog;
    SharedPref sharedPref;
    DBHelper dbHelper;
    AdsPref adsPref;
    private FrameLayout adContainerView;
    private AdView adView;
    com.facebook.ads.AdView fanAdView;
    BannerView bottomBanner;
    RelativeLayout bottomBannerView;
    private InterstitialAd adMobInterstitialAd;
    private com.facebook.ads.InterstitialAd fanInterstitialAd;
    private StartAppAd startAppAd;
    Call<CallbackDetail> callbackCall = null;
    boolean flag = true;
    String wallpaper_id;

    @SuppressWarnings("unchecked")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tools.getTheme(this);
        sharedPref = new SharedPref(this);
        adsPref = new AdsPref(this);
        if (adsPref.getAdStatus().equals(AD_STATUS_ON) && adsPref.getAdType().equals(FAN)) {
            AudienceNetworkInitializeHelper.initialize(this);
        }
        if (adsPref.getBannerAdStatusDetail() != 0) {
            Tools.transparentStatusBar(this);
            if (sharedPref.getIsDarkTheme()) {
                Tools.darkNavigation(this);
            }
        } else {
            Tools.transparentStatusBarNavigation(this);
        }
        setContentView(R.layout.activity_notification_detail);
        Tools.getRtlDirection(this);
        parent_view = findViewById(R.id.coordinatorLayout);

        dbHelper = new DBHelper(this);

        Intent intent = getIntent();
        wallpaper_id = intent.getStringExtra("id");

        setupToolbar();
        requestWallpaperDetail();

        loadBannerAdNetwork();
        loadInterstitialAdNetwork();

    }

    private void requestWallpaperDetail() {
        callbackCall = RestAdapter.createAPI().getOneWallpaper(wallpaper_id);
        callbackCall.enqueue(new Callback<CallbackDetail>() {
            public void onResponse(Call<CallbackDetail> call, Response<CallbackDetail> response) {
                CallbackDetail resp = response.body();
                if (resp != null && resp.status.equals("ok")) {
                    wallpaper = resp.wallpaper;
                    loadView(wallpaper);
                }
            }

            public void onFailure(Call<CallbackDetail> call, Throwable th) {
                Log.e("onFailure", "" + th.getMessage());
            }
        });
    }

    public void loadView(Wallpaper wallpaper) {

        final PhotoView imageView = findViewById(R.id.image_view);
        if (Config.ENABLE_CENTER_CROP_IN_DETAIL_WALLPAPER) {
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        }

        imageView.setOnClickListener(v -> {
            if (flag) {
                fullScreenMode(true);
                flag = false;
            } else {
                fullScreenMode(false);
                flag = true;
            }
        });

        final ProgressBar progressBar = findViewById(R.id.progress_bar);

        if (wallpaper.type.equals("url")) {
            Glide.with(ActivityNotificationDetail.this)
                    .load(wallpaper.image_url.replace(" ", "%20"))
                    .placeholder(R.drawable.ic_transparent)
                    .thumbnail(0.3f)
                    //.centerCrop()
                    .addListener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            progressBar.setVisibility(View.GONE);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            progressBar.setVisibility(View.GONE);
                            return false;
                        }
                    })
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(imageView);
        } else {
            Glide.with(ActivityNotificationDetail.this)
                    .load(BASE_IMAGE_URL + wallpaper.image_upload.replace(" ", "%20"))
                    .placeholder(R.drawable.ic_transparent)
                    .thumbnail(0.3f)
                    //.centerCrop()
                    .addListener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            progressBar.setVisibility(View.GONE);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            progressBar.setVisibility(View.GONE);
                            return false;
                        }
                    })
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(imageView);
        }

        TextView title_toolbar = findViewById(R.id.title_toolbar);
        TextView sub_title_toolbar = findViewById(R.id.sub_title_toolbar);
        if (wallpaper.image_name.equals("")) {
            title_toolbar.setText(wallpaper.category_name);
            sub_title_toolbar.setVisibility(View.GONE);
        } else {
            title_toolbar.setText(wallpaper.image_name);
            sub_title_toolbar.setText(wallpaper.category_name);
            sub_title_toolbar.setVisibility(View.VISIBLE);
        }

        findViewById(R.id.btn_info).setOnClickListener(view -> showBottomSheetDialog(wallpaper));

        findViewById(R.id.btn_save).setOnClickListener(view -> {
            if (wallpaper.type.equals("upload")) {
                downloadWallpaper(BASE_IMAGE_URL + wallpaper.image_upload);
            } else if (wallpaper.type.equals("url")) {
                downloadWallpaper(wallpaper.image_url);
            }
        });

        findViewById(R.id.btn_share).setOnClickListener(view -> {
            if (wallpaper.type.equals("upload")) {
                shareWallpaper(BASE_IMAGE_URL + wallpaper.image_upload);
            } else if (wallpaper.type.equals("url")) {
                shareWallpaper(wallpaper.image_url);
            }
        });

        findViewById(R.id.btn_set_wallpaper).setOnClickListener(view -> {

            if (!verifyPermissions()) {
                return;
            }

            if (wallpaper.image_upload.endsWith(".gif") || wallpaper.image_url.endsWith(".gif")) {
                if (wallpaper.type.equals("upload")) {
                    setGif(BASE_IMAGE_URL + wallpaper.image_upload);
                } else if (wallpaper.type.equals("url")) {
                    setGif(wallpaper.image_url);
                }
            } else {
                if (Build.VERSION.SDK_INT >= 24) {
                    if (wallpaper.type.equals("upload")) {
                        dialogOptionSetWallpaper(BASE_IMAGE_URL + wallpaper.image_upload, wallpaper);
                    } else if (wallpaper.type.equals("url")) {
                        dialogOptionSetWallpaper(wallpaper.image_url, wallpaper);
                    }
                } else {
                    if (wallpaper.type.equals("upload")) {
                        setWallpaper(BASE_IMAGE_URL + wallpaper.image_upload);
                    } else if (wallpaper.type.equals("url")) {
                        setWallpaper(wallpaper.image_url);
                    }
                }
            }
        });

        favToggle(wallpaper);
        findViewById(R.id.btn_favorite).setOnClickListener(view -> {
            if (dbHelper.isFavoritesExist(wallpaper.image_id)) {
                dbHelper.deleteFavorites(wallpaper);
                Snackbar.make(parent_view, getString(R.string.snack_bar_favorite_removed), Snackbar.LENGTH_SHORT).show();
            } else {
                dbHelper.addOneFavorite(wallpaper);
                Snackbar.make(parent_view, getString(R.string.snack_bar_favorite_added), Snackbar.LENGTH_SHORT).show();
            }
            favToggle(wallpaper);
        });

        updateView(wallpaper.image_id);

    }

    private void favToggle(Wallpaper wallpaper) {
        ImageView img_favorite = findViewById(R.id.img_favorite);
        if (dbHelper.isFavoritesExist(wallpaper.image_id)) {
            img_favorite.setImageResource(R.drawable.ic_action_fav);
        } else {
            img_favorite.setImageResource(R.drawable.ic_action_fav_outline);
        }
    }

    public void setupToolbar() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        if (actionBar != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }
    }

    private void showBottomSheetDialog(Wallpaper wallpaper) {
        @SuppressLint("InflateParams") final View view = getLayoutInflater().inflate(R.layout.include_info, null);
        FrameLayout lyt_bottom_sheet = view.findViewById(R.id.bottom_sheet);

        if (sharedPref.getIsDarkTheme()) {
            lyt_bottom_sheet.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_rounded_dark));
        } else {
            lyt_bottom_sheet.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_rounded_default));
        }

        if (wallpaper.image_name.equals("")) {
            ((TextView) view.findViewById(R.id.txt_wallpaper_name)).setText("-");
        } else {
            ((TextView) view.findViewById(R.id.txt_wallpaper_name)).setText(wallpaper.image_name);
        }

        ((TextView) view.findViewById(R.id.txt_category_name)).setText(wallpaper.category_name);

        if (wallpaper.resolution.equals("0")) {
            ((TextView) view.findViewById(R.id.txt_resolution)).setText("-");
        } else {
            ((TextView) view.findViewById(R.id.txt_resolution)).setText(wallpaper.resolution);
        }

        if (wallpaper.size.equals("0")) {
            ((TextView) view.findViewById(R.id.txt_size)).setText("-");
        } else {
            ((TextView) view.findViewById(R.id.txt_size)).setText(wallpaper.size);
        }

        if (wallpaper.mime.equals("")) {
            ((TextView) view.findViewById(R.id.txt_mime_type)).setText("image/jpeg");
        } else {
            ((TextView) view.findViewById(R.id.txt_mime_type)).setText(wallpaper.mime);
        }

        ((TextView) view.findViewById(R.id.txt_view_count)).setText(Tools.withSuffix(wallpaper.views) + "");
        ((TextView) view.findViewById(R.id.txt_download_count)).setText(Tools.withSuffix(wallpaper.downloads) + "");

        LinearLayout lyt_tags = view.findViewById(R.id.lyt_tags);
        if (wallpaper.tags.equals("")) {
            lyt_tags.setVisibility(View.GONE);
        } else {
            lyt_tags.setVisibility(View.VISIBLE);
        }

        @SuppressWarnings("unchecked") ArrayList<String> arrayListTags = new ArrayList(Arrays.asList(wallpaper.tags.split(",")));
        AdapterTags adapterTags = new AdapterTags(this, arrayListTags);
        RecyclerView recycler_view_tags = view.findViewById(R.id.recycler_view_tags);
        ChipsLayoutManager spanLayoutManager = ChipsLayoutManager.newBuilder(getApplicationContext()).setOrientation(ChipsLayoutManager.HORIZONTAL).build();
        recycler_view_tags.addItemDecoration(new SpacingItemDecoration(getResources().getDimensionPixelOffset(R.dimen.chips_space), getResources().getDimensionPixelOffset(R.dimen.chips_space)));
        recycler_view_tags.setLayoutManager(spanLayoutManager);
        recycler_view_tags.setAdapter(adapterTags);

        if (sharedPref.getIsDarkTheme()) {
            mBottomSheetDialog = new BottomSheetDialog(this, R.style.SheetDialogDark);
        } else {
            mBottomSheetDialog = new BottomSheetDialog(this, R.style.SheetDialogLight);
        }
        mBottomSheetDialog.setContentView(view);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //noinspection deprecation
            mBottomSheetDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }

        BottomSheetBehavior bottomSheetBehavior = mBottomSheetDialog.getBehavior();
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                if (newState == BottomSheetBehavior.STATE_COLLAPSED) {

                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });

        mBottomSheetDialog.show();
        mBottomSheetDialog.setOnDismissListener(dialog -> {
            mBottomSheetDialog = null;
        });
    }

    @TargetApi(Build.VERSION_CODES.N)
    public void dialogOptionSetWallpaper(String imageURL, Wallpaper wp) {
        String[] items = getResources().getStringArray(R.array.dialog_set_wallpaper);
        single_choice_selected = items[0];
        int itemSelected = 0;
        new AlertDialog.Builder(ActivityNotificationDetail.this)
                .setTitle(R.string.dialog_set_title)
                .setSingleChoiceItems(items, itemSelected, (dialogInterface, i) -> single_choice_selected = items[i])
                .setPositiveButton(R.string.dialog_option_ok, (dialogInterface, i) -> {
                    Snackbar.make(parent_view, getString(R.string.snack_bar_applying), Snackbar.LENGTH_SHORT).show();
                    Glide.with(this)
                            .load(imageURL.replace(" ", "%20"))
                            .into(new CustomTarget<Drawable>() {
                                @Override
                                public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                                    Bitmap bitmap = ((BitmapDrawable) resource).getBitmap();
                                    if (single_choice_selected.equals(getResources().getString(R.string.set_home_screen))) {
                                        try {
                                            WallpaperManager wallpaperManager = WallpaperManager.getInstance(ActivityNotificationDetail.this);
                                            wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM);
                                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                                Snackbar.make(parent_view, getString(R.string.snack_bar_applied), Snackbar.LENGTH_SHORT).show();
                                                showInterstitialAdNetwork();
                                            }, Constant.DELAY_SET);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                            Snackbar.make(parent_view, getString(R.string.snack_bar_failed), Snackbar.LENGTH_SHORT).show();
                                        }
                                    } else if (single_choice_selected.equals(getResources().getString(R.string.set_lock_screen))) {
                                        try {
                                            WallpaperManager wallpaperManager = WallpaperManager.getInstance(ActivityNotificationDetail.this);
                                            wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK);
                                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                                Snackbar.make(parent_view, getString(R.string.snack_bar_applied), Snackbar.LENGTH_SHORT).show();
                                                showInterstitialAdNetwork();
                                            }, Constant.DELAY_SET);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                            Snackbar.make(parent_view, getString(R.string.snack_bar_failed), Snackbar.LENGTH_SHORT).show();
                                        }
                                    } else if (single_choice_selected.equals(getResources().getString(R.string.set_both))) {
                                        try {
                                            WallpaperManager wallpaperManager = WallpaperManager.getInstance(ActivityNotificationDetail.this);
                                            wallpaperManager.setBitmap(bitmap);
                                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                                Snackbar.make(parent_view, getString(R.string.snack_bar_applied), Snackbar.LENGTH_SHORT).show();
                                                showInterstitialAdNetwork();
                                            }, Constant.DELAY_SET);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                            Snackbar.make(parent_view, getString(R.string.snack_bar_failed), Snackbar.LENGTH_SHORT).show();
                                        }
                                    } else if (single_choice_selected.equals(getResources().getString(R.string.set_crop))) {
                                        if (wp.type.equals("upload")) {
                                            Intent intent = new Intent(getApplicationContext(), ActivityCropWallpaper.class);
                                            intent.putExtra("image_url", BASE_IMAGE_URL + wp.image_upload);
                                            startActivity(intent);
                                        } else if (wp.type.equals("url")) {
                                            Intent intent = new Intent(getApplicationContext(), ActivityCropWallpaper.class);
                                            intent.putExtra("image_url", wp.image_url);
                                            startActivity(intent);
                                        }
                                    } else if (single_choice_selected.equals(getResources().getString(R.string.set_with))) {
                                        //(new SetWallpaperFromOtherApp(ActivityWallpaperDetail.this)).execute(imageURL);
                                        if (wp.type.equals("upload")) {
                                            setWallpaperFromOtherApp(BASE_IMAGE_URL + wp.image_upload);
                                        } else if (wp.type.equals("url")) {
                                            setWallpaperFromOtherApp(wp.image_url);
                                        }
                                    }
                                }

                                @Override
                                public void onLoadCleared(@Nullable Drawable placeholder) {

                                }

                                @Override
                                public void onLoadFailed(@Nullable Drawable errorDrawable) {
                                    super.onLoadFailed(errorDrawable);
                                    Snackbar.make(parent_view, getString(R.string.snack_bar_failed), Snackbar.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton(R.string.dialog_option_cancel, null)
                .show();
    }

    public void setWallpaper(String imageURL) {
        Snackbar.make(parent_view, getString(R.string.snack_bar_applying), Snackbar.LENGTH_SHORT).show();
        Glide.with(this)
                .load(imageURL.replace(" ", "%20"))
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        Bitmap bitmap = ((BitmapDrawable) resource).getBitmap();
                        try {
                            WallpaperManager wallpaperManager = WallpaperManager.getInstance(ActivityNotificationDetail.this);
                            wallpaperManager.setBitmap(bitmap);
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                Snackbar.make(parent_view, getString(R.string.snack_bar_applied), Snackbar.LENGTH_SHORT).show();
                                showInterstitialAdNetwork();
                            }, Constant.DELAY_SET);
                        } catch (IOException e) {
                            e.printStackTrace();
                            Snackbar.make(parent_view, getString(R.string.snack_bar_failed), Snackbar.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {

                    }

                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        super.onLoadFailed(errorDrawable);
                        Snackbar.make(parent_view, getString(R.string.snack_bar_error), Snackbar.LENGTH_SHORT).show();
                    }
                });
    }

    public Boolean verifyPermissions() {
        int permissionExternalMemory = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionExternalMemory != PackageManager.PERMISSION_GRANTED) {
            String[] STORAGE_PERMISSIONS = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
            ActivityCompat.requestPermissions(this, STORAGE_PERMISSIONS, 1);
            return false;
        }
        return true;
    }

    public void setWallpaperFromOtherApp(String imageURL) {

        if (!verifyPermissions()) {
            return;
        }

        Glide.with(this)
                .download(imageURL.replace(" ", "%20"))
                .listener(new RequestListener<File>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<File> target, boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(File resource, Object model, Target<File> target, DataSource dataSource, boolean isFirstResource) {
                        try {
                            if (wallpaper.image_upload.endsWith(".gif") || wallpaper.image_url.endsWith(".gif")) {
                                Tools.setWallpaperFromOtherApp(ActivityNotificationDetail.this, Tools.getBytesFromFile(resource), Tools.createName(imageURL), "image/gif");
                            } else if (wallpaper.image_upload.endsWith(".png") || wallpaper.image_url.endsWith(".png")) {
                                Tools.setWallpaperFromOtherApp(ActivityNotificationDetail.this, Tools.getBytesFromFile(resource), Tools.createName(imageURL), "image/png");
                            } else {
                                Tools.setWallpaperFromOtherApp(ActivityNotificationDetail.this, Tools.getBytesFromFile(resource), Tools.createName(imageURL), "image/jpg");
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return true;
                    }
                }).submit();
    }

    public void setGif(String imageURL) {
        if (!verifyPermissions()) {
            return;
        }
        Snackbar.make(parent_view, getString(R.string.snack_bar_preparing), Snackbar.LENGTH_SHORT).show();
        Glide.with(this)
                .download(imageURL.replace(" ", "%20"))
                .listener(new RequestListener<File>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<File> target, boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(File resource, Object model, Target<File> target, DataSource dataSource, boolean isFirstResource) {
                        try {
                            if (wallpaper.image_upload.endsWith(".gif") || wallpaper.image_url.endsWith(".gif")) {
                                Tools.setGifWallpaper(ActivityNotificationDetail.this, Tools.getBytesFromFile(resource), Tools.createName(imageURL), "image/gif");
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return true;
                    }
                }).submit();
    }

    public void downloadWallpaper(String imageURL) {
        if (!verifyPermissions()) {
            return;
        }
        Snackbar.make(parent_view, getString(R.string.snack_bar_saving), Snackbar.LENGTH_SHORT).show();
        Glide.with(this)
                .download(imageURL.replace(" ", "%20"))
                .listener(new RequestListener<File>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<File> target, boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(File resource, Object model, Target<File> target, DataSource dataSource, boolean isFirstResource) {
                        try {
                            if (wallpaper.image_upload.endsWith(".gif") || wallpaper.image_url.endsWith(".gif")) {
                                Tools.saveImage(ActivityNotificationDetail.this, Tools.getBytesFromFile(resource), Tools.createName(imageURL), "image/gif");
                            } else if (wallpaper.image_upload.endsWith(".png") || wallpaper.image_url.endsWith(".png")) {
                                Tools.saveImage(ActivityNotificationDetail.this, Tools.getBytesFromFile(resource), Tools.createName(imageURL), "image/png");
                            } else {
                                Tools.saveImage(ActivityNotificationDetail.this, Tools.getBytesFromFile(resource), Tools.createName(imageURL), "image/jpg");
                            }

                            Handler handler = new Handler(Looper.getMainLooper());
                            handler.postDelayed(() -> {
                                Snackbar.make(parent_view, getString(R.string.snack_bar_saved), Snackbar.LENGTH_SHORT).show();
                                showInterstitialAdNetwork();
                            }, Constant.DELAY_SET);

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return true;
                    }
                }).submit();
    }

    public void shareWallpaper(String imageURL) {
        if (!verifyPermissions()) {
            return;
        }
        Snackbar.make(parent_view, getString(R.string.snack_bar_preparing
        ), Snackbar.LENGTH_SHORT).show();
        Glide.with(this)
                .download(imageURL.replace(" ", "%20"))
                .listener(new RequestListener<File>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<File> target, boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(File resource, Object model, Target<File> target, DataSource dataSource, boolean isFirstResource) {
                        try {
                            if (wallpaper.image_upload.endsWith(".gif") || wallpaper.image_url.endsWith(".gif")) {
                                Tools.shareImage(ActivityNotificationDetail.this, Tools.getBytesFromFile(resource), Tools.createName(imageURL), "image/gif");
                            } else if (wallpaper.image_upload.endsWith(".png") || wallpaper.image_url.endsWith(".png")) {
                                Tools.shareImage(ActivityNotificationDetail.this, Tools.getBytesFromFile(resource), Tools.createName(imageURL), "image/png");
                            } else {
                                Tools.shareImage(ActivityNotificationDetail.this, Tools.getBytesFromFile(resource), Tools.createName(imageURL), "image/jpg");
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return true;
                    }
                }).submit();
    }

    private void updateView(String image_id) {
        ApiInterface apiInterface = RestAdapter.createAPI();
        Call<Wallpaper> call = apiInterface.updateView(image_id);
        call.enqueue(new Callback<Wallpaper>() {
            @Override
            public void onResponse(Call<Wallpaper> call, Response<Wallpaper> response) {
                Log.d("UPDATE_VIEW", "success");
            }

            @Override
            public void onFailure(Call<Wallpaper> call, Throwable t) {
                Log.d("UPDATE_VIEW", "failed");
            }
        });
    }

    public void loadBannerAdNetwork() {
        if (adsPref.getAdStatus().equals(AD_STATUS_ON) && adsPref.getBannerAdStatusDetail() != 0) {
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
        bottomBanner = new BannerView(ActivityNotificationDetail.this, adsPref.getUnityBannerPlacementId(), new UnityBannerSize(UNITY_ADS_BANNER_WIDTH, UNITY_ADS_BANNER_HEIGHT));
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
            if (adsPref.getInterstitialAdDetail() != 0) {
                adMobInterstitialAd = new InterstitialAd(ActivityNotificationDetail.this);
                adMobInterstitialAd.setAdUnitId(adsPref.getAdMobInterstitialId());
                adMobInterstitialAd.loadAd(Tools.getAdRequest(ActivityNotificationDetail.this));
                adMobInterstitialAd.setAdListener(new AdListener() {
                    @Override
                    public void onAdClosed() {
                        adMobInterstitialAd.loadAd(Tools.getAdRequest(ActivityNotificationDetail.this));
                    }
                });
            }
        } else if (adsPref.getAdStatus().equals(AD_STATUS_ON) && adsPref.getAdType().equals(FAN)) {
            if (adsPref.getInterstitialAdDetail() != 0) {
                if (BuildConfig.DEBUG) {
                    fanInterstitialAd = new com.facebook.ads.InterstitialAd(ActivityNotificationDetail.this, "IMG_16_9_APP_INSTALL#" + adsPref.getFanInterstitialUnitId());
                } else {
                    fanInterstitialAd = new com.facebook.ads.InterstitialAd(ActivityNotificationDetail.this, adsPref.getFanInterstitialUnitId());
                }
                InterstitialAdListener adListener = new InterstitialAdListener() {
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
            if (adsPref.getInterstitialAdDetail() != 0) {
                startAppAd = new StartAppAd(ActivityNotificationDetail.this);
            }
        }
    }

    private void showInterstitialAdNetwork() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (adsPref.getAdStatus().equals(AD_STATUS_ON) && adsPref.getAdType().equals(ADMOB)) {
                if (adsPref.getInterstitialAdDetail() != 0) {
                    if (adMobInterstitialAd != null && adMobInterstitialAd.isLoaded()) {
                        adMobInterstitialAd.show();
                    }
                }
            } else if (adsPref.getAdStatus().equals(AD_STATUS_ON) && adsPref.getAdType().equals(FAN)) {
                if (adsPref.getInterstitialAdDetail() != 0) {
                    if (fanInterstitialAd != null && fanInterstitialAd.isAdLoaded()) {
                        fanInterstitialAd.show();
                    }
                }
            } else if (adsPref.getAdStatus().equals(AD_STATUS_ON) && adsPref.getAdType().equals(STARTAPP)) {
                if (adsPref.getInterstitialAdDetail() != 0) {
                    startAppAd.showAd();
                }
            } else if (adsPref.getAdStatus().equals(AD_STATUS_ON) && adsPref.getAdType().equals(UNITY)) {
                if (adsPref.getInterstitialAdDetail() != 0) {
                    if (UnityAds.isReady(adsPref.getUnityInterstitialPlacementId())) {
                        UnityAds.show(ActivityNotificationDetail.this, adsPref.getUnityInterstitialPlacementId());
                    }
                }
            }
        }, Constant.DELAY_ADS);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    public void fullScreenMode(boolean on) {
        LinearLayout lyt_bottom = findViewById(R.id.lyt_bottom);
        RelativeLayout bg_shadow = findViewById(R.id.bg_shadow);
        if (on) {
            toolbar.setVisibility(View.GONE);
            toolbar.animate().translationY(-112);
            lyt_bottom.setVisibility(View.GONE);
            lyt_bottom.animate().translationY(lyt_bottom.getHeight());

            bg_shadow.setVisibility(View.GONE);
            bg_shadow.animate().translationY(lyt_bottom.getHeight());

            Tools.transparentStatusBarNavigation(this);

            hideSystemUI();

        } else {
            toolbar.setVisibility(View.VISIBLE);
            toolbar.animate().translationY(0);
            lyt_bottom.setVisibility(View.VISIBLE);
            lyt_bottom.animate().translationY(0);

            bg_shadow.setVisibility(View.VISIBLE);
            bg_shadow.animate().translationY(0);

            if (adsPref.getBannerAdStatusDetail() != 0) {
                Tools.transparentStatusBar(this);
            } else {
                Tools.transparentStatusBarNavigation(this);
            }
        }
    }

    private void hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        View decorView = getWindow().getDecorView();
        //noinspection deprecation
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        // Set the content to appear under the system bars so that the
                        // content doesn't resize when the system bars hide and show.
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        // Hide the nav bar and status bar
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

}
