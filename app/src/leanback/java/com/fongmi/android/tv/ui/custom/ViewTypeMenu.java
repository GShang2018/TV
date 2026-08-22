package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListPopupWindow;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.ui.base.ViewType;

import java.util.ArrayList;
import java.util.List;

public class ViewTypeMenu {

    public interface OnSelectListener {

        void onSelect(int viewType);
    }

    public static void show(Context context, View anchor, int menuRes, int currentViewType, OnSelectListener listener) {
        List<Item> items = getItems(context, anchor, menuRes);
        BaseAdapter adapter = new BaseAdapter() {

            @Override
            public int getCount() {
                return items.size();
            }

            @Override
            public Object getItem(int position) {
                return items.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = convertView != null ? convertView : LayoutInflater.from(context).inflate(R.layout.menu_item_view_type, parent, false);
                ImageView icon = view.findViewById(R.id.icon);
                TextView title = view.findViewById(R.id.title);
                ImageView check = view.findViewById(R.id.check);
                Item item = items.get(position);
                icon.setImageDrawable(item.icon);
                title.setText(item.title);
                check.setVisibility(item.viewType == currentViewType ? View.VISIBLE : View.GONE);
                return view;
            }
        };
        ListPopupWindow popup = new ListPopupWindow(context);
        popup.setAnchorView(anchor);
        popup.setAdapter(adapter);
        popup.setModal(true);
        popup.setWidth((int) (context.getResources().getDisplayMetrics().density * 220));
        popup.setOnItemClickListener((parent, view, position, id) -> {
            popup.dismiss();
            listener.onSelect(items.get(position).viewType);
        });
        popup.show();
        popup.getListView().requestFocus();
    }

    private static List<Item> getItems(Context context, View anchor, int menuRes) {
        PopupMenu temp = new PopupMenu(context, anchor);
        temp.inflate(menuRes);
        Menu menu = temp.getMenu();
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            int viewType = getViewType(item.getItemId());
            if (viewType == -1) continue;
            items.add(new Item(item.getIcon(), item.getTitle(), viewType));
        }
        return items;
    }

    private static int getViewType(int id) {
        if (id == R.id.view_portrait) return ViewType.PORTRAIT;
        if (id == R.id.view_grid) return ViewType.GRID;
        if (id == R.id.view_list) return ViewType.LIST;
        if (id == R.id.view_config) return ViewType.CONFIG;
        return -1;
    }

    private static class Item {

        final android.graphics.drawable.Drawable icon;
        final CharSequence title;
        final int viewType;

        Item(android.graphics.drawable.Drawable icon, CharSequence title, int viewType) {
            this.icon = icon;
            this.title = title;
            this.viewType = viewType;
        }
    }
}
