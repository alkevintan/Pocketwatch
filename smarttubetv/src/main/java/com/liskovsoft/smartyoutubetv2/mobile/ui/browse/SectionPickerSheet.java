package com.liskovsoft.smartyoutubetv2.mobile.ui.browse;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.BrowseSection;
import com.liskovsoft.smartyoutubetv2.tv.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Lists the sections that did not fit in the bottom navigation bar.
 */
public class SectionPickerSheet extends BottomSheetDialogFragment {
    public interface OnSectionPicked {
        void onPicked(BrowseSection section);
    }

    private List<BrowseSection> mSections = new ArrayList<>();
    private OnSectionPicked mListener;

    public static SectionPickerSheet create(List<BrowseSection> sections, OnSectionPicked listener) {
        SectionPickerSheet sheet = new SectionPickerSheet();
        sheet.mSections = new ArrayList<>(sections);
        sheet.mListener = listener;
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundResource(R.color.mobile_surface);
        int padding = (int) (8 * getResources().getDisplayMetrics().density);
        root.setPadding(0, padding, 0, padding);

        for (BrowseSection section : mSections) {
            root.addView(createRow(inflater, root, section));
        }

        return root;
    }

    private View createRow(LayoutInflater inflater, ViewGroup parent, BrowseSection section) {
        View row = inflater.inflate(R.layout.item_mobile_setting, parent, false);
        TextView title = row.findViewById(R.id.setting_title);
        title.setText(section.getTitle());

        if (section.getResId() > 0) {
            ((android.widget.ImageView) row.findViewById(R.id.setting_icon)).setImageResource(section.getResId());
        } else {
            row.findViewById(R.id.setting_icon).setVisibility(View.GONE);
        }

        row.setContentDescription(section.getTitle());
        row.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onPicked(section);
            }
            dismissAllowingStateLoss();
        });

        return row;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // The callback cannot survive process death; close rather than show a sheet that does nothing.
        if (savedInstanceState != null && mListener == null) {
            dismissAllowingStateLoss();
        }
    }
}
