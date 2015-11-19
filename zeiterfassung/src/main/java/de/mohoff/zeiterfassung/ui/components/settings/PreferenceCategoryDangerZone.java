package de.mohoff.zeiterfassung.ui.components.settings;

import android.content.Context;
import android.graphics.Color;
import android.preference.PreferenceCategory;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import de.mohoff.zeiterfassung.R;

/**
 * Created by moo on 11/19/15.
 */
public class PreferenceCategoryDangerZone extends PreferenceCategory {
    Context context;

    public PreferenceCategoryDangerZone(Context context) {
        super(context);
        this.context = context;
    }

    public PreferenceCategoryDangerZone(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    public PreferenceCategoryDangerZone(Context context, AttributeSet attrs,
                                int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        TextView titleView = (TextView) view.findViewById(android.R.id.title);
        titleView.setTextColor(context.getResources().getColor(R.color.red));
    }
}
