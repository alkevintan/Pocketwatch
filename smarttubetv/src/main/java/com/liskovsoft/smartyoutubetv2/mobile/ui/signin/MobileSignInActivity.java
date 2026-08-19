package com.liskovsoft.smartyoutubetv2.mobile.ui.signin;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.liskovsoft.sharedutils.helpers.MessageHelpers;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.SignInPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.views.SignInView;
import com.liskovsoft.smartyoutubetv2.mobile.ui.base.MobileActivity;
import com.liskovsoft.smartyoutubetv2.tv.R;

/**
 * Device-pairing sign-in. On a phone the code can be copied and the link opened directly,
 * which is faster than typing it into a second device the way the TV flow assumes.
 */
public class MobileSignInActivity extends MobileActivity implements SignInView {
    private SignInPresenter mPresenter;
    private TextView mCodeView;
    private TextView mInstructions;
    private MaterialButton mCopyButton;
    private MaterialButton mOpenButton;
    private String mSignInUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.App_Theme_Mobile);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mobile_code);

        ((TextView) findViewById(R.id.code_title)).setText(R.string.mobile_sign_in);
        mCodeView = findViewById(R.id.code_value);
        mInstructions = findViewById(R.id.code_instructions);
        mCopyButton = findViewById(R.id.code_copy);
        mOpenButton = findViewById(R.id.code_open);

        mPresenter = SignInPresenter.instance(this);
        mPresenter.setView(this);
        mPresenter.onViewInitialized();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPresenter.onViewDestroyed();
    }

    @Override
    public void showCode(String userCode, String signInUrl) {
        showCode(userCode, signInUrl, signInUrl);
    }

    @Override
    public void showCode(String userCode, String signInUrl, String fullSignInUrl) {
        mSignInUrl = fullSignInUrl != null ? fullSignInUrl : signInUrl;

        mCodeView.setText(userCode);
        // Read out as separate characters so TalkBack does not say "four hundred twelve".
        mCodeView.setContentDescription(spellOut(userCode));
        mInstructions.setText(getString(R.string.mobile_sign_in_instructions, signInUrl));

        mCopyButton.setVisibility(View.VISIBLE);
        mCopyButton.setOnClickListener(v -> copyCode(userCode));

        mOpenButton.setVisibility(View.VISIBLE);
        mOpenButton.setOnClickListener(v -> openLink());
    }

    private String spellOut(String code) {
        if (code == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder();

        for (char c : code.toCharArray()) {
            builder.append(c).append(' ');
        }

        return builder.toString().trim();
    }

    private void copyCode(String userCode) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText("code", userCode));
            MessageHelpers.showMessage(this, R.string.mobile_code_copied);
        }
    }

    private void openLink() {
        if (mSignInUrl == null) {
            return;
        }

        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(mSignInUrl)));
        } catch (Exception e) {
            MessageHelpers.showMessage(this, e.getMessage());
        }
    }

    @Override
    public void close() {
        finish();
    }
}
