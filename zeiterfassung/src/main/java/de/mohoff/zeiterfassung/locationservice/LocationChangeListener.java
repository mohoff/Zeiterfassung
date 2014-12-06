package de.mohoff.zeiterfassung.locationservice;

import android.location.Location;

/**
 * Created by Moritz on 22.09.2014.
 */
public interface LocationChangeListener {
    public void handleLocationUpdate(Location loc);
}
