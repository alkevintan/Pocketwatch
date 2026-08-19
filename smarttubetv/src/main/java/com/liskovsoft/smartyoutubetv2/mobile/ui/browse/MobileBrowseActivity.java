package com.liskovsoft.smartyoutubetv2.mobile.ui.browse;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.BrowseSection;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.SettingsGroup;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup;
import com.liskovsoft.smartyoutubetv2.common.app.models.errors.ErrorFragmentData;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.BrowsePresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.SearchPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.views.BrowseView;
import com.liskovsoft.smartyoutubetv2.mobile.ui.base.MobileActivity;
import com.liskovsoft.smartyoutubetv2.tv.R;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Phone/tablet home screen. Sections become bottom-navigation destinations; anything past the
 * first few is reachable through an overflow sheet, so an arbitrary number of pinned sections works.
 */
public class MobileBrowseActivity extends MobileActivity implements BrowseView {
    /** Bottom nav holds at most 5 items; reserve the last for the overflow when needed. */
    private static final int MAX_NAV_ITEMS = 5;
    private static final int OVERFLOW_ITEM_ID = Integer.MAX_VALUE;

    private final List<BrowseSection> mSections = new ArrayList<>();
    private final Map<Integer, SectionFragment> mFragments = new LinkedHashMap<>();
    private BrowsePresenter mPresenter;
    private BottomNavigationView mBottomNav;
    private Toolbar mToolbar;
    private ProgressBar mProgressBar;
    private int mCurrentSectionId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.App_Theme_Mobile);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mobile_browse);

        mToolbar = findViewById(R.id.toolbar);
        mBottomNav = findViewById(R.id.bottom_nav);
        mProgressBar = findViewById(R.id.browse_progress);

        setSupportActionBar(mToolbar);
        mBottomNav.setOnNavigationItemSelectedListener(this::onNavItemSelected);

        mPresenter = BrowsePresenter.instance(this);
        mPresenter.setView(this);
        mPresenter.onViewInitialized();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem search = menu.add(Menu.NONE, R.id.mobile_action_search, Menu.NONE, R.string.mobile_search_hint);
        search.setIcon(android.R.drawable.ic_menu_search);
        search.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.mobile_action_search) {
            SearchPresenter.instance(this).startSearch(null);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

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
        mPresenter.onViewDestroyed();
    }

    // ---------------- BrowseView ----------------

    @Override
    public void addSection(int index, BrowseSection section) {
        if (section == null) {
            return;
        }

        int existing = indexOfSection(section.getId());

        if (existing != -1) {
            mSections.set(existing, section);
        } else if (index >= 0 && index <= mSections.size()) {
            mSections.add(index, section);
        } else {
            mSections.add(section);
        }

        rebuildNavigation();
    }

    @Override
    public void removeSection(BrowseSection section) {
        if (section == null) {
            return;
        }

        int index = indexOfSection(section.getId());

        if (index != -1) {
            mSections.remove(index);
            mFragments.remove(section.getId());
            rebuildNavigation();
        }
    }

    @Override
    public void removeAllSections() {
        mSections.clear();
        mFragments.clear();
        rebuildNavigation();
    }

    @Override
    public void selectSection(int index, boolean focusOnContent) {
        if (index < 0 || index >= mSections.size()) {
            return;
        }

        showSection(mSections.get(index));
    }

    @Override
    public void updateSection(VideoGroup group) {
        if (group == null) {
            return;
        }

        SectionFragment fragment = findFragmentFor(group.getSection());

        if (fragment != null) {
            fragment.update(group);
        }
    }

    @Override
    public void updateSection(SettingsGroup group) {
        if (group == null) {
            return;
        }

        SectionFragment fragment = findFragmentFor(group.getCategory());

        if (fragment != null) {
            fragment.update(group);
        }
    }

    @Override
    public void clearSection(BrowseSection section) {
        SectionFragment fragment = findFragmentFor(section);

        if (fragment != null) {
            fragment.clear();
        }
    }

    @Override
    public void selectSectionItem(int index) {
        SectionFragment fragment = currentFragment();

        if (fragment != null) {
            fragment.selectItem(index);
        }
    }

    @Override
    public void selectSectionItem(Video item) {
        SectionFragment fragment = currentFragment();

        if (fragment != null) {
            fragment.selectItem(item);
        }
    }

    @Override
    public void showError(ErrorFragmentData data) {
        SectionFragment fragment = currentFragment();

        if (fragment != null) {
            fragment.showError(data);
        }
    }

    @Override
    public void showProgressBar(boolean show) {
        mProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean isProgressBarShowing() {
        return mProgressBar.getVisibility() == View.VISIBLE;
    }

    @Override
    public void focusOnContent() {
        SectionFragment fragment = currentFragment();

        if (fragment != null && fragment.getView() != null) {
            fragment.getView().requestFocus();
        }
    }

    @Override
    public boolean isEmpty() {
        SectionFragment fragment = currentFragment();
        return fragment == null || fragment.isEmpty();
    }

    @Override
    public void updateBadge() {
        // The TV UI paints an account avatar into the header. On mobile the account lives in the
        // settings section, so there is no badge surface to refresh here.
    }

    // ---------------- navigation ----------------

    private void rebuildNavigation() {
        Menu menu = mBottomNav.getMenu();
        menu.clear();

        List<BrowseSection> enabled = enabledSections();
        boolean needsOverflow = enabled.size() > MAX_NAV_ITEMS;
        int inlineCount = needsOverflow ? MAX_NAV_ITEMS - 1 : enabled.size();

        for (int i = 0; i < inlineCount; i++) {
            BrowseSection section = enabled.get(i);
            MenuItem item = menu.add(Menu.NONE, section.getId(), i, section.getTitle());

            if (section.getResId() > 0) {
                item.setIcon(section.getResId());
            }
        }

        if (needsOverflow) {
            MenuItem more = menu.add(Menu.NONE, OVERFLOW_ITEM_ID, inlineCount, R.string.mobile_more_sections);
            more.setIcon(R.drawable.mobile_ic_more_vert);
        }

        mBottomNav.setVisibility(enabled.isEmpty() ? View.GONE : View.VISIBLE);

        // Land on the first section once the presenter has published some.
        if (mCurrentSectionId == -1 && !enabled.isEmpty()) {
            showSection(enabled.get(0));
        }
    }

    private boolean onNavItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == OVERFLOW_ITEM_ID) {
            showOverflowSheet();
            return false; // keep the previous tab checked
        }

        BrowseSection section = findSection(item.getItemId());

        if (section != null) {
            showSection(section);
            return true;
        }

        return false;
    }

    private void showOverflowSheet() {
        List<BrowseSection> enabled = enabledSections();
        List<BrowseSection> overflow = enabled.subList(Math.min(MAX_NAV_ITEMS - 1, enabled.size()), enabled.size());

        SectionPickerSheet sheet = SectionPickerSheet.create(overflow, this::showSection);
        sheet.show(getSupportFragmentManager(), "section_picker");
    }

    private void showSection(BrowseSection section) {
        if (section == null) {
            return;
        }

        mCurrentSectionId = section.getId();
        mToolbar.setTitle(section.getTitle());

        SectionFragment fragment = mFragments.get(section.getId());

        if (fragment == null) {
            fragment = SectionFragment.create(section);
            mFragments.put(section.getId(), fragment);
        }

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.section_container, fragment);
        transaction.commitAllowingStateLoss();

        // Keep the nav highlight in step. Sections reached through the overflow sheet have no bar
        // item of their own, so the overflow entry is highlighted instead of leaving a stale tab lit.
        MenuItem item = mBottomNav.getMenu().findItem(section.getId());

        if (item == null) {
            item = mBottomNav.getMenu().findItem(OVERFLOW_ITEM_ID);
        }

        if (item != null && !item.isChecked()) {
            item.setChecked(true);
        }

        mPresenter.onSectionFocused(section.getId());
    }

    private List<BrowseSection> enabledSections() {
        List<BrowseSection> result = new ArrayList<>();

        for (BrowseSection section : mSections) {
            if (section.isEnabled()) {
                result.add(section);
            }
        }

        return result;
    }

    private int indexOfSection(int sectionId) {
        for (int i = 0; i < mSections.size(); i++) {
            if (mSections.get(i).getId() == sectionId) {
                return i;
            }
        }

        return -1;
    }

    private BrowseSection findSection(int sectionId) {
        int index = indexOfSection(sectionId);
        return index != -1 ? mSections.get(index) : null;
    }

    private SectionFragment findFragmentFor(BrowseSection section) {
        if (section == null) {
            // Updates without a section belong to whatever the user is looking at.
            return currentFragment();
        }

        return mFragments.get(section.getId());
    }

    private SectionFragment currentFragment() {
        return mFragments.get(mCurrentSectionId);
    }
}
