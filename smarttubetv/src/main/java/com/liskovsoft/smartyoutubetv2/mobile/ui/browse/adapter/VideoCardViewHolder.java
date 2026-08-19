package com.liskovsoft.smartyoutubetv2.mobile.ui.browse.adapter;

import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import android.text.TextUtils;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.tv.R;

public class VideoCardViewHolder extends RecyclerView.ViewHolder {
    /** Width of a card in a horizontal shelf, in dp. Grid cards size themselves to their column. */
    private static final int SHELF_CARD_WIDTH_DP = 210;
    private static final float ASPECT_LANDSCAPE = 16f / 9f;
    private static final float ASPECT_PORTRAIT = 9f / 16f;
    private final ImageView mThumb;
    private final TextView mTitle;
    private final TextView mSubtitle;
    private final TextView mBadge;
    private final ProgressBar mProgress;
    private final ImageButton mMenu;
    private final boolean mIsPortraitCard;
    private Video mVideo;

    public VideoCardViewHolder(View itemView, VideoCardCallbacks callbacks, boolean isPortraitCard, boolean isHorizontal) {
        super(itemView);

        mIsPortraitCard = isPortraitCard;
        mThumb = itemView.findViewById(R.id.card_thumb);
        mTitle = itemView.findViewById(R.id.card_title);
        mSubtitle = itemView.findViewById(R.id.card_subtitle);
        mBadge = itemView.findViewById(R.id.card_badge);
        mProgress = itemView.findViewById(R.id.card_progress);
        mMenu = itemView.findViewById(R.id.card_menu);

        if (isHorizontal) {
            ViewGroup.LayoutParams params = itemView.getLayoutParams();
            params.width = dpToPx(itemView.getResources(), SHELF_CARD_WIDTH_DP);
            itemView.setLayoutParams(params);
        }

        itemView.setOnClickListener(v -> {
            if (mVideo != null && callbacks != null) {
                callbacks.onVideoClicked(mVideo);
            }
        });

        // Long-press opens the same context menu as the overflow button. Both are offered because
        // long-press alone is not discoverable and is awkward to trigger with TalkBack.
        itemView.setOnLongClickListener(v -> {
            if (mVideo != null && callbacks != null) {
                callbacks.onVideoMenu(mVideo);
                return true;
            }
            return false;
        });

        mMenu.setOnClickListener(v -> {
            if (mVideo != null && callbacks != null) {
                callbacks.onVideoMenu(mVideo);
            }
        });
    }

    public void bind(Video video) {
        mVideo = video;

        mTitle.setText(video.getTitle());

        CharSequence subtitle = video.getSecondTitle();
        mSubtitle.setText(subtitle);
        mSubtitle.setVisibility(TextUtils.isEmpty(subtitle) ? View.GONE : View.VISIBLE);

        boolean hasBadge = !TextUtils.isEmpty(video.badge);
        mBadge.setText(video.badge);
        mBadge.setVisibility(hasBadge ? View.VISIBLE : View.GONE);

        if (video.percentWatched > 0 && video.percentWatched < 100) {
            mProgress.setProgress((int) video.percentWatched);
            mProgress.setVisibility(View.VISIBLE);
        } else {
            mProgress.setVisibility(View.GONE);
        }

        applyAspectRatio();
        loadThumb(video);
        applyAccessibilityLabel(video);
    }

    public void unbind() {
        mVideo = null;
        Glide.with(mThumb.getContext()).clear(mThumb);
    }

    /**
     * The thumbnail is decorative; the card as a whole carries one label so TalkBack announces the
     * video once instead of reading four sibling views in sequence.
     */
    private void applyAccessibilityLabel(Video video) {
        String duration = video.isLive
                ? itemView.getContext().getString(R.string.mobile_live_now)
                : TextUtils.isEmpty(video.badge) ? "" : video.badge;
        String author = video.getAuthor() != null ? video.getAuthor() : "";

        itemView.setContentDescription(
                itemView.getContext().getString(R.string.mobile_card_description, video.getTitle(), author, duration));

        // The children are covered by the parent's label, so keep them out of the traversal order.
        mTitle.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        mSubtitle.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        mBadge.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    private void applyAspectRatio() {
        mThumb.post(() -> {
            int width = mThumb.getWidth();
            if (width <= 0) {
                return;
            }

            int height = (int) (mIsPortraitCard ? width / ASPECT_PORTRAIT : width / ASPECT_LANDSCAPE);
            ViewGroup.LayoutParams params = mThumb.getLayoutParams();

            if (params.height != height) {
                params.height = height;
                mThumb.setLayoutParams(params);
            }
        });
    }

    private void loadThumb(Video video) {
        String url = video.getCardImageUrl();

        if (TextUtils.isEmpty(url)) {
            mThumb.setImageDrawable(null);
            return;
        }

        Glide.with(mThumb.getContext())
                .load(url)
                .apply(RequestOptions.placeholderOf(R.color.mobile_placeholder))
                .into(mThumb);
    }

    private static int dpToPx(Resources resources, int dp) {
        DisplayMetrics metrics = resources.getDisplayMetrics();
        return Math.round(dp * metrics.density);
    }
}
