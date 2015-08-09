package de.mohoff.zeiterfassung.ui.navdrawer;

import android.content.Context;

import de.mohoff.zeiterfassung.R;

/**
 * Created by TPPOOL01 on 24.11.2014.
 */
public class NavigationListItemLabelService implements NavigationListItem{

    public static final int ITEM_TYPE = 2;

    private int id;
    private boolean isServiceRunning = false;
    private int icon;
    private String label;
    private String buttonLabelStart;
    private String buttonLabelStop;
    private int colorServiceIsRunning;
    private int colorServiceIsStopped;
    private int colorServiceActionDisabled;
    private int textColorDisabled;

    private int textColorEnabled;

    private NavigationListItemLabelService() {
    }

    public static NavigationListItemLabelService create(int id, String label, String icon, Context context, boolean isServiceRunning){
        NavigationListItemLabelService item = new NavigationListItemLabelService();
        item.setId(id);
        item.setServiceRunning(isServiceRunning);
        item.setIcon(context.getResources().getIdentifier(icon, "drawable", context.getPackageName()));
        item.setLabel(label);

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

    public boolean isServiceRunning() {
        return isServiceRunning;
    }

    public void setServiceRunning(boolean isServiceRunning) {
        this.isServiceRunning = isServiceRunning;
    }

    public int getIcon() {
        return icon;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }

    public String getButtonLabelStart() {
        return buttonLabelStart;
    }

    public void setButtonLabelStart(String buttonLabelStart) {
        this.buttonLabelStart = buttonLabelStart;
    }

    public String getButtonLabelStop() {
        return buttonLabelStop;
    }

    public void setButtonLabelStop(String buttonLabelStop) {
        this.buttonLabelStop = buttonLabelStop;
    }

    public int getColorServiceIsRunning() {
        return colorServiceIsRunning;
    }

    public void setColorServiceIsRunning(int colorServiceIsRunning) {
        this.colorServiceIsRunning = colorServiceIsRunning;
    }

    public int getColorServiceIsStopped() {
        return colorServiceIsStopped;
    }

    public void setColorServiceIsStopped(int colorServiceIsStopped) {
        this.colorServiceIsStopped = colorServiceIsStopped;
    }

    public int getColorServiceActionDisabled() {
        return colorServiceActionDisabled;
    }

    public void setColorServiceActionDisabled(int colorServiceActionDisabled) {
        this.colorServiceActionDisabled = colorServiceActionDisabled;
    }

    public int getTextColorDisabled() {
        return textColorDisabled;
    }

    public void setTextColorDisabled(int textColorDisabled) {
        this.textColorDisabled = textColorDisabled;
    }

    public int getTextColorEnabled() {
        return textColorEnabled;
    }

    public void setTextColorEnabled(int textColorEnabled) {
        this.textColorEnabled = textColorEnabled;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean updateActionBarTitle() {
        return false;
    }

   /* @Override
    public boolean updateActionBarTitle() {
        return this.updateActionBarTitle;
    }

    public void setUpdateActionBarTitle(boolean updateActionBarTitle) {
        this.updateActionBarTitle = updateActionBarTitle;
    }*/
}
