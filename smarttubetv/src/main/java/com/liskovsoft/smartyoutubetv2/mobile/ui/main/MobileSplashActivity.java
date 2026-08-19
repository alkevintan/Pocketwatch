package com.liskovsoft.smartyoutubetv2.mobile.ui.main;

import android.content.Intent;
import android.os.Bundle;

import com.liskovsoft.smartyoutubetv2.common.app.presenters.SplashPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.views.SplashView;
import com.liskovsoft.smartyoutubetv2.mobile.ui.base.MobileActivity;

/**
 * Entry point. Does no drawing of its own — the presenter routes on to the real destination.
 */
public class MobileSplashActivity extends MobileActivity implements SplashView {
    private Intent mNewIntent;
    private SplashPresenter mPresenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mNewIntent = getIntent();

        mPresenter = SplashPresenter.instance(this);
        mPresenter.setView(this);
        mPresenter.onViewInitialized();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        mNewIntent = intent;
        mPresenter.onViewInitialized();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPresenter.onViewDestroyed();
    }

    @Override
    public Intent getNewIntent() {
        return mNewIntent;
    }

    @Override
    public void finishView() {
        try {
            finish();
        } catch (NullPointerException e) {
            // Seen on some ROMs when the display stack is torn down mid-finish.
            e.printStackTrace();
        }
    }
}
