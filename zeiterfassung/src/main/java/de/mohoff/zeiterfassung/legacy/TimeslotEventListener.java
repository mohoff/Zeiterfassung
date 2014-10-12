package de.mohoff.zeiterfassung.legacy;

import de.mohoff.zeiterfassung.datamodel.TargetLocationArea;

/**
 * Created by Moritz on 06.10.2014.
 */
public interface TimeslotEventListener {
    public void timeslotStartedEvent(TargetLocationArea tla, long timestampToPersist);
    public void timeslotFinishedEvent(TargetLocationArea tla, long timestampToPersist);
}
