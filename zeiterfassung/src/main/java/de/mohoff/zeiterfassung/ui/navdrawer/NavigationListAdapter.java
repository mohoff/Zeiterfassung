package de.mohoff.zeiterfassung.ui.navdrawer;

import android.content.Context;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import de.mohoff.zeiterfassung.R;

/**
 * Created by TPPOOL01 on 24.11.2014.
 */
public class NavigationListAdapter extends ArrayAdapter<NavigationListItem>{
    private LayoutInflater inflater;
    private Context context;
    private NavigationListItem[] items = new NavigationListItem[6];

    private NavigationDrawerListener navDrawerListener;

    public void setTheListener(NavigationDrawerListener listen) {
        navDrawerListener = listen;
    }

    public NavigationListAdapter(Context context, int textViewResourceId, NavigationListItem[] items){
        // textViewResourceId actually only dummy so that super(..,..,..) is satisfied
        super(context, textViewResourceId, items);
        this.context = context;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent){
        View view = null;
        NavigationListItem menuItem = this.getItem(position);

        if (menuItem.getType() == NavigationListItemLabel.ITEM_TYPE){
            view = getLabelView(convertView, parent, menuItem);
        }
        else if (menuItem.getType() == NavigationListItemSection.SECTION_TYPE){
            view = getSectionView(convertView, parent, menuItem);
        } else {
            view = getLabelServiceView(convertView, parent, menuItem);
        }

        return view;
    }

    public View getLabelView(View convertView, ViewGroup parentView, NavigationListItem navDrawerItem){
        NavigationListItemLabel menuItem = (NavigationListItemLabel) navDrawerItem;
        NavigationListItemLabelHolder navigationListItemLabelHolder = null;

        if(convertView == null){
            convertView = inflater.inflate( R.layout.navigation_drawer_list_item, parentView, false);
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

    public View getLabelServiceView(View convertView, ViewGroup parentView, NavigationListItem navDrawerItem){
        final NavigationListItemLabelService menuItem = (NavigationListItemLabelService) navDrawerItem ;
        NavigationListItemLabelServiceHolder navHolder = null;

        if(convertView == null){
            convertView = inflater.inflate( R.layout.navigation_drawer_list_label_service, parentView, false);
            ImageView iconView = (ImageView) convertView.findViewById(R.id.navigationListLabelIcon);
            TextView labelView = (TextView) convertView.findViewById(R.id.navigationListLabelText);
            Button buttonStartView = (Button) convertView.findViewById(R.id.navigationListLabelButtonStart);
            Button buttonStopView = (Button) convertView.findViewById(R.id.navigationListLabelButtonStop);

            navHolder = new NavigationListItemLabelServiceHolder();
            navHolder.labelView = labelView;
            navHolder.iconView = iconView;
            navHolder.buttonStart = buttonStartView;
            navHolder.buttonStop = buttonStopView;

            convertView.setTag(navHolder);
        }
        if(navHolder == null) {
            navHolder = (NavigationListItemLabelServiceHolder) convertView.getTag();
        }

        navHolder.labelView.setText(menuItem.getLabel());
        navHolder.iconView.setImageResource(menuItem.getIcon());

        if(menuItem.isServiceRunning()){
            // start button
            navHolder.buttonStart.getBackground().setColorFilter(context.getResources().getColor(R.color.grey_25), PorterDuff.Mode.MULTIPLY);
            navHolder.buttonStart.setEnabled(false);
            navHolder.buttonStart.setTextColor(context.getResources().getColor(R.color.white)); // need to setIsRunning text color explicitly after setEnabled(false). Else text color gets grey somehow
            navHolder.buttonStart.setOnClickListener(null);
            // stop button
            navHolder.buttonStop.getBackground().setColorFilter(context.getResources().getColor(R.color.greenish), PorterDuff.Mode.MULTIPLY);
            navHolder.buttonStop.setEnabled(true);
            navHolder.buttonStop.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // stop location service
                    if (navDrawerListener != null) {
                        navDrawerListener.StopButtonClicked();
                    }
                    menuItem.setServiceRunning(false);
                    notifyDataSetChanged();
                    //getLabelServiceView(finalConvertView, finalParentView, menuItem); // redraw View
                }
            });
        } else {
            // start button
            navHolder.buttonStart.getBackground().setColorFilter(context.getResources().getColor(R.color.greenish), PorterDuff.Mode.MULTIPLY);
            navHolder.buttonStart.setEnabled(true);
            navHolder.buttonStart.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // start and connect to location service
                    if (navDrawerListener != null) {
                        navDrawerListener.StartButtonClicked();
                    }
                    menuItem.setServiceRunning(true);
                    notifyDataSetChanged();
                    //getLabelServiceView(finalConvertView, finalParentView, menuItem); // redraw View
                }
            });
            // stop button
            navHolder.buttonStop.getBackground().setColorFilter(context.getResources().getColor(R.color.grey_25), PorterDuff.Mode.MULTIPLY);
            navHolder.buttonStop.setEnabled(false);
            navHolder.buttonStop.setTextColor(context.getResources().getColor(R.color.white)); // need to setIsRunning text color explicitly after setEnabled(false). Else text color gets grey somehow
            navHolder.buttonStop.setOnClickListener(null);
        }

        return convertView;
    }

    public View getSectionView(View convertView, ViewGroup parentView, NavigationListItem navDrawerItem){
        NavigationListItemSection menuSection = (NavigationListItemSection) navDrawerItem;
        NavigationListItemSectionHolder navMenuItemHolder = null;

        if(convertView == null){
            convertView = inflater.inflate( R.layout.navigation_drawer_list_headline, parentView, false);
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
        return 3;
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

    private static class NavigationListItemLabelServiceHolder{
        private TextView labelView;
        private ImageView iconView;
        private Button buttonStart;
        private Button buttonStop;
    }

    private class NavigationListItemSectionHolder{
        private TextView labelView;
    }

}
