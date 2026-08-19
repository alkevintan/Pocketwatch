package com.liskovsoft.smartyoutubetv2.mobile.ui.playback;

import com.liskovsoft.mediaserviceinterfaces.data.CommentGroup;
import com.liskovsoft.mediaserviceinterfaces.data.CommentItem;
import com.liskovsoft.mediaserviceinterfaces.data.MediaItemMetadata;
import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.sharedutils.rx.RxHelper;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.BasePlayerController;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.AbstractCommentsReceiver;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.CommentsReceiver;

import io.reactivex.disposables.Disposable;

/**
 * Supplies the comment stream for the player screen.
 *
 * The stock CommentsController only exposes comments through a dialog. On a phone the comments
 * belong on the page itself, under the video actions, so this controller listens for metadata and
 * hands the view a receiver it can render inline.
 */
public class InlineCommentsController extends BasePlayerController {
    private static final String TAG = InlineCommentsController.class.getSimpleName();

    public interface Callback {
        /** Fired when a new video's comments become available, or with null when there are none. */
        void onCommentsReady(CommentsReceiver receiver);
    }

    private final Callback mCallback;
    private Disposable mAction;
    private String mCommentsKey;

    public InlineCommentsController(Callback callback) {
        mCallback = callback;
    }

    @Override
    public void onMetadata(MediaItemMetadata metadata) {
        mCommentsKey = metadata != null ? metadata.getCommentsKey() : null;

        // Live streams surface a chat key instead; chat stays in the dialog.
        boolean isLive = metadata != null && metadata.getLiveChatKey() != null;

        if (mCallback == null) {
            return;
        }

        mCallback.onCommentsReady(mCommentsKey != null && !isLive ? createReceiver(mCommentsKey) : null);
    }

    /** Re-emits the current video's top-level comments, e.g. after backing out of a reply thread. */
    public void onMetadataRefresh() {
        if (mCallback != null) {
            mCallback.onCommentsReady(mCommentsKey != null ? createReceiver(mCommentsKey) : null);
        }
    }

    @Override
    public void onFinish() {
        dispose();
    }

    public void dispose() {
        RxHelper.disposeActions(mAction);
        mAction = null;
    }

    private CommentsReceiver createReceiver(String commentsKey) {
        return new AbstractCommentsReceiver(getContext()) {
            @Override
            public void onStart() {
                load(this, commentsKey);
            }

            @Override
            public void onLoadMore(CommentGroup commentGroup) {
                load(this, commentGroup.getNextCommentsKey());
            }

            @Override
            public void onCommentClicked(CommentItem commentItem) {
                // Replies open as their own stream, replacing the list until the user goes back.
            }
        };
    }

    private void load(CommentsReceiver receiver, String key) {
        if (key == null) {
            receiver.addCommentGroup(null);
            return;
        }

        RxHelper.disposeActions(mAction);

        mAction = getCommentsService().getCommentsObserve(key)
                .subscribe(
                        receiver::addCommentGroup,
                        error -> {
                            Log.e(TAG, "Comments load error: %s", error.getMessage());
                            receiver.addCommentGroup(null); // clears the loading state
                        }
                );
    }

    /** Loads the replies of a comment as a standalone stream. */
    public CommentsReceiver createRepliesReceiver(CommentItem parent) {
        return new AbstractCommentsReceiver(getContext()) {
            @Override
            public void onStart() {
                load(this, parent.getNestedCommentsKey());
            }

            @Override
            public void onLoadMore(CommentGroup commentGroup) {
                load(this, commentGroup.getNextCommentsKey());
            }
        };
    }
}
