package com.liskovsoft.smartyoutubetv2.mobile.ui.channel;

import android.os.Bundle;

import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.ChannelPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.views.ChannelView;

public class MobileChannelActivity extends MobileVideoListActivity implements ChannelView {
    private ChannelPresenter mPresenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPresenter = ChannelPresenter.instance(this);
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
