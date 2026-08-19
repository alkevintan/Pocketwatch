package com.liskovsoft.smartyoutubetv2.mobile.ui.browse.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup;
import com.liskovsoft.smartyoutubetv2.tv.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Vertical list of horizontally-scrolling shelves — the phone equivalent of a Leanback row section.
 */
public class ShelfAdapter extends RecyclerView.Adapter<ShelfAdapter.ShelfViewHolder> {
    private final List<VideoGroup> mGroups = new ArrayList<>();
    private final VideoCardCallbacks mCallbacks;
    private final RecyclerView.RecycledViewPool mSharedPool = new RecyclerView.RecycledViewPool();

    public ShelfAdapter(VideoCardCallbacks callbacks) {
        mCallbacks = callbacks;
    }

    @NonNull
    @Override
    public ShelfViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mobile_shelf, parent, false);
        return new ShelfViewHolder(view, mCallbacks, mSharedPool);
    }

    @Override
    public void onBindViewHolder(@NonNull ShelfViewHolder holder, int position) {
        holder.bind(mGroups.get(position));
    }

    @Override
    public int getItemCount() {
        return mGroups.size();
    }

    public boolean isEmpty() {
        return mGroups.isEmpty();
    }

    public void clear() {
        int size = mGroups.size();
        mGroups.clear();
        notifyItemRangeRemoved(0, size);
    }

    /** Appends a new shelf, or merges into the matching one if the presenter is paging it. */
    public void update(VideoGroup group) {
        int index = indexOf(group);

        if (index == -1) {
            mGroups.add(group);
            notifyItemInserted(mGroups.size() - 1);
        } else {
            mGroups.set(index, group);
            notifyItemChanged(index);
        }
    }

    public void remove(VideoGroup group) {
        int index = indexOf(group);

        if (index != -1) {
            mGroups.remove(index);
            notifyItemRemoved(index);
        }
    }

    public int indexOf(VideoGroup group) {
        for (int i = 0; i < mGroups.size(); i++) {
            if (mGroups.get(i).getId() == group.getId()) {
                return i;
            }
        }

        return -1;
    }

    public VideoGroup getByIndex(int index) {
        return index >= 0 && index < mGroups.size() ? mGroups.get(index) : null;
    }

    static class ShelfViewHolder extends RecyclerView.ViewHolder {
        private final TextView mTitle;
        private final RecyclerView mList;
        private final VideoCardCallbacks mCallbacks;

        ShelfViewHolder(View itemView, VideoCardCallbacks callbacks, RecyclerView.RecycledViewPool pool) {
            super(itemView);
            mCallbacks = callbacks;
            mTitle = itemView.findViewById(R.id.shelf_title);
            // Lets TalkBack users navigate shelf-by-shelf with heading gestures.
            ViewCompat.setAccessibilityHeading(mTitle, true);
            mList = itemView.findViewById(R.id.shelf_list);
            mList.setLayoutManager(new LinearLayoutManager(itemView.getContext(), LinearLayoutManager.HORIZONTAL, false));
            mList.setRecycledViewPool(pool);
            mList.setNestedScrollingEnabled(false);
        }

        void bind(VideoGroup group) {
            mTitle.setText(group.getTitle());
            // Give the strip a name so TalkBack announces which shelf the user has entered.
            mList.setContentDescription(itemView.getContext().getString(R.string.mobile_shelf_description, group.getTitle()));

            VideoCardAdapter adapter = new VideoCardAdapter(wrap(group), group.isShorts(), true);
            adapter.replace(group.getVideos());
            mList.setAdapter(adapter);
        }

        /** Reports the owning group along with the item, so paging targets the right shelf. */
        private VideoCardCallbacks wrap(VideoGroup group) {
            return new VideoCardCallbacks() {
                @Override
                public void onVideoClicked(Video video) {
                    mCallbacks.onVideoClicked(video);
                }

                @Override
                public void onVideoMenu(Video video) {
                    mCallbacks.onVideoMenu(video);
                }

                @Override
                public void onNearEnd(Video lastVisible) {
                    if (lastVisible != null) {
                        lastVisible.setGroup(group);
                        mCallbacks.onNearEnd(lastVisible);
                    }
                }
            };
        }
    }
}
