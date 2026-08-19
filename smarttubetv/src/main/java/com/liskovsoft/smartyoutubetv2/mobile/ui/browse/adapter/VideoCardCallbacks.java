package com.liskovsoft.smartyoutubetv2.mobile.ui.browse.adapter;

import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;

/** What a card list reports back to whatever is hosting it. */
public interface VideoCardCallbacks {
    void onVideoClicked(Video video);

    void onVideoMenu(Video video);

    /** Fired as the user approaches the end of the list, so the presenter can page in more. */
    void onNearEnd(Video lastVisible);
}
