package de.mohoff.zeiterfassung.locationservice;

import de.mohoff.zeiterfassung.datamodel.Loc;

/**
 * Created by Moritz on 22.09.2014.
 */
public interface LocationChangeListener {
    void onNewLocation(Loc loc);
}
