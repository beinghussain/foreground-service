package com.supersami.foregroundservice;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.util.Log;


// partially took ideas from: https://github.com/zo0r/react-native-push-notification/blob/master/android/src/main/java/com/dieam/reactnativepushnotification/modules/RNPushNotificationHelper.java


class NotificationHelper {
    private static final String NOTIFICATION_CHANNEL_ID = "com.supersami.foregroundservice.channel";

    private static NotificationHelper instance = null;
    private NotificationManager mNotificationManager;

    PendingIntent pendingBtnIntent;
    private Context context;
    private NotificationConfig config;

    public static synchronized NotificationHelper getInstance(Context context) {
        if (instance == null) {
            instance = new NotificationHelper(context);
        }
        return instance;
    }

    private NotificationHelper(Context context) {
        mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.context = context;
        this.config = new NotificationConfig(context);
    }


    Notification buildNotification(Context context, Bundle bundle) {
        if (bundle == null) {
            Log.e("NotificationHelper", "buildNotification: invalid config");
            return null;
        }
        Class mainActivityClass = getMainActivityClass(context);
        if (mainActivityClass == null) {
            return null;
        }

        Intent notificationIntent = new Intent(context, mainActivityClass);
        notificationIntent.putExtra("mainOnPress", bundle.getString("mainOnPress"));
        int uniqueInt1 = (int) (System.currentTimeMillis() & 0xfffffff);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, uniqueInt1, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        if (bundle.getBoolean("button", false) == true) {
            Log.d("SuperLog C ", "inButtonOnPress" + bundle.getString("buttonOnPress"));
            Intent notificationBtnIntent = new Intent(context, mainActivityClass);
            notificationBtnIntent.putExtra("buttonOnPress", bundle.getString("buttonOnPress"));
            int uniqueInt = (int) (System.currentTimeMillis() & 0xfffffff);
            pendingBtnIntent = PendingIntent.getActivity(context, uniqueInt, notificationBtnIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        String title = bundle.getString("title");


        checkOrCreateChannel(mNotificationManager, bundle);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setContentTitle(title)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setShowWhen(false)
                .setOnlyAlertOnce(true)
                .setContentIntent(pendingIntent)
                .setCategory(NotificationCompat.CATEGORY_SYSTEM)
                .setOngoing(bundle.getBoolean("ongoing", false))
                .setContentText(bundle.getString("message"));
        if (bundle.getBoolean("button", false) == true) {
            notificationBuilder.addAction(R.drawable.redbox_top_border_background, bundle.getString("buttonText", "Button"), pendingBtnIntent);
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            notificationBuilder.setColor(this.config.getNotificationColor());
        }

        String iconName = bundle.getString("icon");

        if (iconName == null) {
            iconName = "ic_notification";
        }
        notificationBuilder.setSmallIcon(getResourceIdForResourceName(context, iconName));

        return notificationBuilder.build();
    }

    private Class getMainActivityClass(Context context) {
        String packageName = context.getPackageName();
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        if (launchIntent == null || launchIntent.getComponent() == null) {
            Log.e("NotificationHelper", "Failed to get launch intent or component");
            return null;
        }
        try {
            return Class.forName(launchIntent.getComponent().getClassName());
        } catch (ClassNotFoundException e) {
            Log.e("NotificationHelper", "Failed to get main activity class");
            return null;
        }
    }

    private int getResourceIdForResourceName(Context context, String resourceName) {
        int resourceId = context.getResources().getIdentifier(resourceName, "drawable", context.getPackageName());
        if (resourceId == 0) {
            resourceId = context.getResources().getIdentifier(resourceName, "mipmap", context.getPackageName());
        }
        return resourceId;
    }

    private static boolean channelCreated = false;

    private void checkOrCreateChannel(NotificationManager manager, Bundle bundle) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            return;
        if (channelCreated)
            return;
        if (manager == null)
            return;


        NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, this.config.getChannelName(), NotificationManager.IMPORTANCE_LOW);
        channel.setDescription(this.config.getChannelDescription());
        channel.setImportance(NotificationManager.IMPORTANCE_LOW);
        channel.enableLights(true);
        channel.enableVibration(bundle.getBoolean("vibration"));
        channel.setShowBadge(true);

        manager.createNotificationChannel(channel);
        channelCreated = true;
    }
}