package com.liskovsoft.smartyoutubetv2.common.misc;

import android.content.Intent;

/**
 * Host activity contract the presenters rely on.
 *
 * Extracted from {@link MotherActivity} so that non-leanback (phone/tablet) activities can
 * satisfy the same contract without inheriting the TV-specific display scaling and key handling.
 */
public interface ActivityCallbacks {
    interface OnPermissions {
        void onPermissions(int requestCode, String[] permissions, int[] grantResults);
    }

    interface OnResult {
        void onResult(int requestCode, int resultCode, Intent data);
    }

    void addOnPermissions(OnPermissions callback);

    void addOnResult(OnResult callback);

    ScreensaverManager getScreensaverManager();

    /** Finish the activity, swallowing the window-manager races seen on some devices. */
    void finishReally();
}
