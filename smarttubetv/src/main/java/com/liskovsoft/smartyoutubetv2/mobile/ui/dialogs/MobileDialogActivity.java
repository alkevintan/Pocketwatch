package com.liskovsoft.smartyoutubetv2.mobile.ui.dialogs;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.CommentsReceiver;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionCategory;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.AppDialogPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.views.AppDialogView;
import com.liskovsoft.smartyoutubetv2.mobile.ui.base.MobileActivity;
import com.liskovsoft.smartyoutubetv2.tv.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Settings and context menus, presented as a bottom sheet.
 *
 * Nested option screens push onto a back stack so the system Back gesture steps out one level at
 * a time, the same way the leanback preference screens behave.
 */
public class MobileDialogActivity extends MobileActivity implements AppDialogView {
    /** One level of the dialog back stack. */
    private static class DialogState {
        final List<OptionCategory> categories;
        final CharSequence title;
        final int id;

        DialogState(List<OptionCategory> categories, CharSequence title, int id) {
            this.categories = categories;
            this.title = title;
            this.id = id;
        }
    }

    private final Stack<DialogState> mBackStack = new Stack<>();
    private AppDialogPresenter mPresenter;
    private OptionsAdapter mAdapter;
    private CommentsAdapter mCommentsAdapter;
    private TextView mStatusView;
    private TextView mTitleView;
    private ImageButton mBackButton;
    private RecyclerView mList;
    private View mSheet;
    private boolean mIsTransparent;
    private boolean mIsOverlay;
    private boolean mIsPaused;
    private int mViewId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.App_Theme_Mobile_Dialog);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mobile_dialog);

        mTitleView = findViewById(R.id.dialog_title);
        mBackButton = findViewById(R.id.dialog_back);
        mList = findViewById(R.id.dialog_list);
        mStatusView = findViewById(R.id.dialog_status);
        mSheet = findViewById(R.id.dialog_sheet);

        mAdapter = new OptionsAdapter();
        mList.setLayoutManager(new LinearLayoutManager(this));
        mList.setAdapter(mAdapter);

        capSheetHeight();

        findViewById(R.id.dialog_outside).setOnClickListener(v -> finish());
        mBackButton.setOnClickListener(v -> goBack());

        mPresenter = AppDialogPresenter.instance(this);
        mPresenter.setView(this);
        mPresenter.onViewInitialized();
    }

    /** Keep the sheet off the status bar even when the option list is long. */
    private void capSheetHeight() {
        mSheet.post(() -> {
            int maxHeight = (int) (findViewById(R.id.dialog_scrim).getHeight() * 0.85f);

            if (mSheet.getHeight() > maxHeight) {
                ViewGroup.LayoutParams params = mSheet.getLayoutParams();
                params.height = maxHeight;
                mSheet.setLayoutParams(params);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsPaused = false;
        mPresenter.onViewResumed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIsPaused = true;
        mPresenter.onViewPaused();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseComments();
        mPresenter.onViewDestroyed();
    }

    @Override
    public void onBackPressed() {
        if (canGoBack()) {
            goBack();
        } else {
            super.onBackPressed();
        }
    }

    // ---------------- AppDialogView ----------------

    @Override
    public void show(List<OptionCategory> categories, CharSequence title, boolean isExpandable,
                     boolean isTransparent, boolean isOverlay, int id) {
        mIsTransparent = isTransparent;
        mIsOverlay = isOverlay;
        mViewId = id;

        mBackStack.push(new DialogState(categories != null ? categories : new ArrayList<>(), title, id));
        render();
    }

    private void render() {
        if (mBackStack.isEmpty()) {
            return;
        }

        DialogState state = mBackStack.peek();

        mBackButton.setVisibility(canGoBack() ? View.VISIBLE : View.GONE);

        // A comments/chat category is a stream, not a list of options, so it gets its own adapter.
        CommentsReceiver receiver = findCommentsReceiver(state.categories);

        // Comment dialogs are titled with the video name, which is often absent; fall back to a
        // label rather than leaving an empty header bar.
        mTitleView.setText(!TextUtils.isEmpty(state.title) ? state.title
                : receiver != null ? getString(R.string.mobile_action_comments) : "");

        if (receiver != null) {
            showComments(receiver);
        } else {
            releaseComments();
            mList.setAdapter(mAdapter);
            mAdapter.setCategories(state.categories);
        }

        mList.scrollToPosition(0);
        capSheetHeight();
    }

    private CommentsReceiver findCommentsReceiver(List<OptionCategory> categories) {
        if (categories == null) {
            return null;
        }

        for (OptionCategory category : categories) {
            if ((category.type == OptionCategory.TYPE_COMMENTS || category.type == OptionCategory.TYPE_CHAT)
                    && category.options != null && !category.options.isEmpty()) {
                return category.options.get(0).getCommentsReceiver();
            }
        }

        return null;
    }

    private void showComments(CommentsReceiver receiver) {
        releaseComments();

        mCommentsAdapter = new CommentsAdapter(receiver, this::updateCommentsEmptyState);
        mList.setAdapter(mCommentsAdapter);

        // Comments deserve the full sheet. Posted because the scrim has no measured height yet
        // during render(), and reading it early would collapse the sheet to zero.
        View scrim = findViewById(R.id.dialog_scrim);
        scrim.post(() -> {
            mSheet.getLayoutParams().height = (int) (scrim.getHeight() * 0.9f);
            mSheet.requestLayout();
        });

        mCommentsAdapter.start();
        updateCommentsEmptyState();
    }

    private void updateCommentsEmptyState() {
        if (mCommentsAdapter == null) {
            return;
        }

        boolean empty = mCommentsAdapter.isEmpty();

        mStatusView.setVisibility(empty ? View.VISIBLE : View.GONE);
        mList.setVisibility(empty ? View.GONE : View.VISIBLE);

        if (empty) {
            mStatusView.setText(mCommentsAdapter.isLoading()
                    ? R.string.mobile_loading
                    : R.string.mobile_no_comments);
        }
    }

    private void releaseComments() {
        mCommentsAdapter = null;
        mStatusView.setVisibility(View.GONE);
        mList.setVisibility(View.VISIBLE);
    }

    @Override
    public void finish() {
        super.finish();
    }

    @Override
    public void goBack() {
        if (mBackStack.size() > 1) {
            mBackStack.pop();
            render();
        } else {
            finish();
        }
    }

    @Override
    public void clearBackstack() {
        mBackStack.clear();
    }

    @Override
    public boolean canGoBack() {
        return mBackStack.size() > 1;
    }

    @Override
    public boolean isShown() {
        return !mIsPaused && !mBackStack.isEmpty();
    }

    @Override
    public boolean isTransparent() {
        return mIsTransparent;
    }

    @Override
    public boolean isOverlay() {
        return mIsOverlay;
    }

    @Override
    public boolean isPaused() {
        return mIsPaused;
    }

    @Override
    public int getViewId() {
        return mViewId;
    }
}
