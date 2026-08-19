package com.liskovsoft.smartyoutubetv2.mobile.ui.browse;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.BrowseSection;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.SettingsGroup;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup;
import com.liskovsoft.smartyoutubetv2.common.app.models.errors.ErrorFragmentData;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.BrowsePresenter;
import com.liskovsoft.smartyoutubetv2.mobile.ui.browse.adapter.SettingsAdapter;
import com.liskovsoft.smartyoutubetv2.mobile.ui.browse.adapter.ShelfAdapter;
import com.liskovsoft.smartyoutubetv2.mobile.ui.browse.adapter.VideoCardAdapter;
import com.liskovsoft.smartyoutubetv2.mobile.ui.browse.adapter.VideoCardCallbacks;
import com.liskovsoft.smartyoutubetv2.tv.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders one browse section. The section's type decides the layout:
 * grids become a responsive column grid, row sections become a list of horizontal shelves,
 * and settings sections become a plain vertical list.
 */
public class SectionFragment extends Fragment implements VideoCardCallbacks {
    private static final String ARG_SECTION_ID = "section_id";
    private static final String ARG_SECTION_TYPE = "section_type";
    /** Target card width in dp. The column count is whatever fits, so tablets get more columns. */
    private static final int GRID_CARD_WIDTH_DP = 180;
    private static final int SHORTS_CARD_WIDTH_DP = 130;

    private RecyclerView mList;
    private SwipeRefreshLayout mRefresh;
    private LinearLayout mEmptyView;
    private TextView mEmptyTitle;
    private TextView mEmptyMessage;
    private MaterialButton mEmptyAction;

    private VideoCardAdapter mVideoAdapter;
    private ShelfAdapter mShelfAdapter;
    private SettingsAdapter mSettingsAdapter;
    private int mSectionType = BrowseSection.TYPE_GRID;
    private int mSectionId = -1;
    /**
     * Buffers updates that arrive before the view is inflated. A row section receives one call per
     * shelf in quick succession while the fragment transaction is still pending, so this has to
     * hold all of them rather than only the latest.
     */
    private final List<VideoGroup> mPendingGroups = new ArrayList<>();
    private SettingsGroup mPendingSettingsGroup;

    public static SectionFragment create(BrowseSection section) {
        SectionFragment fragment = new SectionFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_ID, section.getId());
        args.putInt(ARG_SECTION_TYPE, section.getType());
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mSectionId = getArguments().getInt(ARG_SECTION_ID, -1);
            mSectionType = getArguments().getInt(ARG_SECTION_TYPE, BrowseSection.TYPE_GRID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_mobile_section, container, false);

        mRefresh = root.findViewById(R.id.section_refresh);
        mList = root.findViewById(R.id.section_list);
        mEmptyView = root.findViewById(R.id.section_empty);
        mEmptyTitle = root.findViewById(R.id.section_empty_title);
        mEmptyMessage = root.findViewById(R.id.section_empty_message);
        mEmptyAction = root.findViewById(R.id.section_empty_action);

        setupList();

        mRefresh.setOnRefreshListener(() -> {
            getPresenter().refresh(false);
            mRefresh.setRefreshing(false);
        });

        flushPending();

        return root;
    }

    private void setupList() {
        switch (mSectionType) {
            case BrowseSection.TYPE_ROW:
                mShelfAdapter = new ShelfAdapter(this);
                mList.setLayoutManager(new LinearLayoutManager(requireContext()));
                mList.setAdapter(mShelfAdapter);
                break;
            case BrowseSection.TYPE_SETTINGS_GRID:
                mSettingsAdapter = new SettingsAdapter();
                mList.setLayoutManager(new LinearLayoutManager(requireContext()));
                mList.setAdapter(mSettingsAdapter);
                break;
            default:
                boolean isShorts = mSectionType == BrowseSection.TYPE_SHORTS_GRID;
                mVideoAdapter = new VideoCardAdapter(this, isShorts, false);
                mList.setLayoutManager(new GridLayoutManager(requireContext(), calcSpanCount(isShorts)));
                mList.setAdapter(mVideoAdapter);
                break;
        }
    }

    /** Columns are derived from the real screen width, so phones, foldables and tablets all fit. */
    private int calcSpanCount(boolean isShorts) {
        Configuration config = getResources().getConfiguration();
        int targetDp = isShorts ? SHORTS_CARD_WIDTH_DP : GRID_CARD_WIDTH_DP;
        int widthDp = config.screenWidthDp;
        return Math.max(1, widthDp / targetDp);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Re-flow the grid when the device rotates or a foldable opens.
        if (mList != null && mList.getLayoutManager() instanceof GridLayoutManager) {
            boolean isShorts = mSectionType == BrowseSection.TYPE_SHORTS_GRID;
            ((GridLayoutManager) mList.getLayoutManager()).setSpanCount(calcSpanCount(isShorts));
        }
    }

    public int getSectionId() {
        return mSectionId;
    }

    public void update(VideoGroup group) {
        if (mList == null) {
            mPendingGroups.add(group);
            return;
        }

        if (mSectionType == BrowseSection.TYPE_ROW) {
            updateShelf(group);
        } else {
            updateGrid(group);
        }

        updateEmptyState();
    }

    public void update(SettingsGroup group) {
        if (mList == null) {
            mPendingSettingsGroup = group;
            return;
        }

        if (mSettingsAdapter != null) {
            mSettingsAdapter.replace(group.getItems());
        }

        updateEmptyState();
    }

    private void updateShelf(VideoGroup group) {
        if (mShelfAdapter == null) {
            return;
        }

        // The presenter clears a row section by sending an empty group with ACTION_REPLACE, so an
        // empty group is a reset instruction, not a shelf to draw. Adding it anyway produced a
        // phantom titled shelf and, worse, made isEmpty() report false, which suppressed the
        // "sign in" / "nothing here" state the presenter shows when a section returns no rows.
        boolean hasVideos = group.getVideos() != null && !group.getVideos().isEmpty();

        switch (group.getAction()) {
            case VideoGroup.ACTION_REPLACE:
                mShelfAdapter.clear();
                if (hasVideos) {
                    mShelfAdapter.update(group);
                }
                break;
            case VideoGroup.ACTION_REMOVE:
                mShelfAdapter.remove(group);
                break;
            default:
                if (hasVideos) {
                    mShelfAdapter.update(group);
                }
                break;
        }
    }

    private void updateGrid(VideoGroup group) {
        if (mVideoAdapter == null) {
            return;
        }

        switch (group.getAction()) {
            case VideoGroup.ACTION_REPLACE:
                mVideoAdapter.replace(group.getVideos());
                break;
            case VideoGroup.ACTION_PREPEND:
                mVideoAdapter.prepend(group.getVideos());
                break;
            case VideoGroup.ACTION_REMOVE:
                mVideoAdapter.remove(group.getVideos());
                break;
            case VideoGroup.ACTION_REMOVE_AUTHOR:
                mVideoAdapter.removeAuthor(group.getVideos());
                break;
            case VideoGroup.ACTION_SYNC:
                mVideoAdapter.sync(group.getVideos());
                break;
            default:
                mVideoAdapter.append(group.getVideos());
                break;
        }
    }

    public void clear() {
        if (mVideoAdapter != null) {
            mVideoAdapter.clear();
        }
        if (mShelfAdapter != null) {
            mShelfAdapter.clear();
        }
        if (mSettingsAdapter != null) {
            mSettingsAdapter.clear();
        }
        updateEmptyState();
    }

    public boolean isEmpty() {
        if (mVideoAdapter != null) {
            return mVideoAdapter.isEmpty();
        }
        if (mShelfAdapter != null) {
            return mShelfAdapter.isEmpty();
        }
        if (mSettingsAdapter != null) {
            return mSettingsAdapter.isEmpty();
        }
        return true;
    }

    public void selectItem(int index) {
        if (mList != null && index >= 0) {
            mList.smoothScrollToPosition(index);
        }
    }

    public void selectItem(Video video) {
        if (mList != null && mVideoAdapter != null) {
            int index = mVideoAdapter.indexOf(video);
            if (index != -1) {
                mList.smoothScrollToPosition(index);
            }
        }
    }

    public void showError(ErrorFragmentData data) {
        if (mList == null) {
            return;
        }

        clear();
        mEmptyTitle.setText(data.getMessage());
        mEmptyMessage.setText("");

        String actionText = data.getActionText();
        if (actionText != null) {
            mEmptyAction.setText(actionText);
            mEmptyAction.setVisibility(View.VISIBLE);
            mEmptyAction.setOnClickListener(v -> data.onAction());
        } else {
            mEmptyAction.setVisibility(View.GONE);
        }

        mEmptyView.setVisibility(View.VISIBLE);
        mList.setVisibility(View.GONE);
    }

    private void updateEmptyState() {
        if (mList == null) {
            return;
        }

        boolean empty = isEmpty();

        if (empty) {
            mEmptyTitle.setText(R.string.mobile_nothing_here);
            mEmptyMessage.setText("");
            mEmptyAction.setVisibility(View.GONE);
        }

        mEmptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
        mList.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void flushPending() {
        if (!mPendingGroups.isEmpty()) {
            List<VideoGroup> pending = new ArrayList<>(mPendingGroups);
            mPendingGroups.clear();

            for (VideoGroup group : pending) {
                update(group);
            }
        }

        if (mPendingSettingsGroup != null) {
            update(mPendingSettingsGroup);
            mPendingSettingsGroup = null;
        }

        updateEmptyState();
    }

    private BrowsePresenter getPresenter() {
        return BrowsePresenter.instance(requireContext());
    }

    @Override
    public void onVideoClicked(Video video) {
        getPresenter().onVideoItemClicked(video);
    }

    @Override
    public void onVideoMenu(Video video) {
        getPresenter().onVideoItemLongClicked(video);
    }

    @Override
    public void onNearEnd(Video lastVisible) {
        getPresenter().onScrollEnd(lastVisible);
    }
}
