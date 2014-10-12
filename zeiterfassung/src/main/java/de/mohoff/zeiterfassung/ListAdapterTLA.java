package de.mohoff.zeiterfassung;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import de.mohoff.zeiterfassung.datamodel.TargetLocationArea;

import java.util.List;


public class ListAdapterTLA extends ArrayAdapter{

    private Context context;
    List<TargetLocationArea> tlas;


    public ListAdapterTLA(Context context, List<TargetLocationArea> tlas) {
        super(context, R.layout.activity_manage_tla_views);
        this.context = context;
        this.tlas = tlas;
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return tlas.size();
    }

    @Override
    public Object getItem(int position) {
        return tlas.get(position);
    }

    @Override
    public boolean isEnabled(int position) {
        return super.isEnabled(position);
    }

    public static class ViewHolder {
        public ImageView iconView;
        public TextView locationView;
        public TextView activityView;
        public TextView radiusView;
        public TextView latView;
        public TextView lngView;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TargetLocationArea tla = tlas.get(position);
        View view = null;

        if (convertView != null) {
            view = convertView;
        } else {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.activity_manage_tla_views, parent, false);

            ImageView icon_view = (ImageView) view.findViewById(R.id.icon);
            TextView locationTV = (TextView) view.findViewById(R.id.locationName);
            TextView activityTV = (TextView) view.findViewById(R.id.activityName);
            TextView radiusTV = (TextView) view.findViewById(R.id.radius);
            TextView latTV = (TextView) view.findViewById(R.id.lat);
            TextView lngTV = (TextView) view.findViewById(R.id.lng);
            ViewHolder holder = new ViewHolder();
            holder.iconView = icon_view;
            holder.locationView = locationTV;
            holder.activityView = activityTV;
            holder.radiusView = radiusTV;
            holder.latView = latTV;
            holder.lngView = lngTV;
            view.setTag(holder);
        }
        ViewHolder tag = (ViewHolder) view.getTag();

        tag.activityView.setText(tla.getActivityName());
        tag.locationView.setText(tla.getLocationName());
        tag.radiusView.setText("Radius: " + tla.getRadius());
        tag.latView.setText(String.valueOf("lat: " + tla.getLatitude()));
        tag.lngView.setText(String.valueOf("lng: " + tla.getLongitude()));

        // tint background color of list elements
        //view.setBackgroundResource(R.drawable.background_gradient_orange);

        return view;
    }


}

