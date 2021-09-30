package com.app.materialwallpaper.activities;

import android.annotation.TargetApi;
import android.app.WallpaperManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.InterstitialAdListener;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.material.snackbar.Snackbar;
import com.app.materialwallpaper.BuildConfig;
import com.app.materialwallpaper.R;
import com.app.materialwallpaper.utils.AdsPref;
import com.app.materialwallpaper.utils.Constant;
import com.app.materialwallpaper.utils.Tools;
import com.startapp.sdk.adsbase.StartAppAd;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;
import com.unity3d.ads.UnityAds;

import java.io.IOException;

import static com.app.materialwallpaper.utils.Constant.ADMOB;
import static com.app.materialwallpaper.utils.Constant.AD_STATUS_ON;
import static com.app.materialwallpaper.utils.Constant.FAN;
import static com.app.materialwallpaper.utils.Constant.STARTAPP;
import static com.app.materialwallpaper.utils.Constant.UNITY;

public class ActivityCropWallpaper extends AppCompatActivity {

    String image_url;
    Bitmap bitmap = null;
    CropImageView cropImageView;
    private String single_choice_selected;
    CoordinatorLayout parent_view;
    private InterstitialAd adMobInterstitialAd;
    private com.facebook.ads.InterstitialAd fanInterstitialAd;
    private StartAppAd startAppAd;
    AdsPref adsPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tools.getTheme(this);
        Tools.transparentStatusBarNavigation(ActivityCropWallpaper.this);
        setContentView(R.layout.activity_set_wallpaper);
        Tools.getRtlDirection(this);
        adsPref = new AdsPref(this);

        Intent intent = getIntent();
        image_url = intent.getStringExtra("image_url");

        cropImageView = findViewById(R.id.cropImageView);
        parent_view = findViewById(R.id.coordinatorLayout);

        loadWallpaper();
        loadInterstitialAdNetwork();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return true;
    }

    @TargetApi(Build.VERSION_CODES.N)
    public void loadWallpaper() {
        Glide.with(this)
                .load(image_url.replace(" ", "%20"))
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        bitmap = ((BitmapDrawable) resource).getBitmap();
                        cropImageView.setImageBitmap(bitmap);

                        findViewById(R.id.btn_set_wallpaper).setOnClickListener(view -> dialogOptionSetWallpaper());
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
    }

    @TargetApi(Build.VERSION_CODES.N)
    public void dialogOptionSetWallpaper() {
        String[] items = getResources().getStringArray(R.array.dialog_set_crop_wallpaper);
        single_choice_selected = items[0];
        int itemSelected = 0;
        bitmap = cropImageView.getCroppedImage();
        new AlertDialog.Builder(ActivityCropWallpaper.this)
                .setTitle(R.string.dialog_set_title)
                .setSingleChoiceItems(items, itemSelected, (dialogInterface, i) -> single_choice_selected = items[i])
                .setPositiveButton(R.string.dialog_option_ok, (dialogInterface, i) -> {
                    Snackbar.make(parent_view, getString(R.string.snack_bar_applying), Snackbar.LENGTH_SHORT).show();
                    if (single_choice_selected.equals(getResources().getString(R.string.set_home_screen))) {
                        try {
                            WallpaperManager wallpaperManager = WallpaperManager.getInstance(ActivityCropWallpaper.this);
                            wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM);
                            showSuccessSnackBar();
                        } catch (IOException e) {
                            e.printStackTrace();
                            Snackbar.make(parent_view, getString(R.string.snack_bar_failed), Snackbar.LENGTH_SHORT).show();
                        }
                    } else if (single_choice_selected.equals(getResources().getString(R.string.set_lock_screen))) {
                        try {
                            WallpaperManager wallpaperManager = WallpaperManager.getInstance(ActivityCropWallpaper.this);
                            wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK);
                            showSuccessSnackBar();
                        } catch (IOException e) {
                            e.printStackTrace();
                            Snackbar.make(parent_view, getString(R.string.snack_bar_failed), Snackbar.LENGTH_SHORT).show();
                        }
                    } else if (single_choice_selected.equals(getResources().getString(R.string.set_both))) {
                        try {
                            WallpaperManager wallpaperManager = WallpaperManager.getInstance(ActivityCropWallpaper.this);
                            wallpaperManager.setBitmap(bitmap);
                            showSuccessSnackBar();
                        } catch (IOException e) {
                            e.printStackTrace();
                            Snackbar.make(parent_view, getString(R.string.snack_bar_failed), Snackbar.LENGTH_SHORT).show();
                        }
                    }

                })
                .setNegativeButton(R.string.dialog_option_cancel, null)
                .show();
    }

    public void showSuccessSnackBar() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> Snackbar
                .make(parent_view, getString(R.string.snack_bar_applied), Snackbar.LENGTH_SHORT)
                .addCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar snackbar, int event) {
                        finish();
                        showInterstitialAdNetwork();
                    }

                    @Override
                    public void onShown(Snackbar snackbar) {
                    }
                })
                .show(), Constant.DELAY_SET);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }

    private void loadInterstitialAdNetwork() {
        if (adsPref.getAdStatus().equals(AD_STATUS_ON) && adsPref.getAdType().equals(ADMOB)) {
            if (adsPref.getInterstitialAdDetail() != 0) {
                adMobInterstitialAd = new InterstitialAd(ActivityCropWallpaper.this);
                adMobInterstitialAd.setAdUnitId(adsPref.getAdMobInterstitialId());
                adMobInterstitialAd.loadAd(Tools.getAdRequest(ActivityCropWallpaper.this));
                adMobInterstitialAd.setAdListener(new AdListener() {
                    @Override
                    public void onAdClosed() {
                        adMobInterstitialAd.loadAd(Tools.getAdRequest(ActivityCropWallpaper.this));
                    }
                });
            }
        } else if (adsPref.getAdStatus().equals(AD_STATUS_ON) && adsPref.getAdType().equals(FAN)) {
            if (adsPref.getInterstitialAdDetail() != 0) {
                if (BuildConfig.DEBUG) {
                    fanInterstitialAd = new com.facebook.ads.InterstitialAd(ActivityCropWallpaper.this, "IMG_16_9_APP_INSTALL#" + adsPref.getFanInterstitialUnitId());
                } else {
                    fanInterstitialAd = new com.facebook.ads.InterstitialAd(ActivityCropWallpaper.this, adsPref.getFanInterstitialUnitId());
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
            if (adsPref.getInterstitialAdDetail() != 0) {
                startAppAd = new StartAppAd(ActivityCropWallpaper.this);
            }
        }
    }

    private void showInterstitialAdNetwork() {
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
            if (UnityAds.isReady(adsPref.getUnityInterstitialPlacementId())) {
                UnityAds.show(ActivityCropWallpaper.this, adsPref.getUnityInterstitialPlacementId());
            }

        }
    }

}
