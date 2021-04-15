package com.dieam.reactnativepushnotification.modules;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.facebook.react.ReactApplication;
import com.facebook.react.ReactInstanceManager;
import com.facebook.react.bridge.ReactContext;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.SecureRandom;
import java.util.Set;

import static com.dieam.reactnativepushnotification.modules.RNPushNotification.LOG_TAG;

/**
 * Handles tracking tapped and dismissed for notifications (ie: cases where the app is not invoked)
 */
public class RNPushNotificationInteractionTracker extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        final Bundle bundle = intent.getExtras();
        handleInteractionReporting(context, bundle);

        final Bundle notificationBundle = bundle.getBundle("notification");

        if (notificationBundle != null) {
            final boolean isDismissed = notificationBundle.getBoolean("dismissed", false);
            if (!isDismissed) {
                // TODO: Carry props into here eventually...
                final PackageManager pm = context.getPackageManager();
                final Intent launchIntent = pm.getLaunchIntentForPackage("com.wellthapp.reactnative");
                context.startActivity(launchIntent);
            }
        }
    }

    private void handleInteractionReporting(final Context context, final Bundle bundle) {

        if (bundle.getString("id") == null) {
            SecureRandom randomNumberGenerator = new SecureRandom();
            bundle.putString("id", String.valueOf(randomNumberGenerator.nextInt()));
        }

        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            public void run() {
                // Construct and load our normal React JS code bundle
                final ReactInstanceManager mReactInstanceManager = ((ReactApplication) context.getApplicationContext()).getReactNativeHost().getReactInstanceManager();
                ReactContext context = mReactInstanceManager.getCurrentReactContext();

                // If it's constructed, send a notification
                if (context != null) {
                    RNPushNotificationJsDelivery mJsDelivery = new RNPushNotificationJsDelivery(context);
                    mJsDelivery.notifyInteraction(bundle);
                } else {
                    // Otherwise wait for construction, then send the notification
                    mReactInstanceManager.addReactInstanceEventListener(new ReactInstanceManager.ReactInstanceEventListener() {
                        public void onReactContextInitialized(ReactContext context) {
                            RNPushNotificationJsDelivery mJsDelivery = new RNPushNotificationJsDelivery(context);
                            mJsDelivery.notifyInteraction(bundle);
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