package de.mohoff.zeiterfassung.ui.components.settings;

import android.annotation.TargetApi;
import android.content.Context;
import android.preference.PreferenceCategory;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import de.mohoff.zeiterfassung.R;

/**
 * Created by moo on 11/19/15.
 */
public class PreferenceCategoryDangerZone extends PreferenceCategory {
    //TextView titleView;

    /*@Override
    protected View onCreateView(ViewGroup parent) {
        View view = super.onCreateView(parent);
        //titleView = (TextView) view.findViewById(android.R.id.title);
        //titleView.setTextColor(getContext().getResources().getColor(R.color.red));
        return view;
    }*/

    public PreferenceCategoryDangerZone(Context context) {
        super(context);
    }

    public PreferenceCategoryDangerZone(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PreferenceCategoryDangerZone(Context context, AttributeSet attrs,
                                        int defStyle) {
        super(context, attrs, defStyle);
    }

    @TargetApi(21)
    public PreferenceCategoryDangerZone(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        TextView titleView = (TextView) view.findViewById(android.R.id.title);
        titleView.setTextColor(getContext().getResources().getColor(R.color.red));
    }
}
