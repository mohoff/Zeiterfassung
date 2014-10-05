package de.mohoff.zeiterfassung.datamodel;

public class LocPersist extends Loc {

    public LocPersist(double latitude, double longitude, int timestampInMinutes) {
        super(latitude, longitude, timestampInMinutes);
    }

    public LocPersist(double latitude, double longitude, int timestampInMinutes, double accuracyPenalty) {
        super(latitude, longitude, timestampInMinutes, accuracyPenalty);
    }

    public LocPersist(double latitude, double longitude, int timestampInMinutes, double accuracyPenalty, int altitude, int speed) {
        super(latitude, longitude, timestampInMinutes, accuracyPenalty, altitude, speed);
    }
}