package de.mohoff.zeiterfassung.locationservice;

/**
 * Created by moo on 10/19/15.
 */
public interface TimeslotEventListener {
    void onNewTimeslot(int id);

    void onTimeslotSealed(int id);
}
