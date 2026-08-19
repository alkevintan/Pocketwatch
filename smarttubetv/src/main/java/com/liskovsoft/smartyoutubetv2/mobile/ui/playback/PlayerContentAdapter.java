package com.liskovsoft.smartyoutubetv2.mobile.ui.playback;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.liskovsoft.mediaserviceinterfaces.data.CommentItem;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.mobile.ui.browse.adapter.VideoCardCallbacks;
import com.liskovsoft.smartyoutubetv2.mobile.ui.browse.adapter.VideoCardViewHolder;
import com.liskovsoft.smartyoutubetv2.mobile.ui.dialogs.CommentsAdapter;
import com.liskovsoft.smartyoutubetv2.tv.R;

import java.util.ArrayList;
import java.util.List;

/**
 * The scrolling page under the video: metadata and actions, then comments, then suggestions.
 *
 * Everything lives in one recycler so a long comment stream is recycled properly and the whole
 * page scrolls as a unit, rather than nesting lists inside a scroll view.
 */
public class PlayerContentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    static final int TYPE_META = 0;
    static final int TYPE_HEADER = 1;
    static final int TYPE_COMMENT = 2;
    static final int TYPE_SUGGESTION = 3;

    private static final int PREFETCH_DISTANCE = 6;

    /** Supplies the meta row's content and receives its interactions. */
    public interface MetaBinder {
        void onBindMeta(View metaView);
    }

    private final MetaBinder mMetaBinder;
    private final VideoCardCallbacks mSuggestionCallbacks;
    private final CommentsAdapter.CommentCallbacks mCommentCallbacks;

    private final List<CommentItem> mComments = new ArrayList<>();
    private final List<Video> mSuggestions = new ArrayList<>();
    private String mCommentsHeader;
    private String mSuggestionsHeader;
    private boolean mCommentsVisible;

    public PlayerContentAdapter(MetaBinder metaBinder, VideoCardCallbacks suggestionCallbacks,
                                CommentsAdapter.CommentCallbacks commentCallbacks) {
        mMetaBinder = metaBinder;
        mSuggestionCallbacks = suggestionCallbacks;
        mCommentCallbacks = commentCallbacks;
    }

    // ---------------- layout math ----------------
    // Row order: [meta][comments header][comments...][suggestions header][suggestions...]

    private boolean hasCommentsSection() {
        return mCommentsVisible;
    }

    private boolean hasSuggestionsSection() {
        return !mSuggestions.isEmpty();
    }

    private int commentsHeaderPos() {
        return hasCommentsSection() ? 1 : -1;
    }

    private int commentsStart() {
        return hasCommentsSection() ? 2 : -1;
    }

    private int suggestionsHeaderPos() {
        if (!hasSuggestionsSection()) {
            return -1;
        }

        return 1 + (hasCommentsSection() ? 1 + mComments.size() : 0);
    }

    private int suggestionsStart() {
        return hasSuggestionsSection() ? suggestionsHeaderPos() + 1 : -1;
    }

    @Override
    public int getItemCount() {
        int count = 1; // meta

        if (hasCommentsSection()) {
            count += 1 + mComments.size();
        }

        if (hasSuggestionsSection()) {
            count += 1 + mSuggestions.size();
        }

        return count;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return TYPE_META;
        }

        if (position == commentsHeaderPos() || position == suggestionsHeaderPos()) {
            return TYPE_HEADER;
        }

        if (hasSuggestionsSection() && position >= suggestionsStart()) {
            return TYPE_SUGGESTION;
        }

        return TYPE_COMMENT;
    }

    // ---------------- binding ----------------

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        switch (viewType) {
            case TYPE_META:
                return new MetaHolder(inflater.inflate(R.layout.view_mobile_player_meta, parent, false));
            case TYPE_HEADER:
                return new HeaderHolder(inflater.inflate(R.layout.item_mobile_player_header, parent, false));
            case TYPE_COMMENT:
                return new CommentsAdapter.CommentViewHolder(
                        inflater.inflate(R.layout.item_mobile_comment, parent, false), mCommentCallbacks);
            default:
                return new VideoCardViewHolder(
                        inflater.inflate(R.layout.item_mobile_video, parent, false), mSuggestionCallbacks, false, false);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof MetaHolder) {
            mMetaBinder.onBindMeta(holder.itemView);
        } else if (holder instanceof HeaderHolder) {
            ((HeaderHolder) holder).bind(position == commentsHeaderPos() ? mCommentsHeader : mSuggestionsHeader);
        } else if (holder instanceof CommentsAdapter.CommentViewHolder) {
            ((CommentsAdapter.CommentViewHolder) holder).bind(mComments.get(position - commentsStart()));
        } else if (holder instanceof VideoCardViewHolder) {
            int index = position - suggestionsStart();
            ((VideoCardViewHolder) holder).bind(mSuggestions.get(index));

            if (index >= mSuggestions.size() - PREFETCH_DISTANCE && mSuggestionCallbacks != null) {
                Video last = mSuggestions.get(mSuggestions.size() - 1);
                holder.itemView.post(() -> mSuggestionCallbacks.onNearEnd(last));
            }
        }
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);

        if (holder instanceof VideoCardViewHolder) {
            ((VideoCardViewHolder) holder).unbind();
        }
    }

    // ---------------- data ----------------

    public void setSuggestions(List<Video> suggestions, String header) {
        mSuggestions.clear();

        if (suggestions != null) {
            mSuggestions.addAll(suggestions);
        }

        mSuggestionsHeader = header;
        notifyDataSetChanged();
    }

    public void clearSuggestions() {
        mSuggestions.clear();
        notifyDataSetChanged();
    }

    public List<Video> getSuggestions() {
        return mSuggestions;
    }

    public void showComments(boolean visible, String header) {
        mCommentsVisible = visible;
        mCommentsHeader = header;

        if (!visible) {
            mComments.clear();
        }

        notifyDataSetChanged();
    }

    public void setCommentsHeader(String header) {
        mCommentsHeader = header;

        if (hasCommentsSection()) {
            notifyItemChanged(commentsHeaderPos());
        }
    }

    public void clearComments() {
        mComments.clear();
        notifyDataSetChanged();
    }

    public void addComments(List<CommentItem> comments) {
        if (comments == null || comments.isEmpty()) {
            return;
        }

        // The service pads the stream with placeholder entries; they would render as blank rows.
        List<CommentItem> real = new ArrayList<>();

        for (CommentItem comment : comments) {
            if (comment != null && !comment.isEmpty()) {
                real.add(comment);
            }
        }

        if (real.isEmpty()) {
            return;
        }

        int start = commentsStart() + mComments.size();
        mComments.addAll(real);
        notifyItemRangeInserted(start, real.size());
    }

    public void syncComment(CommentItem item) {
        for (int i = 0; i < mComments.size(); i++) {
            if (TextUtils.equals(mComments.get(i).getId(), item.getId())) {
                mComments.set(i, item);
                notifyItemChanged(commentsStart() + i);
                return;
            }
        }
    }

    public int getCommentCount() {
        return mComments.size();
    }

    /** Index of the comments header, so the UI can scroll straight to the comments. */
    public int getCommentsHeaderPosition() {
        return commentsHeaderPos();
    }

    static class MetaHolder extends RecyclerView.ViewHolder {
        MetaHolder(View itemView) {
            super(itemView);
        }
    }

    static class HeaderHolder extends RecyclerView.ViewHolder {
        private final TextView mTitle;

        HeaderHolder(View itemView) {
            super(itemView);
            mTitle = itemView.findViewById(R.id.header_title);
            ViewCompat.setAccessibilityHeading(mTitle, true);
        }

        void bind(String title) {
            mTitle.setText(title);
        }
    }
}
