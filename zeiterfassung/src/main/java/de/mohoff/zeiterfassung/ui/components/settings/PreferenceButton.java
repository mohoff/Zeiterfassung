package de.mohoff.zeiterfassung.ui.components.settings;

import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

/**
 * Created by moo on 11/19/15.
 */
public class PreferenceButton extends Preference {
    public PreferenceButton(Context context) {
        super(context);
    }

    public PreferenceButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PreferenceButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        View preferenceView = super.onCreateView(parent);

        setOnClickListener(preferenceView);

        return preferenceView;
    }

    private void setOnClickListener(View preferenceView) {
        if (preferenceView != null && preferenceView instanceof ViewGroup) {
            ViewGroup widgetFrameView = ((ViewGroup)preferenceView.findViewById(android.R.id.widget_frame));
            if (widgetFrameView != null) {
                // find the button
                Button button = null;
                int count = widgetFrameView.getChildCount();
                for (int i = 0; i < count; i++) {
                    View view = widgetFrameView.getChildAt(i);
                    if (view instanceof Button) {
                        button = (Button)view;
                        break;
                    }
                }

                if (button != null) {
                    // set the OnClickListener
                    button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // do whatever you want to do
                        }
                    });
                }
            }
        }
    }
}
