package com.liskovsoft.smartyoutubetv2.mobile.ui.channel;

import android.os.Bundle;

import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.ChannelUploadsPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.views.ChannelUploadsView;

public class MobileChannelUploadsActivity extends MobileVideoListActivity implements ChannelUploadsView {
    private ChannelUploadsPresenter mPresenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPresenter = ChannelUploadsPresenter.instance(this);
        mPresenter.setView(this);
        mPresenter.onViewInitialized();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPresenter.onViewDestroyed();
    }

    @Override
    public void update(VideoGroup videoGroup) {
        applyUpdate(videoGroup);
    }

    @Override
    protected void onCardClicked(Video video) {
        mPresenter.onVideoItemClicked(video);
    }

    @Override
    protected void onCardLongClicked(Video video) {
        mPresenter.onVideoItemLongClicked(video);
    }

    @Override
    public void onNearEnd(Video lastVisible) {
        mPresenter.onScrollEnd(lastVisible);
    }
}
