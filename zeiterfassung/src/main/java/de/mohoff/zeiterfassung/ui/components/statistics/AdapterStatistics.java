package de.mohoff.zeiterfassung.ui.components.statistics;

import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import de.mohoff.zeiterfassung.R;
import de.mohoff.zeiterfassung.datamodel.Stat;

/**
 * Created by moo on 11/1/15.
 */
public class AdapterStatistics extends RecyclerView.Adapter<AdapterStatistics.ViewHolder> {
    private List<Stat> data;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView headline;
        public TextView content;
        public ViewHolder(View v) {
            super(v);
            headline = (TextView) v.findViewById(R.id.headline);
            content = (TextView) v.findViewById(R.id.content);
        }
    }

    public AdapterStatistics(List<Stat> data) {
        this.data = data;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public AdapterStatistics.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                   int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_statistics_element, parent, false);

        return new ViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Stat stat = data.get(position);
        holder.headline.setText(stat.getDisplayString());
        holder.content.setText(stat.getValue());
        if(stat.getValue().equals("n.a.")){
            holder.content.setTypeface(null, Typeface.ITALIC);
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return data.size();
    }
}
