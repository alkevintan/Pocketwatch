package com.liskovsoft.smartyoutubetv2.mobile.ui.search;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ProgressBar;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.liskovsoft.sharedutils.helpers.MessageHelpers;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup;
import com.liskovsoft.smartyoutubetv2.common.app.models.search.MediaServiceSearchTagProvider;
import com.liskovsoft.smartyoutubetv2.common.app.models.search.vineyard.Tag;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.SearchPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.views.SearchView;
import com.liskovsoft.smartyoutubetv2.mobile.ui.base.MobileActivity;
import com.liskovsoft.smartyoutubetv2.mobile.ui.browse.adapter.VideoCardAdapter;
import com.liskovsoft.smartyoutubetv2.mobile.ui.browse.adapter.VideoCardCallbacks;
import com.liskovsoft.smartyoutubetv2.tv.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Search: a text field in the toolbar, live suggestion chips underneath, results in a grid.
 */
public class MobileSearchActivity extends MobileActivity implements SearchView, VideoCardCallbacks {
    private static final int VOICE_REQUEST_CODE = 4231;
    private static final int GRID_CARD_WIDTH_DP = 180;

    private SearchPresenter mPresenter;
    private EditText mInput;
    private ChipGroup mTags;
    private HorizontalScrollView mTagsScroll;
    private RecyclerView mResults;
    private ProgressBar mProgress;
    private VideoCardAdapter mAdapter;
    private MediaServiceSearchTagProvider mTagsProvider;
    /** Guards the text watcher while we set the field programmatically. */
    private boolean mSuppressTextEvents;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.App_Theme_Mobile);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mobile_search);

        mInput = findViewById(R.id.search_input);
        mTags = findViewById(R.id.search_tags);
        mTagsScroll = findViewById(R.id.search_tags_scroll);
        mResults = findViewById(R.id.search_results);
        mProgress = findViewById(R.id.search_progress);

        MaterialToolbar toolbar = findViewById(R.id.search_toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        mAdapter = new VideoCardAdapter(this, false, false);
        mResults.setLayoutManager(new GridLayoutManager(this, calcSpanCount()));
        mResults.setAdapter(mAdapter);

        setupInput();

        mPresenter = SearchPresenter.instance(this);
        mPresenter.setView(this);
        mPresenter.onViewInitialized();
    }

    private void setupInput() {
        mInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                submit(mInput.getText().toString());
                return true;
            }
            return false;
        });

        mInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                if (!mSuppressTextEvents) {
                    requestTags(s.toString());
                }
            }
        });
    }

    private int calcSpanCount() {
        return Math.max(1, getResources().getConfiguration().screenWidthDp / GRID_CARD_WIDTH_DP);
    }

    private void submit(String query) {
        if (TextUtils.isEmpty(query)) {
            return;
        }

        hideKeyboard();
        mPresenter.onSearch(query);
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

        if (imm != null) {
            imm.hideSoftInputFromWindow(mInput.getWindowToken(), 0);
        }
    }

    private void requestTags(String query) {
        if (mTagsProvider == null) {
            return;
        }

        mTagsProvider.search(query, results -> runOnUiThread(() -> showTags(results)));
    }

    private void showTags(List<Tag> tags) {
        mTags.removeAllViews();

        if (tags == null || tags.isEmpty()) {
            mTagsScroll.setVisibility(View.GONE);
            return;
        }

        for (Tag tag : tags) {
            mTags.addView(createChip(tag));
        }

        mTagsScroll.setVisibility(View.VISIBLE);
    }

    private Chip createChip(Tag tag) {
        Chip chip = new Chip(this);
        chip.setText(tag.tag);
        chip.setTag(tag);
        chip.setContentDescription(tag.tag);
        chip.setEnsureMinTouchTargetSize(true);
        chip.setOnClickListener(v -> {
            setSearchText(tag.tag);
            submit(tag.tag);
        });
        chip.setOnLongClickListener(v -> {
            mPresenter.onTagLongClicked(tag);
            return true;
        });
        return chip;
    }

    private void setSearchText(String text) {
        mSuppressTextEvents = true;
        mInput.setText(text);
        mInput.setSelection(text != null ? text.length() : 0);
        mSuppressTextEvents = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPresenter.onViewDestroyed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == VOICE_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            List<String> spoken = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

            if (spoken != null && !spoken.isEmpty()) {
                setSearchText(spoken.get(0));
                submit(spoken.get(0));
            }
        }
    }

    // ---------------- SearchView ----------------

    @Override
    public void updateSearch(VideoGroup group) {
        if (group == null) {
            return;
        }

        switch (group.getAction()) {
            case VideoGroup.ACTION_REPLACE:
                mAdapter.replace(group.getVideos());
                break;
            case VideoGroup.ACTION_REMOVE:
                mAdapter.remove(group.getVideos());
                break;
            case VideoGroup.ACTION_REMOVE_AUTHOR:
                mAdapter.removeAuthor(group.getVideos());
                break;
            case VideoGroup.ACTION_SYNC:
                mAdapter.sync(group.getVideos());
                break;
            default:
                mAdapter.append(group.getVideos());
                break;
        }
    }

    @Override
    public void clearSearch() {
        mAdapter.clear();
    }

    @Override
    public void clearSearchTags() {
        mTags.removeAllViews();
        mTagsScroll.setVisibility(View.GONE);
    }

    @Override
    public void removeSearchTag(Tag tag) {
        for (int i = 0; i < mTags.getChildCount(); i++) {
            View child = mTags.getChildAt(i);

            if (tag.equals(child.getTag())) {
                mTags.removeViewAt(i);
                break;
            }
        }

        if (mTags.getChildCount() == 0) {
            mTagsScroll.setVisibility(View.GONE);
        }
    }

    @Override
    public void setTagsProvider(MediaServiceSearchTagProvider provider) {
        mTagsProvider = provider;
    }

    @Override
    public void showProgressBar(boolean show) {
        mProgress.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void startSearch(String searchText) {
        if (searchText != null) {
            setSearchText(searchText);
            submit(searchText);
        } else {
            // No query supplied: open with the keyboard up so the user can start typing.
            mInput.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(mInput, InputMethodManager.SHOW_IMPLICIT);
            }
        }
    }

    @Override
    public String getSearchText() {
        return mInput.getText().toString();
    }

    @Override
    public void startVoiceRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        try {
            startActivityForResult(intent, VOICE_REQUEST_CODE);
        } catch (Exception e) {
            // No speech recognizer on this device.
            MessageHelpers.showMessage(this, e.getMessage());
        }
    }

    // ---------------- card callbacks ----------------

    @Override
    public void onVideoClicked(Video video) {
        mPresenter.onVideoItemClicked(video);
    }

    @Override
    public void onVideoMenu(Video video) {
        mPresenter.onVideoItemLongClicked(video);
    }

    @Override
    public void onNearEnd(Video lastVisible) {
        mPresenter.onScrollEnd(lastVisible);
    }
}
