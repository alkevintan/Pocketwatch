package com.liskovsoft.smartyoutubetv2.mobile.ui.webbrowser;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.android.material.appbar.MaterialToolbar;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.WebBrowserPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.views.WebBrowserView;
import com.liskovsoft.smartyoutubetv2.mobile.ui.base.MobileActivity;
import com.liskovsoft.smartyoutubetv2.tv.R;

/** Minimal in-app browser used for links the app opens itself. */
public class MobileWebBrowserActivity extends MobileActivity implements WebBrowserView {
    private WebBrowserPresenter mPresenter;
    private WebView mWebView;
    private MaterialToolbar mToolbar;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.App_Theme_Mobile);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mobile_webbrowser);

        mToolbar = findViewById(R.id.web_toolbar);
        mToolbar.setNavigationOnClickListener(v -> finish());

        mWebView = findViewById(R.id.web_view);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setDomStorageEnabled(true);
        // Keep navigation inside the activity instead of bouncing out to an external browser.
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                mToolbar.setTitle(view.getTitle());
            }
        });

        mPresenter = WebBrowserPresenter.instance(this);
        mPresenter.setView(this);
        mPresenter.onViewInitialized();
    }

    @Override
    public void loadUrl(String url) {
        mWebView.loadUrl(url);
    }

    @Override
    public void onBackPressed() {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
            return;
        }

        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPresenter.onViewDestroyed();
    }
}
