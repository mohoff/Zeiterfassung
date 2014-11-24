package de.mohoff.zeiterfassung;

/**
 * Created by TPPOOL01 on 24.11.2014.
 */
public interface NavigationListItem {
    public int getId();
    public String getLabel();
    public int getType();
    public boolean isEnabled();
    public boolean updateActionBarTitle();
}
