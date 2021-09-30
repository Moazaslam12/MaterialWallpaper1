package com.app.materialwallpaper.models;

import java.io.Serializable;

public class AdStatus implements Serializable {

    public int banner_ad_on_home_page;
    public int banner_ad_on_search_page;
    public int banner_ad_on_wallpaper_detail;
    public int banner_ad_on_wallpaper_by_category;
    public int interstitial_ad_on_click_wallpaper;
    public int interstitial_ad_on_wallpaper_detail;
    public int native_ad_on_wallpaper_list;
    public int native_ad_on_exit_dialog;
    public String last_update_ads_status;

}
