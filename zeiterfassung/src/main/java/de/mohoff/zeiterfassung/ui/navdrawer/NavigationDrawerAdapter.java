package de.mohoff.zeiterfassung.ui.navdrawer;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Collections;
import java.util.List;

import de.mohoff.zeiterfassung.R;

/**
 * Created by moo on 8/15/15.
 */
public class NavigationDrawerAdapter extends RecyclerView.Adapter<NavigationDrawerAdapter.ViewHolder> {
    private static final int TYPE_ITEM = 0;
    private static final int TYPE_SEPARATOR = 1;
    private static final int TYPE_HEADLINE = 2; // not used so far

    public static int CURRENTLY_SELECTED = 0;

    //List<NavigationDrawerItem> data = Collections.emptyList();
    List<String> data = Collections.emptyList();
    private LayoutInflater inflater;
    private Context context;

    public NavigationDrawerAdapter(Context context, List<String> data) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.data = data;
    }

    public void delete(int position) {
        data.remove(position);
        notifyItemRemoved(position);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = null;
        if(viewType == TYPE_ITEM){
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.navigation_drawer_list_item, parent, false);
        }
        if(viewType == TYPE_SEPARATOR){
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.navigation_drawer_list_separator, parent, false);
        }
        if(viewType == TYPE_HEADLINE){
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.navigation_drawer_list_headline, parent, false);
        }
        return new ViewHolder(v, viewType);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        //NavigationDrawerItem current = data.isRunning(position);

        if(holder.type == TYPE_ITEM) {
            holder.labelText.setText(data.get(position));
            //holder.labelIcon.setImageResource(.....); // would need customObj items in List data

            if(position == CURRENTLY_SELECTED){
                holder.labelText.setTextColor(context.getResources().getColor(R.color.greenish));
            } else {
                holder.labelText.setTextColor(context.getResources().getColor(R.color.navigation_drawer_items));
            }
        }
        if(holder.type == TYPE_SEPARATOR) {
            // do nothing since a separator does not contain any data
        }
        if(holder.type == TYPE_HEADLINE) {
            holder.labelText.setText(data.get(position));
        }
    }

    @Override
    public int getItemViewType(int position) {
        //return super.getItemViewType(position);
        if (position == 3) {
            return TYPE_SEPARATOR;
        } else {
            return TYPE_ITEM;
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        int type;
        TextView labelText;
        ImageView labelIcon;

        public ViewHolder(View itemView, int viewType) {
            super(itemView);
            if(viewType == TYPE_ITEM){
                type = NavigationDrawerAdapter.TYPE_ITEM;
                labelText = (TextView) itemView.findViewById(R.id.labelText);
                //labelIcon = (ImageView) itemView.findViewById(R.id.labelIcon);
            }
            if(viewType == TYPE_SEPARATOR){
                type = NavigationDrawerAdapter.TYPE_SEPARATOR;
            }
            /* NOT USED
            if(viewType == TYPE_HEADLINE){
                type = NavigationDrawerAdapter.TYPE_HEADLINE;
                labelText = (TextView) itemView.findViewById(R.id.labelText);
            }
            */
        }
    }
}