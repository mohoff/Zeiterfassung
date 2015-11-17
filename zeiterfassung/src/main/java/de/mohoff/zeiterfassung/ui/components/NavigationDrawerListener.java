package de.mohoff.zeiterfassung.ui.components;

import android.view.View;

/**
 * Created by TPPOOL01 on 26.11.2014.
 */
public interface NavigationDrawerListener {
    public void StartButtonClicked();
    public void StopButtonClicked();
    public void onItemSelected(View view, int position);
}
