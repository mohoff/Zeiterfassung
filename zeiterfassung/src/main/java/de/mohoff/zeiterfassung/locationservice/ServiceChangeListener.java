package de.mohoff.zeiterfassung.locationservice;

/**
 * Created by moo on 10/24/15.
 */
public interface ServiceChangeListener {
    void onServiceStatusEvent(boolean isRunning);
}
