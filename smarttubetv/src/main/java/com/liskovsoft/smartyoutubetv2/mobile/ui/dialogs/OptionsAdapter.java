package com.liskovsoft.smartyoutubetv2.mobile.ui.dialogs;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionCategory;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionItem;
import com.liskovsoft.smartyoutubetv2.tv.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Flattens the presenter's option categories into a single scrolling list of rows.
 *
 * The TV UI navigates into a sub-screen per category; on a phone the categories are laid out
 * inline under headers instead, which keeps every setting one scroll away rather than several taps.
 */
public class OptionsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_HEADER = 0;
    private static final int VIEW_PLAIN = 1;
    private static final int VIEW_RADIO = 2;
    private static final int VIEW_CHECK = 3;
    private static final int VIEW_SWITCH = 4;
    private static final int VIEW_LONGTEXT = 5;

    /** One visual line: either a category header or a single option. */
    private static class Row {
        final int viewType;
        final CharSequence header;
        final OptionItem item;
        final OptionCategory category;

        Row(int viewType, CharSequence header, OptionItem item, OptionCategory category) {
            this.viewType = viewType;
            this.header = header;
            this.item = item;
            this.category = category;
        }
    }

    private final List<Row> mRows = new ArrayList<>();

    public void setCategories(List<OptionCategory> categories) {
        mRows.clear();

        if (categories != null) {
            for (OptionCategory category : categories) {
                addCategory(category);
            }
        }

        notifyDataSetChanged();
    }

    private void addCategory(OptionCategory category) {
        if (category == null || category.options == null) {
            return;
        }

        if (!TextUtils.isEmpty(category.title)) {
            mRows.add(new Row(VIEW_HEADER, category.title, null, category));
        }

        int viewType = viewTypeFor(category.type);

        for (OptionItem item : category.options) {
            mRows.add(new Row(viewType, null, item, category));
        }
    }

    private int viewTypeFor(int categoryType) {
        switch (categoryType) {
            case OptionCategory.TYPE_RADIO_LIST:
                return VIEW_RADIO;
            case OptionCategory.TYPE_CHECKBOX_LIST:
                return VIEW_CHECK;
            case OptionCategory.TYPE_SINGLE_SWITCH:
                return VIEW_SWITCH;
            case OptionCategory.TYPE_LONG_TEXT:
                return VIEW_LONGTEXT;
            default:
                // STRING_LIST, SINGLE_BUTTON and the chat/comments types all read as a tappable row.
                return VIEW_PLAIN;
        }
    }

    @Override
    public int getItemViewType(int position) {
        return mRows.get(position).viewType;
    }

    @Override
    public int getItemCount() {
        return mRows.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        switch (viewType) {
            case VIEW_HEADER:
                return new HeaderHolder(inflater.inflate(R.layout.item_mobile_option_header, parent, false));
            case VIEW_RADIO:
                return new OptionHolder(inflater.inflate(R.layout.item_mobile_option_radio, parent, false));
            case VIEW_CHECK:
                return new OptionHolder(inflater.inflate(R.layout.item_mobile_option_check, parent, false));
            case VIEW_SWITCH:
                return new OptionHolder(inflater.inflate(R.layout.item_mobile_option_switch, parent, false));
            case VIEW_LONGTEXT:
                return new LongTextHolder(inflater.inflate(R.layout.item_mobile_option_longtext, parent, false));
            default:
                return new OptionHolder(inflater.inflate(R.layout.item_mobile_option_plain, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Row row = mRows.get(position);

        if (holder instanceof HeaderHolder) {
            ((HeaderHolder) holder).bind(row.header);
        } else if (holder instanceof LongTextHolder) {
            ((LongTextHolder) holder).bind(row.item);
        } else {
            ((OptionHolder) holder).bind(row, this);
        }
    }

    /**
     * Applies a selection and repaints the peers a radio group shares state with.
     */
    private void onItemToggled(Row row, boolean selected) {
        row.item.onSelect(selected);

        if (row.viewType == VIEW_RADIO && selected) {
            // Exactly one member of a radio category stays selected.
            for (Row other : mRows) {
                if (other.category == row.category && other.item != null && other.item != row.item) {
                    other.item.onSelect(false);
                }
            }
        }

        notifyDataSetChanged();
    }

    static class HeaderHolder extends RecyclerView.ViewHolder {
        private final TextView mHeader;

        HeaderHolder(View itemView) {
            super(itemView);
            mHeader = itemView.findViewById(R.id.option_header);
            // Announced as a heading so TalkBack users can jump between setting groups.
            ViewCompat.setAccessibilityHeading(mHeader, true);
            itemView.setFocusable(false);
        }

        void bind(CharSequence header) {
            mHeader.setText(header);
        }
    }

    static class LongTextHolder extends RecyclerView.ViewHolder {
        private final TextView mText;

        LongTextHolder(View itemView) {
            super(itemView);
            mText = itemView.findViewById(R.id.option_title);
        }

        void bind(OptionItem item) {
            mText.setText(item.getTitle());
        }
    }

    static class OptionHolder extends RecyclerView.ViewHolder {
        private final TextView mTitle;
        private final TextView mDescription;
        private final CompoundButton mWidget;

        OptionHolder(View itemView) {
            super(itemView);
            mTitle = itemView.findViewById(R.id.option_title);
            mDescription = itemView.findViewById(R.id.option_description);
            View widget = itemView.findViewById(R.id.option_widget);
            mWidget = widget instanceof CompoundButton ? (CompoundButton) widget : null;
        }

        void bind(Row row, OptionsAdapter adapter) {
            OptionItem item = row.item;

            mTitle.setText(item.getTitle());

            CharSequence description = item.getDescription();
            mDescription.setText(description);
            mDescription.setVisibility(TextUtils.isEmpty(description) ? View.GONE : View.VISIBLE);

            if (mWidget != null) {
                mWidget.setChecked(item.isSelected());
            }

            // The row owns the label and the state, so the checkbox itself stays out of the
            // traversal order and TalkBack reads "title, description, checked" once.
            itemView.setContentDescription(
                    TextUtils.isEmpty(description) ? item.getTitle() : item.getTitle() + ". " + description);

            itemView.setOnClickListener(v -> {
                boolean selected = mWidget == null || !mWidget.isChecked();
                adapter.onItemToggled(row, selected);
            });
        }
    }
}
