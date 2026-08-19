package com.liskovsoft.smartyoutubetv2.mobile.ui.browse.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.tv.R;

import java.util.ArrayList;
import java.util.List;

/**
 * A flat list of video cards. Backs both the vertical grid of a TYPE_GRID section and the
 * horizontal strip inside a shelf.
 */
public class VideoCardAdapter extends RecyclerView.Adapter<VideoCardViewHolder> {
    /** Start paging when this many items remain below the fold. */
    private static final int PREFETCH_DISTANCE = 6;
    private final List<Video> mItems = new ArrayList<>();
    private final VideoCardCallbacks mCallbacks;
    private final boolean mIsPortraitCard;
    private final boolean mIsHorizontal;

    public VideoCardAdapter(VideoCardCallbacks callbacks, boolean isPortraitCard, boolean isHorizontal) {
        mCallbacks = callbacks;
        mIsPortraitCard = isPortraitCard;
        mIsHorizontal = isHorizontal;
        // No stable ids on purpose: Video.hashCode() is content-based, so the same video appearing
        // twice in a list (routine in suggestions) would collide and RecyclerView would throw.
    }

    @NonNull
    @Override
    public VideoCardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mobile_video, parent, false);
        return new VideoCardViewHolder(view, mCallbacks, mIsPortraitCard, mIsHorizontal);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoCardViewHolder holder, int position) {
        Video video = mItems.get(position);
        holder.bind(video);

        if (mCallbacks != null && position >= mItems.size() - PREFETCH_DISTANCE) {
            // Posted, not called inline: a presenter that appends synchronously would otherwise
            // notify while RecyclerView is mid-layout, which throws.
            Video last = mItems.get(mItems.size() - 1);
            holder.itemView.post(() -> mCallbacks.onNearEnd(last));
        }
    }

    @Override
    public void onViewRecycled(@NonNull VideoCardViewHolder holder) {
        super.onViewRecycled(holder);
        holder.unbind();
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public List<Video> getItems() {
        return mItems;
    }

    public void append(List<Video> videos) {
        if (videos == null || videos.isEmpty()) {
            return;
        }

        int start = mItems.size();
        mItems.addAll(videos);
        notifyItemRangeInserted(start, videos.size());
    }

    public void prepend(List<Video> videos) {
        if (videos == null || videos.isEmpty()) {
            return;
        }

        mItems.addAll(0, videos);
        notifyItemRangeInserted(0, videos.size());
    }

    public void replace(List<Video> videos) {
        mItems.clear();
        if (videos != null) {
            mItems.addAll(videos);
        }
        notifyDataSetChanged();
    }

    public void remove(List<Video> videos) {
        if (videos == null) {
            return;
        }

        for (Video video : videos) {
            int index = mItems.indexOf(video);
            if (index != -1) {
                mItems.remove(index);
                notifyItemRemoved(index);
            }
        }
    }

    /** Drops every item published by the same author. Used by the "hide this channel" action. */
    public void removeAuthor(List<Video> videos) {
        if (videos == null || videos.isEmpty()) {
            return;
        }

        for (Video source : videos) {
            String author = source.getAuthor();

            if (author == null) {
                continue;
            }

            for (int i = mItems.size() - 1; i >= 0; i--) {
                if (author.equals(mItems.get(i).getAuthor())) {
                    mItems.remove(i);
                    notifyItemRemoved(i);
                }
            }
        }
    }

    /** Refreshes in place so watch-progress and badges stay current without losing scroll. */
    public void sync(List<Video> videos) {
        if (videos == null) {
            return;
        }

        for (Video source : videos) {
            int index = mItems.indexOf(source);
            if (index != -1) {
                mItems.set(index, source);
                notifyItemChanged(index);
            }
        }
    }

    public void clear() {
        int size = mItems.size();
        mItems.clear();
        notifyItemRangeRemoved(0, size);
    }

    public int indexOf(Video video) {
        return mItems.indexOf(video);
    }

    public boolean isEmpty() {
        return mItems.isEmpty();
    }
}
