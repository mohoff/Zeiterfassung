package de.mohoff.zeiterfassung.ui.navdrawer;

import android.content.Context;
import android.view.View;

/**
 * Created by TPPOOL01 on 24.11.2014.
 */
public class NavigationListItemLabel implements NavigationListItem, View.OnClickListener{

    public static final int ITEM_TYPE = 1;

    private int id;
    private String label;
    private int icon;
    private boolean updateActionBarTitle;

    private NavigationListItemLabel() {
    }

    public static NavigationListItemLabel create(int id, String label, String icon, boolean updateActionBarTitle, Context context) {
        NavigationListItemLabel item = new NavigationListItemLabel();
        item.setId(id);
        item.setLabel(label);
        item.setIcon(context.getResources().getIdentifier(icon, "drawable", context.getPackageName()));
        item.setUpdateActionBarTitle(updateActionBarTitle);
        return item;
    }

    @Override
    public int getType() {
        return ITEM_TYPE;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public int getIcon() {
        return icon;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean updateActionBarTitle() {
        return this.updateActionBarTitle;
    }

    public void setUpdateActionBarTitle(boolean updateActionBarTitle) {
        this.updateActionBarTitle = updateActionBarTitle;
    }

    @Override
    public void onClick(View v) {

    }
}
