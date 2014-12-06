package de.mohoff.zeiterfassung.datamodel;

public class Loc {

    private double latitude;            // mandatory
    private double longitude;           // mandatory
    private long timestampInMillis;     // mandatory
    private double accuracy;
    private double accuracyMultiplier;
    private int altitude;
    private int speed;

    public Loc(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Loc(double latitude, double longitude, double accuracy) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracy = accuracy;
    }

    public Loc(double latitude, double longitude, long timestampInMillis) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestampInMillis = timestampInMillis;
    }

    public Loc(double latitude, double longitude, long timestampInMillis, double accuracyMultiplier) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestampInMillis = timestampInMillis;
        this.accuracyMultiplier = accuracyMultiplier;
    }

    public Loc(double latitude, double longitude, long timestampInMillis, double accuracyMultiplier, int altitude, int speed) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestampInMillis = timestampInMillis;
        this.accuracyMultiplier = accuracyMultiplier;
        this.altitude = altitude;
        this.speed = speed;
    }

    static public int distanceTo(Loc loc1, Loc loc2){
        int earthRadius = 6371; // km

        double loc1LatInRadians = Math.toRadians(loc1.getLatitude());
        double loc2LatInRadians = Math.toRadians(loc2.getLatitude());
        double latDiffInRadians = Math.toRadians(loc1.getLatitude() - loc2.getLatitude());
        double lngDiffInRadians = Math.toRadians(loc1.getLongitude() - loc2.getLongitude());

        double a = Math.sin(latDiffInRadians/2) * Math.sin(latDiffInRadians/2) +
                Math.cos(loc1LatInRadians) * Math.cos(loc2LatInRadians) *
                        Math.sin(lngDiffInRadians/2) * Math.sin(lngDiffInRadians/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double distance = earthRadius * c;

        return (int) distance;
    }

    public int distanceTo(Loc targetLoc){
        int earthRadius = 6371; // km

        double loc1LatInRadians = Math.toRadians(this.getLatitude());
        double loc2LatInRadians = Math.toRadians(targetLoc.getLatitude());
        double latDiffInRadians = Math.toRadians(targetLoc.getLatitude() - this.getLatitude());
        double lngDiffInRadians = Math.toRadians(targetLoc.getLongitude() - this.getLongitude());

        double a = Math.sin(latDiffInRadians/2) * Math.sin(latDiffInRadians/2) +
                Math.cos(loc1LatInRadians) * Math.cos(loc2LatInRadians) *
                        Math.sin(lngDiffInRadians/2) * Math.sin(lngDiffInRadians/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double distance = earthRadius * c * 1000;

        return (int) distance; // * 1000 for km?
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(double accuracy) {
        this.accuracy = accuracy;
    }

    public double getAccuracyMultiplier() {
        return accuracyMultiplier;
    }

    public void setAccuracyMultiplier(double accuracyMultiplier) {
        this.accuracyMultiplier = accuracyMultiplier;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public int getAltitude() {
        return altitude;
    }

    public void setAltitude(int altitude) {
        this.altitude = altitude;
    }

    public long getTimestampInMillis() {
        return timestampInMillis;
    }

    public void setTimestampInMillis(long timestampInMinutes) {
        this.timestampInMillis = timestampInMillis;
    }


}
