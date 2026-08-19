package com.liskovsoft.smartyoutubetv2.mobile.ui.playback;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlayerView;
import com.github.vkay94.dtpv.DoubleTapPlayerAdapter;
import com.github.vkay94.dtpv.DoubleTapPlayerView;
import com.github.vkay94.dtpv.youtube.YouTubeOverlay;
import com.liskovsoft.mediaserviceinterfaces.data.CommentGroup;
import com.liskovsoft.mediaserviceinterfaces.data.CommentItem;
import com.liskovsoft.mediaserviceinterfaces.data.MediaItemFormatInfo;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup;
import com.bumptech.glide.request.RequestOptions;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.ChatReceiver;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.CommentsReceiver;
import com.liskovsoft.smartyoutubetv2.mobile.ui.dialogs.CommentsAdapter;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.SeekBarSegment;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.PlaybackPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.views.PlaybackView;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.controller.ExoPlayerController;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.other.ExoPlayerInitializer;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.selector.FormatItem;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.versions.renderer.CustomOverridesRenderersFactory;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.versions.selector.RestoreTrackSelector;
import com.liskovsoft.smartyoutubetv2.mobile.ui.base.MobileActivity;
import com.liskovsoft.smartyoutubetv2.mobile.ui.browse.adapter.VideoCardAdapter;
import com.liskovsoft.smartyoutubetv2.mobile.ui.browse.adapter.VideoCardCallbacks;
import com.liskovsoft.smartyoutubetv2.tv.R;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Phone/tablet player.
 *
 * The engine half of {@link PlaybackView} is delegated wholesale to {@link ExoPlayerController},
 * which is already UI-agnostic; only the presentation half is new here. Portrait shows the video
 * above the title, actions and suggestions; landscape promotes the video to fullscreen.
 */
public class MobilePlaybackActivity extends MobileActivity implements PlaybackView, VideoCardCallbacks {
    private PlaybackPresenter mPresenter;
    private ExoPlayerController mController;
    private ExoPlayerInitializer mInitializer;
    private SimpleExoPlayer mPlayer;
    private PlayerView mPlayerView;
    private View mPlayerContainer;
    private RecyclerView mBelowPlayer;
    private ProgressBar mProgressBar;
    private PlayerContentAdapter mContentAdapter;
    private ImageButton mFullscreenButton;
    private YouTubeOverlay mYouTubeOverlay;
    private DoubleTapPlayerAdapter mDoubleTapAdapter;
    private InlineCommentsController mCommentsController;
    private CommentsReceiver mCommentsReceiver;
    /** The bound meta row, when one is on screen; used to repaint action state live. */
    private View mMetaView;

    /**
     * The action strip under the video. Ids come from the common module and are the same ones the
     * presenter drives through setButtonState / onButtonClicked, so behaviour matches the TV UI.
     */
    private static final int[][] ACTIONS = {
            // id, icon, short visible label, full label used for the spoken description
            {R.id.action_thumbs_up,             R.drawable.mobile_ic_thumb_up,       R.string.mobile_action_like,        R.string.action_like},
            {R.id.action_thumbs_down,           R.drawable.mobile_ic_thumb_down,     R.string.mobile_action_dislike,     R.string.action_dislike},
            {R.id.action_subscribe,             R.drawable.action_subscribe,         R.string.mobile_action_subscribe,   R.string.mobile_action_subscribe},
            {R.id.action_channel,               R.drawable.action_channel,           R.string.mobile_action_channel,     R.string.action_channel},
            {R.id.action_chat,                  R.drawable.action_chat,              R.string.mobile_action_comments,    R.string.open_chat},
            {R.id.action_playlist_add,          R.drawable.action_playlist_add,      R.string.mobile_action_playlist,    R.string.action_playlist_add},
            {R.id.action_share,                 R.drawable.action_share,             R.string.mobile_action_share,       R.string.share_link},
            {R.id.action_info,                  R.drawable.action_info,              R.string.mobile_action_info,        R.string.action_video_info},
            {R.id.lb_control_high_quality,      R.drawable.lb_ic_hq,                 R.string.mobile_action_quality,     R.string.playback_settings},
            {R.id.lb_control_closed_captioning, R.drawable.lb_ic_cc,                 R.string.mobile_action_captions,    R.string.mobile_action_captions},
            {R.id.action_video_speed,           R.drawable.action_video_speed,       R.string.mobile_action_speed,       R.string.action_video_speed},
            {R.id.action_video_zoom,            R.drawable.action_video_zoom,        R.string.mobile_action_aspect,      R.string.video_aspect},
            {R.id.action_repeat,                R.drawable.action_mode_none,         R.string.mobile_action_repeat,      R.string.repeat_mode_none},
            {R.id.action_playback_queue,        R.drawable.action_queue,             R.string.mobile_action_queue,       R.string.action_playback_queue},
            {R.id.action_content_block,         R.drawable.action_content_block,     R.string.mobile_action_sponsorblock,R.string.content_block_provider},
            {R.id.action_seek_interval,         R.drawable.action_seek_interval,     R.string.mobile_action_seek,        R.string.seek_interval},
            {R.id.action_pip,                   R.drawable.action_pip,               R.string.mobile_action_background,  R.string.run_in_background},
            {R.id.action_search,                R.drawable.action_search,            R.string.mobile_action_search,      R.string.action_search},
            {R.id.action_sound_off,             R.drawable.action_sound_off,         R.string.mobile_action_mute,        R.string.action_sound_off},
            {R.id.action_rotate,                R.drawable.action_rotate,            R.string.mobile_action_rotate,      R.string.video_rotate},
            {R.id.action_flip,                  R.drawable.action_flip,              R.string.mobile_action_flip,        R.string.video_flip},
            {R.id.action_screen_dimming,        R.drawable.action_screen_timeout_on, R.string.mobile_action_dimming,     R.string.screen_dimming},
            {R.id.action_afr,                   R.drawable.action_afr,               R.string.mobile_action_afr,         R.string.auto_frame_rate},
            {R.id.action_video_stats,           R.drawable.action_video_stats,       R.string.mobile_action_stats,       R.string.player_tweaks},
    };

    private final Map<Integer, Integer> mButtonStates = new HashMap<>();
    private final Map<Integer, View> mButtonViews = new HashMap<>();
    private final Map<Integer, String> mButtonLabels = new HashMap<>();
    private final List<VideoGroup> mSuggestionGroups = new ArrayList<>();
    private Video mVideo;
    private CommentGroup mLastCommentGroup;
    private boolean mCommentsLoading;
    private boolean mShowingReplies;
    private String mTitle;
    private CharSequence mSubtitle;
    private String mChannelIconUrl;
    private boolean mIsFullscreen;
    private boolean mIsEngineBlocked;
    private int mResizeMode = RESIZE_MODE_DEFAULT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.App_Theme_Mobile_Player);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mobile_playback);

        bindViews();

        mInitializer = new ExoPlayerInitializer(this);
        mPresenter = PlaybackPresenter.instance(this);
        mPresenter.setView(this);
        mController = new ExoPlayerController(this, mPresenter);

        createPlayer();
        initGestures();
        applyOrientationLayout(getResources().getConfiguration().orientation);

        // Comments live on the page, under the actions, so the view needs its own metadata hook.
        mCommentsController = new InlineCommentsController(this::onCommentsReady);
        mPresenter.addEventListener(mCommentsController);

        mPresenter.onViewInitialized();
        mPresenter.onEngineInitialized();
    }

    private void bindViews() {
        mPlayerView = findViewById(R.id.player_view);
        mPlayerContainer = findViewById(R.id.player_container);
        mBelowPlayer = findViewById(R.id.below_player);
        mProgressBar = findViewById(R.id.player_progress);
        mYouTubeOverlay = findViewById(R.id.youtube_overlay);

        mContentAdapter = new PlayerContentAdapter(this::onBindMeta, this, mCommentCallbacks);
        mBelowPlayer.setLayoutManager(new LinearLayoutManager(this));
        mBelowPlayer.setAdapter(mContentAdapter);

        mFullscreenButton = mPlayerView.findViewById(R.id.mobile_fullscreen);

        if (mFullscreenButton != null) {
            mFullscreenButton.setOnClickListener(v -> toggleFullscreen());
        }
    }

    /**
     * Fills the metadata row. It is a recycled row like any other, so the action strip is rebuilt
     * here from the states the presenter has published rather than being constructed once.
     */
    private void onBindMeta(View metaView) {
        mMetaView = metaView;

        TextView title = metaView.findViewById(R.id.video_title);
        TextView subtitle = metaView.findViewById(R.id.video_subtitle);
        ImageView channelIcon = metaView.findViewById(R.id.channel_icon);
        LinearLayout actions = metaView.findViewById(R.id.player_actions);

        title.setText(mTitle);
        subtitle.setText(mSubtitle);

        if (TextUtils.isEmpty(mChannelIconUrl)) {
            channelIcon.setVisibility(View.GONE);
        } else {
            channelIcon.setVisibility(View.VISIBLE);
            Glide.with(this).load(mChannelIconUrl).apply(RequestOptions.circleCropTransform()).into(channelIcon);
        }

        metaView.findViewById(R.id.channel_row).setOnClickListener(
                v -> mPresenter.onButtonClicked(R.id.action_channel, getButtonState(R.id.action_channel)));

        buildActionStrip(actions);
    }

    private void buildActionStrip(LinearLayout container) {
        container.removeAllViews();
        mButtonViews.clear();

        LayoutInflater inflater = LayoutInflater.from(this);

        for (int[] action : ACTIONS) {
            int id = action[0];
            View item = inflater.inflate(R.layout.item_mobile_action, container, false);

            ImageView icon = item.findViewById(R.id.action_icon);
            TextView label = item.findViewById(R.id.action_label);
            icon.setImageResource(action[1]);
            label.setText(action[2]);
            mButtonLabels.put(id, getString(action[3]));

            item.setOnClickListener(v -> mPresenter.onButtonClicked(id, getButtonState(id)));
            item.setOnLongClickListener(v -> {
                mPresenter.onButtonLongClicked(id, getButtonState(id));
                return true;
            });

            mButtonViews.put(id, item);
            container.addView(item);
            applyButtonState(id, getButtonState(id));
        }
    }

    /** Repaints one action. BUTTON_DISABLED hides it, mirroring the TV's disabled actions. */
    private void applyButtonState(int buttonId, int buttonState) {
        View item = mButtonViews.get(buttonId);

        // Nothing to repaint while the meta row is scrolled off; the state is stored and applied
        // when the row is bound again.
        if (item == null) {
            return;
        }

        if (buttonState == BUTTON_DISABLED) {
            item.setVisibility(View.GONE);
            return;
        }

        item.setVisibility(View.VISIBLE);

        boolean on = buttonState != BUTTON_OFF;
        ImageView icon = item.findViewById(R.id.action_icon);
        TextView label = item.findViewById(R.id.action_label);
        int color = ContextCompat.getColor(this, on ? R.color.mobile_primary : R.color.mobile_on_surface_secondary);
        icon.setColorFilter(color);
        label.setTextColor(color);

        // The visible caption is abbreviated to fit; TalkBack gets the full name plus the toggle
        // state, since the icon tint alone conveys nothing.
        String spoken = mButtonLabels.get(buttonId);
        item.setContentDescription((spoken != null ? spoken : label.getText())
                + ", " + getString(on ? R.string.mobile_action_on : R.string.mobile_action_off));
    }

    private final CommentsAdapter.CommentCallbacks mCommentCallbacks = new CommentsAdapter.CommentCallbacks() {
        @Override
        public void onCommentClicked(CommentItem item) {
            if (item == null || item.isEmpty() || item.getNestedCommentsKey() == null) {
                return;
            }

            // Replies replace the stream; backing out reloads the top-level comments.
            attachComments(mCommentsController.createRepliesReceiver(item), true);
        }

        @Override
        public void onCommentLongClicked(CommentItem item) {
            // Liking a comment requires an account; the action lives in the signed-in build only.
        }
    };

    /** Called when a new video's comments become available (or null when it has none). */
    private void onCommentsReady(CommentsReceiver receiver) {
        mShowingReplies = false;
        attachComments(receiver, false);
    }

    private void attachComments(CommentsReceiver receiver, boolean isReplies) {
        mCommentsReceiver = receiver;
        mShowingReplies = isReplies;
        mContentAdapter.clearComments();

        if (receiver == null) {
            mContentAdapter.showComments(false, null);
            return;
        }

        mContentAdapter.showComments(true, getString(isReplies ? R.string.mobile_replies : R.string.mobile_action_comments));
        mContentAdapter.setCommentsHeader(getString(R.string.mobile_loading));

        receiver.setCallback(new CommentsReceiver.Callback() {
            @Override
            public void onCommentGroup(CommentGroup commentGroup) {
                if (mCommentsReceiver != receiver) {
                    return; // a newer video's stream took over
                }

                mLastCommentGroup = commentGroup;
                mCommentsLoading = false;

                if (commentGroup != null && commentGroup.getComments() != null) {
                    mContentAdapter.addComments(commentGroup.getComments());
                }

                mContentAdapter.setCommentsHeader(mContentAdapter.getCommentCount() == 0
                        ? getString(R.string.mobile_no_comments)
                        : getString(isReplies ? R.string.mobile_replies : R.string.mobile_action_comments));
            }

            @Override
            public void onBackup(CommentsReceiver.Backup backup) { }

            @Override
            public void onSync(CommentItem commentItem) {
                mContentAdapter.syncComment(commentItem);
            }
        });

        mCommentsLoading = true;
        receiver.onStart();
    }

    /** Pages the comment stream when the user nears the end of the page. */
    private void loadMoreCommentsIfNeeded() {
        if (mCommentsLoading || mCommentsReceiver == null || mLastCommentGroup == null
                || mLastCommentGroup.getNextCommentsKey() == null) {
            return;
        }

        mCommentsLoading = true;
        mCommentsReceiver.onLoadMore(mLastCommentGroup);
    }

    /** Double-tap either edge of the video to seek, matching the TV build's gesture handling. */
    private void initGestures() {
        mDoubleTapAdapter = new DoubleTapPlayerAdapter(mPlayerContainer);
        mDoubleTapAdapter.controller(mYouTubeOverlay);
        mDoubleTapAdapter.onSingleTap(event -> {
            if (isControlsShown()) {
                showControls(false);
            } else {
                showControls(true);
            }
        });

        mYouTubeOverlay
                .player(mPlayer)
                .playerView(mDoubleTapAdapter)
                .seekSeconds(10)
                .performListener(new YouTubeOverlay.PerformListener() {
                    @Override
                    public void onAnimationStart() {
                        mYouTubeOverlay.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onAnimationEnd() {
                        mYouTubeOverlay.setVisibility(View.GONE);
                    }

                    @Override
                    public Boolean shouldForward(@NonNull Player player, @NonNull DoubleTapPlayerView playerView, float posX) {
                        int state = player.getPlaybackState();

                        if (state == Player.STATE_IDLE || state == Player.STATE_ENDED) {
                            playerView.cancelInDoubleTapMode();
                            return false;
                        }

                        // Only the outer thirds seek; the middle stays a plain tap target.
                        if (posX < playerView.getPlayerWidth() * 0.35) {
                            return false;
                        }

                        if (posX > playerView.getPlayerWidth() * 0.65) {
                            return true;
                        }

                        playerView.cancelInDoubleTapMode();
                        return null;
                    }
                });

        mPlayerContainer.setOnTouchListener((v, event) -> mDoubleTapAdapter.onTouchEvent(event));

        // Paging the comment stream is driven by the page's own scroll position.
        mBelowPlayer.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                LinearLayoutManager manager = (LinearLayoutManager) recyclerView.getLayoutManager();

                if (manager != null && manager.findLastVisibleItemPosition() >= mContentAdapter.getItemCount() - 6) {
                    loadMoreCommentsIfNeeded();
                }
            }
        });
    }

    private void createPlayer() {
        DefaultTrackSelector trackSelector = new RestoreTrackSelector(new AdaptiveTrackSelection.Factory());
        mController.setTrackSelector(trackSelector);

        mPlayer = mInitializer.createPlayer(this, new CustomOverridesRenderersFactory(this), trackSelector);
        mController.setPlayer(mPlayer);
        mPlayerView.setPlayer(mPlayer);

        // Backstop for the presenter's progress bar: once the engine is ready the spinner must go,
        // regardless of which controller last asked for it.
        mPlayer.addListener(new Player.EventListener() {
            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                if (playbackState == Player.STATE_READY) {
                    mProgressBar.setVisibility(View.GONE);
                }
            }
        });
    }

    // ---------------- orientation / fullscreen ----------------

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        applyOrientationLayout(newConfig.orientation);
    }

    /** Landscape gives the video the whole screen; portrait restores the 16:9 + content split. */
    private void applyOrientationLayout(int orientation) {
        boolean fullscreen = orientation == Configuration.ORIENTATION_LANDSCAPE || mIsFullscreen;

        ViewGroup.LayoutParams params = mPlayerContainer.getLayoutParams();
        params.height = fullscreen
                ? ViewGroup.LayoutParams.MATCH_PARENT
                : Math.round(getResources().getDisplayMetrics().widthPixels * 9f / 16f);
        mPlayerContainer.setLayoutParams(params);

        mIsFullscreen = fullscreen;
        updateBelowPlayerVisibility();

        if (mFullscreenButton != null) {
            mFullscreenButton.setImageResource(
                    fullscreen ? R.drawable.mobile_ic_fullscreen_exit : R.drawable.mobile_ic_fullscreen);
            mFullscreenButton.setContentDescription(
                    getString(fullscreen ? R.string.mobile_exit_fullscreen : R.string.mobile_fullscreen));
        }
    }

    private void toggleFullscreen() {
        mIsFullscreen = !mIsFullscreen;
        // Rotating is what actually gives the video the screen; the layout follows the new config.
        setRequestedOrientation(mIsFullscreen
                ? ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                : ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
    }

    @Override
    public void onBackPressed() {
        if (mIsFullscreen) {
            toggleFullscreen();
            return;
        }

        if (mShowingReplies) {
            // Step back out of a reply thread to the top-level comments.
            mCommentsController.onMetadataRefresh();
            return;
        }

        super.onBackPressed();
    }

    // ---------------- lifecycle ----------------

    @Override
    protected void onResume() {
        super.onResume();
        mPresenter.onViewResumed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPresenter.onViewPaused();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mPresenter.removeEventListener(mCommentsController);
        mCommentsController.dispose();
        mPresenter.onViewDestroyed();
        releasePlayer();
    }

    private void releasePlayer() {
        if (mPlayer != null) {
            // Must run before the engine goes away: this is what persists the watch position.
            mPresenter.onEngineReleased();
            mPlayerView.setPlayer(null);
            mController.release();
            mPlayer = null;
        }
    }

    // ---------------- PlayerEngine: source loading ----------------

    @Override
    public void openSabr(MediaItemFormatInfo formatInfo) {
        mController.openSabr(formatInfo);
    }

    @Override
    public void openDash(MediaItemFormatInfo formatInfo) {
        mController.openDash(formatInfo);
    }

    @Override
    public void openDash(InputStream dashManifest) {
        mController.openDash(dashManifest);
    }

    @Override
    public void openDashUrl(String dashManifestUrl) {
        mController.openDashUrl(dashManifestUrl);
    }

    @Override
    public void openHlsUrl(String hlsPlaylistUrl) {
        mController.openHlsUrl(hlsPlaylistUrl);
    }

    @Override
    public void openUrlList(List<String> urlList) {
        mController.openUrlList(urlList);
    }

    @Override
    public void openMerged(MediaItemFormatInfo formatInfo, String hlsPlaylistUrl) {
        mController.openMerged(formatInfo, hlsPlaylistUrl);
    }

    @Override
    public void openMerged(InputStream dashManifest, String hlsPlaylistUrl) {
        mController.openMerged(dashManifest, hlsPlaylistUrl);
    }

    // ---------------- PlayerEngine: transport ----------------

    @Override
    public long getPositionMs() {
        return mController.getPositionMs();
    }

    @Override
    public void setPositionMs(long positionMs) {
        mController.setPositionMs(positionMs);
    }

    @Override
    public long getDurationMs() {
        return mController.getDurationMs();
    }

    @Override
    public void setPlayWhenReady(boolean play) {
        mController.setPlayWhenReady(play);
    }

    @Override
    public boolean getPlayWhenReady() {
        return mController.getPlayWhenReady();
    }

    @Override
    public boolean isPlaying() {
        return mController.isPlaying();
    }

    @Override
    public boolean isLoading() {
        return mController.isLoading();
    }

    @Override
    public boolean containsMedia() {
        return mController.containsMedia();
    }

    @Override
    public void resetPlayerState() {
        mController.resetPlayerState();
    }

    // ---------------- PlayerEngine: tracks ----------------

    @Override
    public List<FormatItem> getVideoFormats() {
        return mController.getVideoFormats();
    }

    @Override
    public List<FormatItem> getAudioFormats() {
        return mController.getAudioFormats();
    }

    @Override
    public List<FormatItem> getSubtitleFormats() {
        return mController.getSubtitleFormats();
    }

    @Override
    public void setFormat(FormatItem option) {
        mController.selectFormat(option);
    }

    @Override
    public FormatItem getVideoFormat() {
        return mController.getVideoFormat();
    }

    @Override
    public FormatItem getAudioFormat() {
        return mController.getAudioFormat();
    }

    @Override
    public FormatItem getSubtitleFormat() {
        return mController.getSubtitleFormat();
    }

    // ---------------- PlayerEngine: engine state ----------------

    @Override
    public boolean isEngineInitialized() {
        return mPlayer != null;
    }

    @Override
    public void restartEngine() {
        releasePlayer();
        createPlayer();
        mPresenter.onEngineInitialized();
    }

    /** Teardown order matters: the presenter saves state, then the engine is torn down. */

    @Override
    public void reloadPlayback() {
        if (mPlayer != null) {
            mPresenter.onEngineReleased();
            mPresenter.onEngineInitialized();
        }
    }

    @Override
    public void blockEngine(boolean block) {
        mIsEngineBlocked = block;
    }

    @Override
    public boolean isEngineBlocked() {
        return mIsEngineBlocked;
    }

    @Override
    public boolean isInPIPMode() {
        return sIsInPipMode;
    }

    // ---------------- PlayerEngine: audio/video shaping ----------------

    @Override
    public void setSpeed(float speed) {
        mController.setSpeed(speed);
    }

    @Override
    public float getSpeed() {
        return mController.getSpeed();
    }

    @Override
    public void setPitch(float pitch) {
        mController.setPitch(pitch);
    }

    @Override
    public float getPitch() {
        return mController.getPitch();
    }

    @Override
    public void setVolume(float volume) {
        mController.setVolume(volume);
    }

    @Override
    public float getVolume() {
        return mController.getVolume();
    }

    @Override
    public void setResizeMode(int mode) {
        mResizeMode = mode;
        mPlayerView.setResizeMode(mode);
    }

    @Override
    public int getResizeMode() {
        return mResizeMode;
    }

    /** ExoPlayer's content frame, which owns aspect ratio and zoom for the video surface. */
    private AspectRatioFrameLayout getContentFrame() {
        View frame = mPlayerView.findViewById(R.id.exo_content_frame);
        return frame instanceof AspectRatioFrameLayout ? (AspectRatioFrameLayout) frame : null;
    }

    @Override
    public void setZoomPercents(int percents) {
        AspectRatioFrameLayout contentFrame = getContentFrame();

        // Must go through the content frame, which ignores non-positive values. Scaling the
        // surface view directly turned the stored default of -1 into a 1% mirrored video.
        if (contentFrame != null) {
            contentFrame.setZoom(percents);
        }
    }

    @Override
    public void setAspectRatio(float ratio) {
        AspectRatioFrameLayout contentFrame = getContentFrame();

        // A ratio of 0 means "use the video's own aspect", which PlayerView already applies from
        // the decoded video size. Forwarding the 0 would blank the frame.
        if (contentFrame != null && ratio > 0) {
            contentFrame.setAspectRatio(ratio);
        }
    }

    @Override
    public void setRotationAngle(int angle) {
        // Rotating a SurfaceView needs a TextureView swap and an engine restart (see the TV
        // SurfacePlaybackFragment). Not offered on mobile, where the device handles orientation.
    }

    @Override
    public void setVideoFlipEnabled(boolean enabled) {
        // See setRotationAngle: needs a TextureView-backed surface.
    }

    @Override
    public void setVideoGravity(int gravity) {
        // Repositioning the surface within its container is a TV-only accommodation for
        // overscan; on a handset the video always fills the player box.
    }

    // ---------------- PlayerManager ----------------

    @Override
    public void setVideo(Video item) {
        mVideo = item;
        mController.setVideo(item);

        if (item != null) {
            mTitle = item.getTitle();
            mSubtitle = item.getSecondTitleFull();
            refreshMeta();
        }
    }

    @Override
    public Video getVideo() {
        return mVideo;
    }

    @Override
    public void finishReally() {
        super.finishReally();
    }

    @Override
    public void showBackground(String url) {
        // The player surface covers the window on a handset, so there is no visible backdrop.
    }

    @Override
    public void showBackgroundColor(int colorResId) {
        mPlayerContainer.setBackgroundColor(Color.BLACK);
    }

    @Override
    public boolean isEmbed() {
        return false;
    }

    // ---------------- PlayerUI: suggestions ----------------

    @Override
    public void updateSuggestions(VideoGroup group) {
        if (group == null) {
            return;
        }

        int index = getSuggestionsIndex(group);

        if (index == -1) {
            mSuggestionGroups.add(group);
        } else {
            mSuggestionGroups.set(index, group);
        }

        // The phone layout shows one flat "up next" list rather than a row per group.
        List<Video> all = new ArrayList<>();

        for (VideoGroup suggestionGroup : mSuggestionGroups) {
            if (suggestionGroup.getVideos() != null) {
                all.addAll(suggestionGroup.getVideos());
            }
        }

        mContentAdapter.setSuggestions(all, getString(R.string.mobile_suggestions));
    }

    @Override
    public void removeSuggestions(VideoGroup group) {
        int index = getSuggestionsIndex(group);

        if (index != -1) {
            mSuggestionGroups.remove(index);
            rebuildSuggestions();
        }
    }

    private void rebuildSuggestions() {
        List<Video> all = new ArrayList<>();

        for (VideoGroup suggestionGroup : mSuggestionGroups) {
            if (suggestionGroup.getVideos() != null) {
                all.addAll(suggestionGroup.getVideos());
            }
        }

        mContentAdapter.setSuggestions(all, getString(R.string.mobile_suggestions));
    }

    @Override
    public int getSuggestionsIndex(VideoGroup group) {
        for (int i = 0; i < mSuggestionGroups.size(); i++) {
            if (mSuggestionGroups.get(i).getId() == group.getId()) {
                return i;
            }
        }

        return -1;
    }

    @Override
    public VideoGroup getSuggestionsByIndex(int index) {
        return index >= 0 && index < mSuggestionGroups.size() ? mSuggestionGroups.get(index) : null;
    }

    @Override
    public void focusSuggestedItem(int index) {
        if (index >= 0) {
            mBelowPlayer.smoothScrollToPosition(index);
        }
    }

    @Override
    public void focusSuggestedItem(Video video) {
        int index = mContentAdapter.getSuggestions().indexOf(video);

        if (index != -1) {
            mBelowPlayer.smoothScrollToPosition(index);
        }
    }

    @Override
    public void resetSuggestedPosition() {
        mBelowPlayer.scrollToPosition(0);
    }

    @Override
    public boolean isSuggestionsEmpty() {
        return mContentAdapter.getSuggestions().isEmpty();
    }

    @Override
    public void clearSuggestions() {
        mSuggestionGroups.clear();
        mContentAdapter.clearSuggestions();
    }

    // ---------------- PlayerUI: chrome ----------------

    /**
     * On TV "overlay" means the chrome drawn over the video, which the presenter hides during
     * playback. It must not be mapped to the metadata/suggestions panel: that panel is ordinary
     * page content on a phone and its visibility is owned by the fullscreen state alone.
     */
    @Override
    public void showOverlay(boolean show) {
        showControls(show);
    }

    private void updateBelowPlayerVisibility() {
        mBelowPlayer.setVisibility(mIsFullscreen ? View.GONE : View.VISIBLE);
    }

    @Override
    public boolean isOverlayShown() {
        return isControlsShown();
    }

    @Override
    public void showSuggestions(boolean show) {
        // The page scrolls as a unit on mobile; suggestions are always part of it.
    }

    @Override
    public boolean isSuggestionsShown() {
        return !mContentAdapter.getSuggestions().isEmpty();
    }

    @Override
    public void showControls(boolean show) {
        if (show) {
            mPlayerView.showController();
        } else {
            mPlayerView.hideController();
        }
    }

    @Override
    public boolean isControlsShown() {
        return mPlayerView.isControllerVisible();
    }

    @Override
    public int getButtonState(int buttonId) {
        Integer state = mButtonStates.get(buttonId);
        return state != null ? state : BUTTON_OFF;
    }

    @Override
    public void setButtonState(int buttonId, int buttonState) {
        mButtonStates.put(buttonId, buttonState);
        applyButtonState(buttonId, buttonState);
    }

    @Override
    public void setChannelIcon(String iconUrl) {
        mChannelIconUrl = iconUrl;
        refreshMeta();
    }

    @Override
    public void setTitle(String title) {
        mTitle = title;
        refreshMeta();
    }

    private void refreshMeta() {
        if (mContentAdapter != null) {
            mContentAdapter.notifyItemChanged(0);
        }
    }

    @Override
    public void setSeekPreviewTitle(String title) {
        // Scrub previews are not shown in the mobile controller yet.
    }

    @Override
    public void setNextTitle(Video nextVideo) {
        // "Up next" is the first row of the suggestions list on a phone.
    }

    @Override
    public void showDebugInfo(boolean show) {
        // Debug overlay is a TV-side diagnostic; not surfaced in the mobile chrome.
    }

    @Override
    public void showSubtitles(boolean show) {
        View subtitleView = mPlayerView.getSubtitleView();

        if (subtitleView != null) {
            subtitleView.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void loadStoryboard() {
        // Storyboard scrubbing thumbnails are not wired into the mobile time bar yet.
    }

    @Override
    public void showProgressBar(boolean show) {
        // While the engine is loading or buffering it owns the indicator (PlayerView draws its
        // own), so presenter-driven toggles are ignored - mirrors the TV implementation.
        if (mController.isLoading() || mController.isBuffering()) {
            return;
        }

        mProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void setSeekBarSegments(List<SeekBarSegment> segments) {
        // SponsorBlock segment shading needs a custom time bar; skipping still works because
        // the presenter seeks past segments itself.
    }

    @Override
    public void updateEndingTime() {
        // The controller renders position/duration itself.
    }

    @Override
    public void setChatReceiver(ChatReceiver chatReceiver) {
        // Live chat is presented through the dialog menu rather than beside the video.
    }

    // ---------------- suggestion card callbacks ----------------

    @Override
    public void onVideoClicked(Video video) {
        mPresenter.onSuggestionItemClicked(video);
    }

    @Override
    public void onVideoMenu(Video video) {
        mPresenter.onSuggestionItemLongClicked(video);
    }

    @Override
    public void onNearEnd(Video lastVisible) {
        mPresenter.onScrollEnd(lastVisible);
    }
}
