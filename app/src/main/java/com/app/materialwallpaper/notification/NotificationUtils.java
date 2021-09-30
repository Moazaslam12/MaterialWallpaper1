package com.app.materialwallpaper.notification;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import com.app.materialwallpaper.activities.ActivityNotificationDetail;

import java.util.List;

public class NotificationUtils {

    private Context context;

    public NotificationUtils(Context context) {
        this.context = context;
    }

    public static boolean isAppIsInBackground(Context context) {
        boolean isInBackground = true;
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT_WATCH) {
            List<ActivityManager.RunningAppProcessInfo> runningProcesses = am.getRunningAppProcesses();
            for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
                if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    for (String activeProcess : processInfo.pkgList) {
                        if (activeProcess.equals(context.getPackageName())) {
                            isInBackground = false;
                        }
                    }
                }
            }
        } else {
            List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);
            ComponentName componentInfo = taskInfo.get(0).topActivity;
            if (componentInfo.getPackageName().equals(context.getPackageName())) {
                isInBackground = false;
            }
        }

        return isInBackground;
    }

    public void playNotificationSound() {
        try {
            Uri sound = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.getPackageName() + "");
            Ringtone r = RingtoneManager.getRingtone(context, sound);
            r.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Clears notification tray messages
    public static void clearNotifications(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }

    public static void oneSignalNotificationHandler(Activity activity, Intent intent) {

        if (intent.hasExtra("nid")) {

            String nid = intent.getStringExtra("nid");
            String url = intent.getStringExtra("external_link");

            if (nid.equals("0")) {
                if (url.equals("") || url.equals("no_url")) {
                    Log.d("OneSignal", "do nothing");
                } else {
                    Intent b = new Intent(Intent.ACTION_VIEW);
                    b.setData(Uri.parse(url));
                    activity.startActivity(b);
                }
            } else {
                Intent act2 = new Intent(activity, ActivityNotificationDetail.class);
                act2.putExtra("id", nid);
                activity.startActivity(act2);
            }

        }

    }

}
