package com.app.materialwallpaper.adapters;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.facebook.ads.AdError;
import com.facebook.ads.AdOptionsView;
import com.facebook.ads.NativeAd;
import com.facebook.ads.NativeAdLayout;
import com.facebook.ads.NativeAdListener;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.formats.MediaView;
import com.app.materialwallpaper.BuildConfig;
import com.app.materialwallpaper.Config;
import com.app.materialwallpaper.R;
import com.app.materialwallpaper.models.Wallpaper;
import com.app.materialwallpaper.utils.AdsPref;
import com.app.materialwallpaper.utils.Constant;
import com.app.materialwallpaper.utils.DBHelper;
import com.app.materialwallpaper.utils.NativeTemplateStyle;
import com.app.materialwallpaper.utils.SharedPref;
import com.app.materialwallpaper.utils.TemplateView;
import com.app.materialwallpaper.utils.Tools;
import com.startapp.sdk.ads.nativead.NativeAdDetails;
import com.startapp.sdk.ads.nativead.NativeAdPreferences;
import com.startapp.sdk.ads.nativead.StartAppNativeAd;
import com.startapp.sdk.adsbase.Ad;
import com.startapp.sdk.adsbase.adlisteners.AdEventListener;

import java.util.ArrayList;
import java.util.List;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;
import static com.app.materialwallpaper.utils.Constant.ADMOB;
import static com.app.materialwallpaper.utils.Constant.AD_STATUS_ON;
import static com.app.materialwallpaper.utils.Constant.FAN;
import static com.app.materialwallpaper.utils.Constant.STARTAPP;
import static com.app.materialwallpaper.utils.Constant.STARTAPP_IMAGE_MEDIUM;
import static com.app.materialwallpaper.utils.Constant.UNITY;

public class AdapterWallpaper extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final int VIEW_AD = 2;
    private final int VIEW_ITEM = 1;
    private final int VIEW_PROG = 0;
    private List<Wallpaper> items;
    DBHelper dbHelper;
    private boolean loading;
    private OnLoadMoreListener onLoadMoreListener;

    private Context context;
    private OnItemClickListener mOnItemClickListener;
    private Wallpaper pos;
    private CharSequence charSequence = null;
    private boolean scrolling = false;

    private StartAppNativeAd startAppNativeAd;
    private NativeAdDetails nativeAdDetails = null;

    public interface OnItemClickListener {
        void onItemClick(View view, Wallpaper obj, int position);
    }

    public void setOnItemClickListener(final OnItemClickListener mItemClickListener) {
        this.mOnItemClickListener = mItemClickListener;
    }

    public AdapterWallpaper(Context context, RecyclerView view, List<Wallpaper> items) {
        this.items = items;
        this.context = context;
        lastItemViewDetector(view);
        view.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    scrolling = true;
                } else if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    scrolling = false;
                }
                super.onScrollStateChanged(recyclerView, newState);
            }
        });
    }

    public static class OriginalViewHolder extends RecyclerView.ViewHolder {

        public ImageView wallpaper_image;
        public TextView wallpaper_name;
        public TextView category_name;
        public CardView card_view;
        LinearLayout bg_shadow;
        FrameLayout lyt_parent;

        public OriginalViewHolder(View v) {
            super(v);
            wallpaper_image = v.findViewById(R.id.wallpaper_image);
            wallpaper_name = v.findViewById(R.id.wallpaper_name);
            category_name = v.findViewById(R.id.category_name);
            card_view = v.findViewById(R.id.card_view);
            bg_shadow = v.findViewById(R.id.bg_shadow);
            lyt_parent = v.findViewById(R.id.lyt_parent);
        }

    }

    public static class NativeAdViewHolder extends RecyclerView.ViewHolder {

        public ImageView wallpaper_image;
        public TextView wallpaper_name;
        public TextView category_name;
        public CardView card_view;
        public RelativeLayout bg_view;
        LinearLayout bg_shadow;
        FrameLayout lyt_parent;

        //AdMob
        LinearLayout lyt_native_ad;
        TemplateView admob_native_template;
        CardView card_media_view;
        MediaView admob_media_view;
        TextView primary;
        TextView secondary;

        //FAN
        private NativeAd nativeAd;
        RelativeLayout lyt_fan_native;
        private NativeAdLayout nativeAdLayout;
        private LinearLayout nativeAdView;

        //StartApp
        RelativeLayout lyt_startapp_native;
        ImageView startapp_native_image;
        TextView startapp_native_title;
        TextView startapp_native_description;
        Button startapp_native_button;

        NativeAdViewHolder(View v) {
            super(v);
            wallpaper_image = v.findViewById(R.id.wallpaper_image);
            wallpaper_name = v.findViewById(R.id.wallpaper_name);
            category_name = v.findViewById(R.id.category_name);
            card_view = v.findViewById(R.id.card_view);
            bg_view = v.findViewById(R.id.bg_view);
            bg_shadow = v.findViewById(R.id.bg_shadow);
            lyt_parent = v.findViewById(R.id.lyt_parent);

            //admob native ad
            lyt_native_ad = v.findViewById(R.id.lyt_native_ad);
            admob_native_template = v.findViewById(R.id.native_template);
            card_media_view = v.findViewById(R.id.card_media_view);
            admob_media_view = v.findViewById(R.id.media_view);
            primary = v.findViewById(R.id.primary);
            secondary = v.findViewById(R.id.secondary);

            //fan native ad
            lyt_fan_native = v.findViewById(R.id.lyt_fan_native);
            nativeAdLayout = v.findViewById(R.id.native_ad_container);

            //startapp native ad
            lyt_startapp_native = v.findViewById(R.id.lyt_startapp_native);
            startapp_native_image = v.findViewById(R.id.startapp_native_image);
            startapp_native_title = v.findViewById(R.id.startapp_native_title);
            startapp_native_description = v.findViewById(R.id.startapp_native_description);
            startapp_native_button = v.findViewById(R.id.startapp_native_button);
            startapp_native_button.setOnClickListener(v1 -> itemView.performClick());
        }

    }

    public static class ProgressViewHolder extends RecyclerView.ViewHolder {
        public ProgressBar progressBar;

        ProgressViewHolder(View v) {
            super(v);
            progressBar = v.findViewById(R.id.load_more);
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder vh;
        SharedPref sharedPref = new SharedPref(context);
        if (viewType == VIEW_ITEM) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_wallpaper, parent, false);
            vh = new OriginalViewHolder(v);
        } else if (viewType == VIEW_AD) {
            if (Tools.isConnect(context)) {
                if (sharedPref.getWallpaperColumns() == 3) {
                    if (Config.ENABLE_DISPLAY_WALLPAPER_IN_SQUARE) {
                        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_native_ad_3_column_square, parent, false);
                        vh = new NativeAdViewHolder(v);
                    } else {
                        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_native_ad_3_column, parent, false);
                        vh = new NativeAdViewHolder(v);
                    }
                } else {
                    View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_native_ad_2_column, parent, false);
                    vh = new NativeAdViewHolder(v);
                }
            } else {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_wallpaper, parent, false);
                vh = new OriginalViewHolder(v);
            }
        } else {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_load_more, parent, false);
            vh = new ProgressViewHolder(v);
        }
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {
        if (holder instanceof OriginalViewHolder) {
            final Wallpaper p = items.get(position);
            final OriginalViewHolder vItem = (OriginalViewHolder) holder;

            vItem.wallpaper_name.setText(p.image_name);
            vItem.category_name.setText(p.category_name);

            if (!Config.ENABLE_DISPLAY_WALLPAPER_NAME) {
                vItem.wallpaper_name.setVisibility(View.GONE);
            }

            if (!Config.ENABLE_DISPLAY_WALLPAPER_CATEGORY) {
                vItem.category_name.setVisibility(View.GONE);
            }

            SharedPref sharedPref = new SharedPref(context);
            if (sharedPref.getIsDarkTheme()) {
                vItem.card_view.setCardBackgroundColor(context.getResources().getColor(R.color.colorToolbarDark));
            } else {
                vItem.card_view.setCardBackgroundColor(context.getResources().getColor(R.color.grey_soft));
            }

            if (!Config.ENABLE_DISPLAY_WALLPAPER_NAME && !Config.ENABLE_DISPLAY_WALLPAPER_CATEGORY) {
                vItem.bg_shadow.setBackgroundResource(R.drawable.ic_transparent);
            }

            if (p.type.equals("url")) {
                Glide.with(context)
                        .load(p.image_url.replace(" ", "%20"))
                        .transition(withCrossFade())
                        .thumbnail(0.1f)
                        .apply(new RequestOptions().override(Constant.THUMBNAIL_WIDTH, Constant.THUMBNAIL_HEIGHT))
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.ic_transparent)
                        .centerCrop()
                        .into(vItem.wallpaper_image);
            } else {
                Glide.with(context)
                        .load(Config.ADMIN_PANEL_URL + "/upload/" + items.get(position).image_upload.replace(" ", "%20"))
                        .transition(withCrossFade())
                        .thumbnail(0.1f)
                        .apply(new RequestOptions().override(Constant.THUMBNAIL_WIDTH, Constant.THUMBNAIL_HEIGHT))
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.ic_transparent)
                        .centerCrop()
                        .into(vItem.wallpaper_image);
            }

            vItem.lyt_parent.setOnClickListener(view -> {
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onItemClick(view, p, position);
                }
            });

        } else if (holder instanceof NativeAdViewHolder) {
            final Wallpaper p = items.get(position);
            final NativeAdViewHolder vItem = (NativeAdViewHolder) holder;
            AdsPref adsPref = new AdsPref(context);

            vItem.wallpaper_name.setText(p.image_name);
            vItem.category_name.setText(p.category_name);

            if (!Config.ENABLE_DISPLAY_WALLPAPER_NAME) {
                vItem.wallpaper_name.setVisibility(View.GONE);
            }

            if (!Config.ENABLE_DISPLAY_WALLPAPER_CATEGORY) {
                vItem.category_name.setVisibility(View.GONE);
            }

            SharedPref sharedPref = new SharedPref(context);
            if (sharedPref.getIsDarkTheme()) {
                vItem.card_view.setCardBackgroundColor(context.getResources().getColor(R.color.colorToolbarDark));
                vItem.bg_view.setBackgroundResource(R.drawable.bg_card_dark);
            } else {
                vItem.card_view.setCardBackgroundColor(context.getResources().getColor(R.color.grey_soft));
                vItem.bg_view.setBackgroundResource(R.drawable.bg_card_light);
            }

            if (!Config.ENABLE_DISPLAY_WALLPAPER_NAME && !Config.ENABLE_DISPLAY_WALLPAPER_CATEGORY) {
                vItem.bg_shadow.setBackgroundResource(R.drawable.ic_transparent);
            }

            if (p.type.equals("url")) {
                Glide.with(context)
                        .load(p.image_url.replace(" ", "%20"))
                        .transition(withCrossFade())
                        .thumbnail(0.1f)
                        .apply(new RequestOptions().override(Constant.THUMBNAIL_WIDTH, Constant.THUMBNAIL_HEIGHT))
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.ic_transparent)
                        .centerCrop()
                        .into(vItem.wallpaper_image);
            } else {
                Glide.with(context)
                        .load(Config.ADMIN_PANEL_URL + "/upload/" + p.image_upload.replace(" ", "%20"))
                        .transition(withCrossFade())
                        .thumbnail(0.1f)
                        .apply(new RequestOptions().override(Constant.THUMBNAIL_WIDTH, Constant.THUMBNAIL_HEIGHT))
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.ic_transparent)
                        .centerCrop()
                        .into(vItem.wallpaper_image);
            }

            if (adsPref.getAdStatus().equals(AD_STATUS_ON)) {
                if (adsPref.getNativeAdWallpaperList() != 0) {
                    switch (adsPref.getAdType()) {
                        case ADMOB:
                            AdLoader adLoader = new AdLoader.Builder(context, adsPref.getAdMobNativeId())
                                    .forUnifiedNativeAd(unifiedNativeAd -> {
                                        if (sharedPref.getIsDarkTheme()) {
                                            ColorDrawable colorDrawable = new ColorDrawable(ContextCompat.getColor(context, R.color.colorBackgroundDark));
                                            NativeTemplateStyle styles = new NativeTemplateStyle.Builder().withMainBackgroundColor(colorDrawable).build();
                                            vItem.admob_native_template.setStyles(styles);
                                            vItem.card_media_view.setCardBackgroundColor(context.getResources().getColor(R.color.colorToolbarDark));
                                        } else {
                                            ColorDrawable colorDrawable = new ColorDrawable(ContextCompat.getColor(context, R.color.colorBackgroundLight));
                                            NativeTemplateStyle styles = new NativeTemplateStyle.Builder().withMainBackgroundColor(colorDrawable).build();
                                            vItem.admob_native_template.setStyles(styles);
                                            vItem.card_media_view.setCardBackgroundColor(context.getResources().getColor(R.color.colorShimmer));
                                        }
                                        vItem.primary.setBackgroundResource(R.color.transparent);
                                        vItem.secondary.setBackgroundResource(R.color.transparent);
                                        vItem.admob_media_view.setImageScaleType(ImageView.ScaleType.CENTER_CROP);
                                        vItem.admob_native_template.setNativeAd(unifiedNativeAd);
                                    }).withAdListener(new AdListener() {
                                        @Override
                                        public void onAdLoaded() {
                                            super.onAdLoaded();
                                            vItem.lyt_native_ad.setVisibility(View.VISIBLE);
                                            Log.d("ADMOB", "native ad loaded");
                                        }

                                        @Override
                                        public void onAdFailedToLoad(LoadAdError adError) {
                                            vItem.lyt_native_ad.setVisibility(View.GONE);
                                        }
                                    })
                                    .build();
                            adLoader.loadAd(new AdRequest.Builder().build());
                            break;
                        case FAN:
                            //final ThemePref themePref = new ThemePref(context);
                            if (BuildConfig.DEBUG) {
                                vItem.nativeAd = new NativeAd(context, "IMG_16_9_APP_INSTALL#" + adsPref.getFanNativeUnitId());
                            } else {
                                vItem.nativeAd = new NativeAd(context, adsPref.getFanNativeUnitId());
                            }
                            NativeAdListener nativeAdListener = new NativeAdListener() {
                                @Override
                                public void onMediaDownloaded(com.facebook.ads.Ad ad) {

                                }

                                @Override
                                public void onError(com.facebook.ads.Ad ad, AdError adError) {

                                }

                                @Override
                                public void onAdLoaded(com.facebook.ads.Ad ad) {
                                    // Race condition, load() called again before last ad was displayed
                                    vItem.lyt_fan_native.setVisibility(View.VISIBLE);
                                    if (vItem.nativeAd == null || vItem.nativeAd != ad) {
                                        return;
                                    }
                                    // Inflate Native Ad into Container
                                    //inflateAd(nativeAd);
                                    vItem.nativeAd.unregisterView();
                                    // Add the Ad view into the ad container.
                                    LayoutInflater inflater = LayoutInflater.from(context);
                                    // Inflate the Ad view.  The layout referenced should be the one you created in the last step.

                                    vItem.nativeAdView = (LinearLayout) inflater.inflate(R.layout.gnt_fan_small_template, vItem.nativeAdLayout, false);

                                    vItem.nativeAdLayout.addView(vItem.nativeAdView);

                                    // Add the AdOptionsView
                                    LinearLayout adChoicesContainer = vItem.nativeAdView.findViewById(R.id.ad_choices_container);
                                    AdOptionsView adOptionsView = new AdOptionsView(context, vItem.nativeAd, vItem.nativeAdLayout);
                                    adChoicesContainer.removeAllViews();
                                    adChoicesContainer.addView(adOptionsView, 0);

                                    // Create native UI using the ad metadata.
                                    TextView nativeAdTitle = vItem.nativeAdView.findViewById(R.id.native_ad_title);
                                    com.facebook.ads.MediaView nativeAdMedia = vItem.nativeAdView.findViewById(R.id.native_ad_media);
                                    TextView nativeAdSocialContext = vItem.nativeAdView.findViewById(R.id.native_ad_social_context);
                                    TextView nativeAdBody = vItem.nativeAdView.findViewById(R.id.native_ad_body);
                                    TextView sponsoredLabel = vItem.nativeAdView.findViewById(R.id.native_ad_sponsored_label);
                                    Button nativeAdCallToAction = vItem.nativeAdView.findViewById(R.id.native_ad_call_to_action);
                                    LinearLayout ad_unit = vItem.nativeAdView.findViewById(R.id.ad_unit);

                                    // Set the Text.
                                    nativeAdTitle.setText(vItem.nativeAd.getAdvertiserName());
                                    nativeAdBody.setText(vItem.nativeAd.getAdBodyText());
                                    nativeAdSocialContext.setText(vItem.nativeAd.getAdSocialContext());
                                    nativeAdCallToAction.setVisibility(vItem.nativeAd.hasCallToAction() ? View.VISIBLE : View.INVISIBLE);
                                    nativeAdCallToAction.setText(vItem.nativeAd.getAdCallToAction());
                                    sponsoredLabel.setText(vItem.nativeAd.getSponsoredTranslation());

                                    // Create a list of clickable views
                                    List<View> clickableViews = new ArrayList<>();
                                    clickableViews.add(nativeAdTitle);
                                    clickableViews.add(ad_unit);
                                    clickableViews.add(nativeAdCallToAction);

                                    // Register the Title and CTA button to listen for clicks.
                                    vItem.nativeAd.registerViewForInteraction(vItem.nativeAdView, nativeAdMedia, clickableViews);

                                }

                                @Override
                                public void onAdClicked(com.facebook.ads.Ad ad) {

                                }

                                @Override
                                public void onLoggingImpression(com.facebook.ads.Ad ad) {

                                }
                            };

                            NativeAd.NativeLoadAdConfig loadAdConfig = vItem.nativeAd.buildLoadAdConfig().withAdListener(nativeAdListener).build();
                            vItem.nativeAd.loadAd(loadAdConfig);

                            break;
                        case STARTAPP:
                            startAppNativeAd = new StartAppNativeAd(context);
                            NativeAdPreferences nativePrefs = new NativeAdPreferences()
                                    .setAdsNumber(1)
                                    .setAutoBitmapDownload(true)
                                    .setPrimaryImageSize(STARTAPP_IMAGE_MEDIUM);
                            AdEventListener adListener = new AdEventListener() {
                                @Override
                                public void onReceiveAd(Ad arg0) {
                                    ArrayList<NativeAdDetails> nativeAdsList = startAppNativeAd.getNativeAds();
                                    if (nativeAdsList.size() > 0) {
                                        nativeAdDetails = nativeAdsList.get(0);
                                    }
                                    if (nativeAdDetails != null) {
                                        vItem.startapp_native_image.setImageBitmap(nativeAdDetails.getImageBitmap());
                                        vItem.startapp_native_title.setText(nativeAdDetails.getTitle());
                                        vItem.startapp_native_description.setText(nativeAdDetails.getDescription());
                                        vItem.startapp_native_button.setText(nativeAdDetails.isApp() ? "Install" : "Open");
                                        nativeAdDetails.registerViewForInteraction(vItem.itemView);
                                    }
                                    vItem.lyt_startapp_native.setVisibility(View.VISIBLE);

                                }

                                @Override
                                public void onFailedToReceiveAd(Ad arg0) {
                                    vItem.lyt_startapp_native.setVisibility(View.GONE);
                                }
                            };
                            startAppNativeAd.loadAd(nativePrefs, adListener);
                            break;
                    }
                }
            }

            vItem.lyt_parent.setOnClickListener(view -> {
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onItemClick(view, p, position);
                }
            });
        } else {
            ((ProgressViewHolder) holder).progressBar.setIndeterminate(true);
        }

        if (getItemViewType(position) == VIEW_AD) {
            StaggeredGridLayoutManager.LayoutParams layoutParams = (StaggeredGridLayoutManager.LayoutParams) holder.itemView.getLayoutParams();
            layoutParams.setFullSpan(Tools.isConnect(context));
        } else if (getItemViewType(position) == VIEW_PROG) {
            StaggeredGridLayoutManager.LayoutParams layoutParams = (StaggeredGridLayoutManager.LayoutParams) holder.itemView.getLayoutParams();
            layoutParams.setFullSpan(true);
        } else {
            StaggeredGridLayoutManager.LayoutParams layoutParams = (StaggeredGridLayoutManager.LayoutParams) holder.itemView.getLayoutParams();
            layoutParams.setFullSpan(false);
        }

    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (items.get(position) != null) {
            AdsPref adsPref = new AdsPref(context);
            SharedPref sharedPref = new SharedPref(context);
            if (adsPref.getAdStatus().equals(AD_STATUS_ON) && adsPref.getNativeAdWallpaperList() != 0) {
                if (adsPref.getAdType().equals(UNITY)) {
                    return VIEW_ITEM;
                } else if (sharedPref.getWallpaperColumns() == 3) {
                    if (position % Constant.NATIVE_AD_INTERVAL_3_COLUMNS == Constant.NATIVE_AD_INDEX_3_COLUMNS) {
                        return VIEW_AD;
                    } else {
                        return VIEW_ITEM;
                    }
                } else {
                    if (position % Constant.NATIVE_AD_INTERVAL_2_COLUMNS == Constant.NATIVE_AD_INDEX_2_COLUMNS) {
                        return VIEW_AD;
                    } else {
                        return VIEW_ITEM;
                    }
                }
            } else {
                return VIEW_ITEM;
            }
        } else {
            return VIEW_PROG;
        }
    }

    public void insertData(List<Wallpaper> items) {
        setLoaded();
        int positionStart = getItemCount();
        int itemCount = items.size();
        this.items.addAll(items);
        notifyItemRangeInserted(positionStart, itemCount);
    }

    public void setItems(List<Wallpaper> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    public void setLoaded() {
        loading = false;
        for (int i = 0; i < getItemCount(); i++) {
            if (items.get(i) == null) {
                items.remove(i);
                notifyItemRemoved(i);
            }
        }
    }

    public void setLoading() {
        if (getItemCount() != 0) {
            this.items.add(null);
            notifyItemInserted(getItemCount() - 1);
            loading = true;
        }
    }

    public void resetListData() {
        this.items.clear();
        notifyDataSetChanged();
    }

    public void setOnLoadMoreListener(OnLoadMoreListener onLoadMoreListener) {
        this.onLoadMoreListener = onLoadMoreListener;
    }

    private void lastItemViewDetector(RecyclerView recyclerView) {
        if (recyclerView.getLayoutManager() instanceof StaggeredGridLayoutManager) {
            final StaggeredGridLayoutManager layoutManager = (StaggeredGridLayoutManager) recyclerView.getLayoutManager();
            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    int lastPos = getLastVisibleItem(layoutManager.findLastVisibleItemPositions(null));
                    if (!loading && lastPos == getItemCount() - 1 && onLoadMoreListener != null) {
                        int current_page = getItemCount() / Constant.LOAD_MORE;
                        onLoadMoreListener.onLoadMore(current_page);
                        loading = true;
                    }
                }
            });
        }
    }

    public interface OnLoadMoreListener {
        void onLoadMore(int current_page);
    }

    private int getLastVisibleItem(int[] into) {
        int last_idx = into[0];
        for (int i : into) {
            if (last_idx < i) last_idx = i;
        }
        return last_idx;
    }

}