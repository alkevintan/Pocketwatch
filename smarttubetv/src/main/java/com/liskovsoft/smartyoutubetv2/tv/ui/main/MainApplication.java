package com.liskovsoft.smartyoutubetv2.tv.ui.main;

import android.os.Build.VERSION;

import androidx.multidex.MultiDexApplication;

import com.liskovsoft.sharedutils.helpers.Helpers;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.BrowseSection;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.BrowsePresenter;
import com.liskovsoft.smartyoutubetv2.common.app.views.AddDeviceView;
import com.liskovsoft.smartyoutubetv2.common.app.views.AppDialogView;
import com.liskovsoft.smartyoutubetv2.common.app.views.BrowseView;
import com.liskovsoft.smartyoutubetv2.common.app.views.ChannelUploadsView;
import com.liskovsoft.smartyoutubetv2.common.app.views.ChannelView;
import com.liskovsoft.smartyoutubetv2.common.app.views.PlaybackView;
import com.liskovsoft.smartyoutubetv2.common.app.views.SearchView;
import com.liskovsoft.smartyoutubetv2.common.app.views.SignInView;
import com.liskovsoft.smartyoutubetv2.common.app.views.SplashView;
import com.liskovsoft.smartyoutubetv2.common.app.views.ViewManager;
import com.liskovsoft.smartyoutubetv2.common.app.views.WebBrowserView;
import com.liskovsoft.smartyoutubetv2.common.prefs.GeneralData;
import com.liskovsoft.smartyoutubetv2.common.prefs.NetworkData;
import com.liskovsoft.smartyoutubetv2.common.prefs.PlayerData;
import com.liskovsoft.smartyoutubetv2.common.prefs.PlayerTweaksData;
import com.liskovsoft.smartyoutubetv2.tv.ui.adddevice.AddDeviceActivity;
import com.liskovsoft.smartyoutubetv2.mobile.ui.browse.MobileBrowseActivity;
import com.liskovsoft.smartyoutubetv2.mobile.ui.dialogs.MobileDialogActivity;
import com.liskovsoft.smartyoutubetv2.mobile.ui.adddevice.MobileAddDeviceActivity;
import com.liskovsoft.smartyoutubetv2.mobile.ui.channel.MobileChannelActivity;
import com.liskovsoft.smartyoutubetv2.mobile.ui.channel.MobileChannelUploadsActivity;
import com.liskovsoft.smartyoutubetv2.mobile.ui.playback.MobilePlaybackActivity;
import com.liskovsoft.smartyoutubetv2.mobile.ui.signin.MobileSignInActivity;
import com.liskovsoft.smartyoutubetv2.mobile.ui.webbrowser.MobileWebBrowserActivity;
import com.liskovsoft.smartyoutubetv2.mobile.ui.search.MobileSearchActivity;
import com.liskovsoft.smartyoutubetv2.mobile.ui.main.MobileSplashActivity;
import com.liskovsoft.smartyoutubetv2.tv.ui.browse.BrowseActivity;
import com.liskovsoft.smartyoutubetv2.tv.ui.channel.ChannelActivity;
import com.liskovsoft.smartyoutubetv2.tv.ui.channeluploads.ChannelUploadsActivity;
import com.liskovsoft.smartyoutubetv2.tv.ui.dialogs.AppDialogActivity;
import com.liskovsoft.smartyoutubetv2.tv.ui.dialogs.AppDialogActivityOpaque;
import com.liskovsoft.smartyoutubetv2.tv.ui.playback.PlaybackActivity;
import com.liskovsoft.smartyoutubetv2.tv.ui.search.tags.SearchTagsActivity;
import com.liskovsoft.smartyoutubetv2.tv.ui.signin.SignInActivity;
import com.liskovsoft.smartyoutubetv2.tv.ui.webbrowser.WebBrowserActivity;

import org.conscrypt.Conscrypt;

import java.lang.Thread.UncaughtExceptionHandler;
import java.security.Provider;
import java.security.Security;

public class MainApplication extends MultiDexApplication { // fix: Didn't find class "com.google.firebase.provider.FirebaseInitProvider"
    static {
        // fix youtube bandwidth throttling (best - false)???
        // false is better for streams (less buffering)
        System.setProperty("http.keepAlive", "false");
        // fix ipv6 infinite video buffering???
        // Better to remove this fix at all. Users complain about infinite loading.
        //System.setProperty("java.net.preferIPv6Addresses", "true");
        // Another IPv6 fix (no effect)
        // https://stackoverflow.com/questions/1920623/sometimes-httpurlconnection-getinputstream-executes-too-slowly
        //System.setProperty("java.net.preferIPv4Stack" , "true");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // ByeByeDPI fix
        // https://android-review.googlesource.com/c/platform/external/conscrypt/+/89408/
        // NOTE: Android 10+ (API 29+) uses system Conscrypt TLS; custom Security providers are unnecessary
        // NOTE: May cause 'Unexpected playback error null'
        //if (Build.VERSION.SDK_INT < 29 && Conscrypt.isAvailable()) {
        //    Security.insertProviderAt(Conscrypt.newProvider(), 1);
        //}

        // Important: Initialize the native Conscrypt provider BEFORE reading any configs/SharedPreferences.
        // Otherwise, early disk I/O shifts the ClassLoader on some Android TV devices, causing silent JNI linking errors.
        Provider conscryptProvider = null;
        try {
            conscryptProvider = Conscrypt.newProvider();
        } catch (Throwable e) {
            // UnsatisfiedLinkError
        }

        if (conscryptProvider != null && NetworkData.instance(this).isConscryptEnabled()) {
            try {
                Security.insertProviderAt(conscryptProvider, 1);
            } catch (Throwable e) {
                // UnsatisfiedLinkError
            }
        }

        setupGlobalExceptionHandler();
        setupViewManager();
    }

    private void setupViewManager() {
        ViewManager viewManager = ViewManager.instance(this);

        // Phone/tablet UI. Every view interface maps to a touch-first activity; the leanback
        // activities remain in the tree but are no longer reachable through the ViewManager.
        viewManager.setRoot(MobileBrowseActivity.class);
        viewManager.register(SplashView.class, MobileSplashActivity.class); // no parent, because it's root activity
        viewManager.register(BrowseView.class, MobileBrowseActivity.class); // no parent, because it's root activity
        viewManager.register(PlaybackView.class, MobilePlaybackActivity.class, MobileBrowseActivity.class);
        viewManager.register(AppDialogView.class, MobileDialogActivity.class, MobileBrowseActivity.class);
        viewManager.register(SearchView.class, MobileSearchActivity.class, MobileBrowseActivity.class);
        viewManager.register(SignInView.class, MobileSignInActivity.class, MobileBrowseActivity.class);
        viewManager.register(AddDeviceView.class, MobileAddDeviceActivity.class, MobileBrowseActivity.class);
        viewManager.register(ChannelView.class, MobileChannelActivity.class, MobileBrowseActivity.class);
        viewManager.register(ChannelUploadsView.class, MobileChannelUploadsActivity.class, MobileBrowseActivity.class);
        viewManager.register(WebBrowserView.class, MobileWebBrowserActivity.class, MobileBrowseActivity.class);
    }

    private void setupGlobalExceptionHandler() {
        UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();

        if (defaultHandler == null) {
            return;
        }

        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            if (shouldIgnore(e)) {
                return;
            }

            applyCrashFixes(e);
            //e = wrapWithAdditionalInfo(e);

            defaultHandler.uncaughtException(t, e);
        });
    }

    private boolean shouldIgnore(Throwable e) {
        if (Helpers.containsAny(e.getMessage(), "KatnissVoiceInteractionService", "ListenableFuture", "Missing android.support.FILE_PROVIDER_PATHS meta-data")
                || e.getClass().getName().startsWith("org.chromium")) {
            // IllegalStateException: Not allowed to start service Intent { act=android.service.voice.VoiceInteractionService
            // cmp=com.google.android.katniss/.search.serviceapi.KatnissVoiceInteractionService (has extras) }:
            // app is in background uid UidRecord{40e7240 u0a19 CEM idle change:cached procs:1 seq(0,0,0)}

            // java.lang.NoSuchMethodError: No interface method addListener(Ljava/lang/Runnable;Ljava/util/concurrent/Executor;)V
            // in class Lcom/google/common/util/concurrent/ListenableFuture; or its super classes
            // (declaration of 'com.google.common.util.concurrent.ListenableFuture' appears in /system/framework/libsetting.jar)

            // Fatal Exception: org.chromium.base.JniAndroid$UncaughtExceptionException
            // 1) Caused by java.lang.InterruptedException
            // 2) Caused by java.lang.SecurityException: The calling process has already registered an InputDevicesChangedListener.

            // IllegalArgumentException: Missing android.support.FILE_PROVIDER_PATHS meta-data (Shield Android TV 95%, MiTV-AFKR0 5%)
            return true;
        }

        return false;
    }

    private Throwable wrapWithAdditionalInfo(Throwable e) {
        if (Helpers.equalsAny(e.getMessage(),
                "parameter must be a descendant of this view",
                "Attempt to invoke virtual method 'android.view.ViewGroup$LayoutParams android.view.View.getLayoutParams()' on a null object reference")) {
            Class<?> view = ViewManager.instance(getApplicationContext()).getTopView();
            BrowseSection section = null;

            if (view == BrowseView.class) {
                section = BrowsePresenter.instance(getApplicationContext()).getCurrentSection();
            }

            e = new RuntimeException("A crash in the view " + view.getSimpleName() + ", section id " + (section != null ? section.getId() : "-1"), e);
        }
        return e;
    }

    private void applyCrashFixes(Throwable e) {
        if (e instanceof OutOfMemoryError || e.getCause() instanceof OutOfMemoryError) {
            Class<?> view = ViewManager.instance(getApplicationContext()).getTopView();
            if (view == PlaybackView.class) {
                PlayerTweaksData tweaksData = PlayerTweaksData.instance(getApplicationContext());
                PlayerData playerData = PlayerData.instance(getApplicationContext());
                int playerDataSource = tweaksData.getPlayerDataSource();
                int videoBufferType = playerData.getVideoBufferType();
                if (playerDataSource == PlayerTweaksData.PLAYER_DATA_SOURCE_OKHTTP) {
                    tweaksData.setPlayerDataSource(PlayerTweaksData.PLAYER_DATA_SOURCE_DEFAULT);
                    tweaksData.persistNow();
                } else if (videoBufferType == PlayerData.BUFFER_HIGH || videoBufferType == PlayerData.BUFFER_HIGHEST) {
                    playerData.setVideoBufferType(PlayerData.BUFFER_MEDIUM);
                    playerData.persistNow();
                }
            }
        }
    }
}
