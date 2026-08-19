package com.liskovsoft.smartyoutubetv2.mobile.ui.base;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.liskovsoft.sharedutils.locale.LocaleContextWrapper;
import com.liskovsoft.sharedutils.locale.LocaleUpdater;
import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.smartyoutubetv2.common.misc.ActivityCallbacks;
import com.liskovsoft.smartyoutubetv2.common.misc.ScreensaverManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Base activity for the phone/tablet UI.
 *
 * Deliberately does NOT extend MotherActivity: that class rewrites DisplayMetrics so every screen
 * reports a fixed 1920dp width, which is right for a 10-foot TV layout and ruinous for a handset.
 * Here the platform's real density is left alone so that normal dp/sp resource qualifiers work.
 */
public abstract class MobileActivity extends AppCompatActivity implements ActivityCallbacks {
    private static final String TAG = MobileActivity.class.getSimpleName();
    // Static so the callbacks survive "Don't keep activities", matching MotherActivity's behavior.
    private static List<ActivityCallbacks.OnPermissions> sOnPermissions;
    private static List<ActivityCallbacks.OnResult> sOnResults;
    private ScreensaverManager mScreensaverManager;
    /** Tracked here rather than read from the framework, which only exposes it from API 24. */
    protected static boolean sIsInPipMode;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "Starting %s...", this.getClass().getSimpleName());
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Built here rather than in onCreate: the manager inserts its dim overlay into the decor
        // view, and on AppCompat that must happen after the subclass has called setContentView,
        // otherwise it lands outside the sub-decor and covers the content.
        getScreensaverManager();
    }

    @Override
    protected void attachBaseContext(Context context) {
        Context wrapped = context;

        if (context != null) {
            // Keep the app's locale override, but pass null metrics so the device density is preserved.
            wrapped = LocaleContextWrapper.wrap(context, LocaleUpdater.getSavedLocale(context), null);
        }

        super.attachBaseContext(wrapped);
    }

    @Override
    protected void onResume() {
        super.onResume();

        LocaleUpdater.applySavedLocale(this);
        getScreensaverManager().enable();
    }

    @Override
    protected void onPause() {
        super.onPause();

        getScreensaverManager().disable();
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        sIsInPipMode = isInPictureInPictureMode;
    }

    @Override
    public ScreensaverManager getScreensaverManager() {
        // Callers such as Utils.enableScreensaver dereference this without a null check, so it is
        // created on demand if something asks before onPostCreate.
        if (mScreensaverManager == null) {
            mScreensaverManager = new ScreensaverManager(this);
            // Order matters: disable() is a no-op once blocked.
            mScreensaverManager.disable();
            mScreensaverManager.setBlocked(true);
        }

        return mScreensaverManager;
    }

    @Override
    public void finishReally() {
        try {
            super.finish();
        } catch (Exception e) {
            // TextView not attached to window manager (IllegalArgumentException)
            e.printStackTrace();
        }
    }

    @Override
    public void addOnPermissions(ActivityCallbacks.OnPermissions callback) {
        if (sOnPermissions == null) {
            sOnPermissions = new ArrayList<>();
        }

        sOnPermissions.remove(callback);
        sOnPermissions.add(callback);
    }

    @Override
    public void addOnResult(ActivityCallbacks.OnResult callback) {
        if (sOnResults == null) {
            sOnResults = new ArrayList<>();
        }

        sOnResults.remove(callback);
        sOnResults.add(callback);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (sOnPermissions != null) {
            for (ActivityCallbacks.OnPermissions callback : sOnPermissions) {
                callback.onPermissions(requestCode, permissions, grantResults);
            }
            sOnPermissions.clear();
            sOnPermissions = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (sOnResults != null) {
            for (ActivityCallbacks.OnResult callback : sOnResults) {
                callback.onResult(requestCode, resultCode, data);
            }
            sOnResults.clear();
            sOnResults = null;
        }
    }
}
