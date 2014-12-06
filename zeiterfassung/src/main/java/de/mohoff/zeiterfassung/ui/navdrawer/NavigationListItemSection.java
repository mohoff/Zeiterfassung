package de.mohoff.zeiterfassung.ui.navdrawer;

/**
 * Created by TPPOOL01 on 24.11.2014.
 */
public class NavigationListItemSection implements NavigationListItem {
    public static final int SECTION_TYPE = 0;
    private int id;
    private String label;

    private NavigationListItemSection() {
    }

    public static NavigationListItemSection create(int id, String label) {
        NavigationListItemSection section = new NavigationListItemSection();
        section.setId(id);
        section.setLabel(label);
        return section;
    }

    @Override
    public int getType() {
        return SECTION_TYPE;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public boolean updateActionBarTitle() {
        return false;
    }
}

