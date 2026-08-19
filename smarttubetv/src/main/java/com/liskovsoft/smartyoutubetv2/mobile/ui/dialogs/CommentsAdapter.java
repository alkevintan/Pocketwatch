package com.liskovsoft.smartyoutubetv2.mobile.ui.dialogs;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.liskovsoft.mediaserviceinterfaces.data.CommentGroup;
import com.liskovsoft.mediaserviceinterfaces.data.CommentItem;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.CommentsReceiver;
import com.liskovsoft.smartyoutubetv2.tv.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders the comment stream that {@link CommentsReceiver} feeds, paging as the user scrolls.
 */
public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.CommentViewHolder>
        implements CommentsReceiver.Callback {
    /** Interactions on a single comment row. */
    public interface CommentCallbacks {
        void onCommentClicked(CommentItem item);

        void onCommentLongClicked(CommentItem item);
    }

    private static final int PREFETCH_DISTANCE = 5;
    private final List<CommentItem> mItems = new ArrayList<>();
    private final CommentsReceiver mReceiver;
    private final Runnable mOnChanged;
    private CommentGroup mLastGroup;
    private boolean mLoading;

    public CommentsAdapter(CommentsReceiver receiver, Runnable onChanged) {
        mReceiver = receiver;
        mOnChanged = onChanged;
        mReceiver.setCallback(this);
    }

    public void start() {
        mLoading = true;
        mReceiver.onStart();
    }

    public boolean isLoading() {
        return mLoading;
    }

    // ---------------- CommentsReceiver.Callback ----------------

    @Override
    public void onCommentGroup(CommentGroup commentGroup) {
        mLoading = false;
        mLastGroup = commentGroup;

        if (commentGroup == null || commentGroup.getComments() == null) {
            if (mOnChanged != null) {
                mOnChanged.run();
            }
            return;
        }

        int start = mItems.size();
        mItems.addAll(commentGroup.getComments());
        notifyItemRangeInserted(start, commentGroup.getComments().size());

        if (mOnChanged != null) {
            mOnChanged.run();
        }
    }

    @Override
    public void onBackup(CommentsReceiver.Backup backup) {
        // Restoring a previous scroll position is not offered yet; the stream reloads instead.
    }

    @Override
    public void onSync(CommentItem commentItem) {
        int index = indexOf(commentItem);

        if (index != -1) {
            mItems.set(index, commentItem);
            notifyItemChanged(index);
        }
    }

    private int indexOf(CommentItem item) {
        for (int i = 0; i < mItems.size(); i++) {
            if (TextUtils.equals(mItems.get(i).getId(), item.getId())) {
                return i;
            }
        }

        return -1;
    }

    // ---------------- adapter ----------------

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mobile_comment, parent, false);
        return new CommentViewHolder(view, new CommentCallbacks() {
            @Override
            public void onCommentClicked(CommentItem item) {
                mReceiver.onCommentClicked(item);
            }

            @Override
            public void onCommentLongClicked(CommentItem item) {
                mReceiver.onCommentLongClicked(item);
            }
        });
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        holder.bind(mItems.get(position));

        if (position >= mItems.size() - PREFETCH_DISTANCE && mLastGroup != null
                && mLastGroup.getNextCommentsKey() != null && !mLoading) {
            mLoading = true;
            CommentGroup group = mLastGroup;
            // Posted so paging never mutates the list during a layout pass.
            holder.itemView.post(() -> mReceiver.onLoadMore(group));
        }
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public boolean isEmpty() {
        return mItems.isEmpty();
    }

    public static class CommentViewHolder extends RecyclerView.ViewHolder {
        private final ImageView mAvatar;
        private final TextView mAuthor;
        private final TextView mMessage;
        private final TextView mMeta;
        private CommentItem mItem;

        public CommentViewHolder(View itemView, CommentCallbacks callbacks) {
            super(itemView);
            mAvatar = itemView.findViewById(R.id.comment_avatar);
            mAuthor = itemView.findViewById(R.id.comment_author);
            mMessage = itemView.findViewById(R.id.comment_message);
            mMeta = itemView.findViewById(R.id.comment_meta);

            itemView.setOnClickListener(v -> {
                if (mItem != null && callbacks != null) {
                    callbacks.onCommentClicked(mItem);
                }
            });
            itemView.setOnLongClickListener(v -> {
                if (mItem != null && callbacks != null) {
                    callbacks.onCommentLongClicked(mItem);
                    return true;
                }
                return false;
            });
        }

        public void bind(CommentItem item) {
            mItem = item;
            mAuthor.setText(item.getAuthorName());
            mMessage.setText(item.getMessage());

            StringBuilder meta = new StringBuilder();
            if (!TextUtils.isEmpty(item.getPublishedDate())) {
                meta.append(item.getPublishedDate());
            }
            if (!TextUtils.isEmpty(item.getLikeCount())) {
                if (meta.length() > 0) {
                    meta.append("  ·  ");
                }
                meta.append(item.getLikeCount());
            }
            if (!TextUtils.isEmpty(item.getReplyCount())) {
                if (meta.length() > 0) {
                    meta.append("  ·  ");
                }
                meta.append(item.getReplyCount());
            }

            mMeta.setText(meta);
            mMeta.setVisibility(meta.length() == 0 ? View.GONE : View.VISIBLE);

            // One label for the whole row so TalkBack reads the comment as a unit.
            itemView.setContentDescription(item.getAuthorName() + ". " + item.getMessage() + ". " + meta);

            if (TextUtils.isEmpty(item.getAuthorPhoto())) {
                mAvatar.setImageDrawable(null);
            } else {
                Glide.with(mAvatar.getContext())
                        .load(item.getAuthorPhoto())
                        .apply(RequestOptions.circleCropTransform())
                        .into(mAvatar);
            }
        }
    }
}
