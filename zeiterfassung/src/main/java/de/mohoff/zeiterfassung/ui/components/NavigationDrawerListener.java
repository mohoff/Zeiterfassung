package de.mohoff.zeiterfassung.ui.components;

import android.view.View;

/**
 * Created by TPPOOL01 on 26.11.2014.
 */
public interface NavigationDrawerListener {
    void StartButtonClicked();

    void StopButtonClicked();

    void onItemSelected(View view, int position);
}
