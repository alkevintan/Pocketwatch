package com.liskovsoft.smartyoutubetv2.mobile.ui.adddevice;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.liskovsoft.sharedutils.helpers.MessageHelpers;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.AddDevicePresenter;
import com.liskovsoft.smartyoutubetv2.common.app.views.AddDeviceView;
import com.liskovsoft.smartyoutubetv2.mobile.ui.base.MobileActivity;
import com.liskovsoft.smartyoutubetv2.tv.R;

/** Shows the pairing code another device must enter to cast to this one. */
public class MobileAddDeviceActivity extends MobileActivity implements AddDeviceView {
    private AddDevicePresenter mPresenter;
    private TextView mCodeView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.App_Theme_Mobile);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mobile_code);

        ((TextView) findViewById(R.id.code_title)).setText(R.string.mobile_add_device);
        mCodeView = findViewById(R.id.code_value);
        ((TextView) findViewById(R.id.code_instructions)).setText(R.string.mobile_add_device_instructions);
        findViewById(R.id.code_open).setVisibility(View.GONE);

        mPresenter = AddDevicePresenter.instance(this);
        mPresenter.setView(this);
        mPresenter.onViewInitialized();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPresenter.onViewDestroyed();
    }

    @Override
    public void showCode(String userCode) {
        mCodeView.setText(userCode);

        StringBuilder spelled = new StringBuilder();
        for (char c : userCode.toCharArray()) {
            spelled.append(c).append(' ');
        }
        mCodeView.setContentDescription(spelled.toString().trim());

        MaterialButton copy = findViewById(R.id.code_copy);
        copy.setVisibility(View.VISIBLE);
        copy.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (clipboard != null) {
                clipboard.setPrimaryClip(ClipData.newPlainText("code", userCode));
                MessageHelpers.showMessage(this, R.string.mobile_code_copied);
            }
        });
    }

    @Override
    public void close() {
        finish();
    }
}
