package com.dieam.reactnativepushnotification.modules;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.facebook.react.ReactApplication;
import com.facebook.react.ReactInstanceManager;
import com.facebook.react.bridge.ReactContext;
import java.security.SecureRandom;

import static com.dieam.reactnativepushnotification.modules.RNPushNotification.LOG_TAG;

/**
 * Handles logging and publishing LOCAL push notifications
 */
public class RNPushNotificationPublisher extends BroadcastReceiver {
    final static String NOTIFICATION_ID = "notificationId";

    @Override
    public void onReceive(final Context context, final Intent intent) {
        final Bundle bundle = intent.getExtras();
        handleLocalNotification(context, bundle);
    }

    private void handleLocalNotification(final Context context, final Bundle bundle) {
        // Generate random notification ID if one isn't provided in the bundle
        if (bundle.getString("id") == null) {
            SecureRandom randomNumberGenerator = new SecureRandom();
            bundle.putString("id", String.valueOf(randomNumberGenerator.nextInt()));
        }

        // Instantiate the push notification helper to display the message
        final Application applicationContext = (Application) context.getApplicationContext();
        final RNPushNotificationHelper pushNotificationHelper = new RNPushNotificationHelper(applicationContext);

        bundle.putBoolean("foreground", pushNotificationHelper.isApplicationInForeground());
        bundle.putBoolean("background", pushNotificationHelper.isApplicationInBackground());


        // Report the notification to the Javascript layer
        report(context, bundle);

        // Send the notification to the notification center
        pushNotificationHelper.sendToNotificationCentre(bundle);
    }

    private void report(final Context context, final Bundle bundle) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            public void run() {
                final ReactInstanceManager mReactInstanceManager = ((ReactApplication) context.getApplicationContext()).getReactNativeHost().getReactInstanceManager();
                ReactContext context = mReactInstanceManager.getCurrentReactContext();

                if (context != null) {
                    RNPushNotificationJsDelivery mJsDelivery = new RNPushNotificationJsDelivery(context);
                    bundle.putBoolean("userInteraction", false);
                    mJsDelivery.notifyNotification(bundle);
                } else {
                    mReactInstanceManager.addReactInstanceEventListener(new ReactInstanceManager.ReactInstanceEventListener() {
                        public void onReactContextInitialized(ReactContext context) {
                            RNPushNotificationJsDelivery mJsDelivery = new RNPushNotificationJsDelivery(context);
                            bundle.putBoolean("userInteraction", false);
                            mJsDelivery.notifyNotification(bundle);
                            mReactInstanceManager.removeReactInstanceEventListener(this);
                        }
                    });

                    if (!mReactInstanceManager.hasStartedCreatingInitialContext()) {
                        // Construct it in the background
                        mReactInstanceManager.createReactContextInBackground();
                    }
                }
            }
        });
    }
}