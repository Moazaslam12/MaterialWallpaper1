package com.app.materialwallpaper;

import com.app.materialwallpaper.utils.Constant;

public class Config {

    public static final String ADMIN_PANEL_URL = "http://10.0.2.2/material_wallpaper";

    //splash duration
    public static final int SPLASH_TIME = 1000;

    //column count
    public static final int DEFAULT_WALLPAPER_COLUMN = Constant.WALLPAPER_TWO_COLUMNS;
    public static final int DEFAULT_CATEGORY_COLUMN = Constant.CATEGORY_COLUMNS;

    //UI Config
    public static final boolean ENABLE_CENTER_CROP_IN_DETAIL_WALLPAPER = true;
    public static final boolean ENABLE_DISPLAY_WALLPAPER_NAME = true;
    public static final boolean ENABLE_DISPLAY_WALLPAPER_CATEGORY = true;
    public static final boolean ENABLE_WALLPAPER_COUNT_ON_CATEGORY = true;
    public static final boolean ENABLE_DISPLAY_WALLPAPER_IN_SQUARE = false;

    //EU Consent
    public static final boolean USE_LEGACY_GDPR_EU_CONSENT = true;

    //RTL Mode
    public static final boolean ENABLE_RTL_MODE = false;

}
