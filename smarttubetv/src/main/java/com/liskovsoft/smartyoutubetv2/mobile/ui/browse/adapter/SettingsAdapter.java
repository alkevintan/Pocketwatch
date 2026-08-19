package com.liskovsoft.smartyoutubetv2.mobile.ui.browse.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.liskovsoft.smartyoutubetv2.common.app.models.data.SettingsItem;
import com.liskovsoft.smartyoutubetv2.tv.R;

import java.util.ArrayList;
import java.util.List;

/** Renders a TYPE_SETTINGS_GRID section as a plain vertical list of tappable rows. */
public class SettingsAdapter extends RecyclerView.Adapter<SettingsAdapter.SettingsViewHolder> {
    private final List<SettingsItem> mItems = new ArrayList<>();

    @NonNull
    @Override
    public SettingsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mobile_setting, parent, false);
        return new SettingsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SettingsViewHolder holder, int position) {
        holder.bind(mItems.get(position));
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    public boolean isEmpty() {
        return mItems.isEmpty();
    }

    public void replace(List<SettingsItem> items) {
        mItems.clear();
        if (items != null) {
            mItems.addAll(items);
        }
        notifyDataSetChanged();
    }

    public void clear() {
        int size = mItems.size();
        mItems.clear();
        notifyItemRangeRemoved(0, size);
    }

    static class SettingsViewHolder extends RecyclerView.ViewHolder {
        private final TextView mTitle;
        private final ImageView mIcon;

        SettingsViewHolder(View itemView) {
            super(itemView);
            mTitle = itemView.findViewById(R.id.setting_title);
            mIcon = itemView.findViewById(R.id.setting_icon);
        }

        void bind(SettingsItem item) {
            mTitle.setText(item.title);

            if (item.imageResId > 0) {
                mIcon.setImageResource(item.imageResId);
                mIcon.setVisibility(View.VISIBLE);
            } else {
                mIcon.setVisibility(View.GONE);
            }

            itemView.setContentDescription(item.title);
            itemView.setOnClickListener(v -> {
                if (item.onClick != null) {
                    item.onClick.run();
                }
            });
        }
    }
}
