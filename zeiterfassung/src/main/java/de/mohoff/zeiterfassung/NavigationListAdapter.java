package de.mohoff.zeiterfassung;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by TPPOOL01 on 24.11.2014.
 */
public class NavigationListAdapter extends ArrayAdapter<NavigationListItem>{
    private LayoutInflater inflater;
    private NavigationListItem[] items = new NavigationListItem[6];

    public NavigationListAdapter(Context context, int textViewResourceId, NavigationListItem[] items){
        // textViewResourceId actually only dummy so that super(..,..,..) is satisfied
        super(context, textViewResourceId, items);
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent){
        View view = null;
        NavigationListItem menuItem = this.getItem(position);
        if (menuItem.getType() == NavigationListItemLabel.ITEM_TYPE) {
            view = getLabelView(convertView, parent, menuItem);
        }
        else {
            view = getSectionView(convertView, parent, menuItem);
        }
        return view;
    }

    public View getLabelView(View convertView, ViewGroup parentView, NavigationListItem navDrawerItem){

        NavigationListItemLabel menuItem = (NavigationListItemLabel) navDrawerItem ;
        NavigationListItemLabelHolder navigationListItemLabelHolder = null;

        if(convertView == null){
            convertView = inflater.inflate( R.layout.navigation_drawer_list_label, parentView, false);
            ImageView iconView = (ImageView) convertView.findViewById(R.id.navigationListLabelIcon);
            TextView labelView = (TextView) convertView.findViewById(R.id.navigationListLabelText);

            navigationListItemLabelHolder = new NavigationListItemLabelHolder();
            navigationListItemLabelHolder.labelView = labelView ;
            navigationListItemLabelHolder.iconView = iconView ;

            convertView.setTag(navigationListItemLabelHolder);
        }

        if(navigationListItemLabelHolder == null){
            navigationListItemLabelHolder = (NavigationListItemLabelHolder) convertView.getTag();
        }

        navigationListItemLabelHolder.labelView.setText(menuItem.getLabel());
        navigationListItemLabelHolder.iconView.setImageResource(menuItem.getIcon());

        return convertView;
    }

    public View getSectionView(View convertView, ViewGroup parentView, NavigationListItem navDrawerItem){
        NavigationListItemSection menuSection = (NavigationListItemSection) navDrawerItem;
        NavigationListItemSectionHolder navMenuItemHolder = null;

        if(convertView == null){
            convertView = inflater.inflate( R.layout.navigation_drawer_list_section, parentView, false);
            TextView labelView = (TextView) convertView.findViewById(R.id.navigationListSectionText);

            navMenuItemHolder = new NavigationListItemSectionHolder();
            navMenuItemHolder.labelView = labelView;
            convertView.setTag(navMenuItemHolder);
        }

        if(navMenuItemHolder == null){
            navMenuItemHolder = (NavigationListItemSectionHolder) convertView.getTag();
        }

        navMenuItemHolder.labelView.setText(menuSection.getLabel());

        return convertView;
    }

    @Override
    public int getViewTypeCount(){
        return 2;
    }

    @Override
    public int getItemViewType(int position){
        return this.getItem(position).getType();
    }

    @Override
    public boolean isEnabled(int position){
        return getItem(position).isEnabled();
    }

    private static class NavigationListItemLabelHolder{
        private TextView labelView;
        private ImageView iconView;
    }

    private class NavigationListItemSectionHolder{
        private TextView labelView;
    }

}
