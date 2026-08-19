package com.liskovsoft.smartyoutubetv2.mobile.ui.channel;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup;
import com.liskovsoft.smartyoutubetv2.mobile.ui.base.MobileActivity;
import com.liskovsoft.smartyoutubetv2.mobile.ui.browse.adapter.VideoCardAdapter;
import com.liskovsoft.smartyoutubetv2.mobile.ui.browse.adapter.VideoCardCallbacks;
import com.liskovsoft.smartyoutubetv2.tv.R;

/**
 * Shared scaffolding for the screens that are "a toolbar and a grid of videos".
 */
public abstract class MobileVideoListActivity extends MobileActivity implements VideoCardCallbacks {
    private static final int GRID_CARD_WIDTH_DP = 180;

    protected VideoCardAdapter mAdapter;
    protected RecyclerView mList;
    private ProgressBar mProgress;
    private MaterialToolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.App_Theme_Mobile);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mobile_video_list);

        mToolbar = findViewById(R.id.list_toolbar);
        mList = findViewById(R.id.list_content);
        mProgress = findViewById(R.id.list_progress);

        mToolbar.setNavigationOnClickListener(v -> finish());

        mAdapter = new VideoCardAdapter(this, false, false);
        mList.setLayoutManager(new GridLayoutManager(this, calcSpanCount()));
        mList.setAdapter(mAdapter);
    }

    private int calcSpanCount() {
        return Math.max(1, getResources().getConfiguration().screenWidthDp / GRID_CARD_WIDTH_DP);
    }

    protected void setToolbarTitle(CharSequence title) {
        mToolbar.setTitle(title);
    }

    /** Applies one presenter update to the grid, honouring the group's action. */
    protected void applyUpdate(VideoGroup group) {
        if (group == null) {
            return;
        }

        if (group.getTitle() != null) {
            setToolbarTitle(group.getTitle());
        }

        switch (group.getAction()) {
            case VideoGroup.ACTION_REPLACE:
                mAdapter.replace(group.getVideos());
                break;
            case VideoGroup.ACTION_PREPEND:
                mAdapter.prepend(group.getVideos());
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

    public void showProgressBar(boolean show) {
        mProgress.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    public void clear() {
        mAdapter.clear();
    }

    public void setPosition(int index) {
        if (index >= 0) {
            mList.smoothScrollToPosition(index);
        }
    }

    @Override
    public void onVideoClicked(Video video) {
        onCardClicked(video);
    }

    @Override
    public void onVideoMenu(Video video) {
        onCardLongClicked(video);
    }

    protected abstract void onCardClicked(Video video);

    protected abstract void onCardLongClicked(Video video);
}
