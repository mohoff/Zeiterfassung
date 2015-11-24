package de.mohoff.zeiterfassung.datamodel;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.ArrayList;


@DatabaseTable(tableName = "locdump")
public class Loc implements Parcelable{

    @DatabaseField(generatedId = true) // autoincrement primary key
    private int _id;
    @DatabaseField(canBeNull = false)
    private double latitude;            // mandatory
    @DatabaseField(canBeNull = false)
    private double longitude;           // mandatory
    @DatabaseField(canBeNull = false,  unique = true)
    private long timestampInMillis;     // mandatory
    @DatabaseField
    private double accuracy;
    private double accuracyMultiplier;
    @DatabaseField
    private int altitude;
    @DatabaseField
    private int speed;
    private boolean isRealUpdate = true;

    public boolean isRealUpdate() {
        return isRealUpdate;
    }

    public void setIsRealUpdate(boolean isTrue){
        isRealUpdate = isTrue;
    }

    public Loc(){}

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

    public LatLng getLatLng(){
        return new LatLng(latitude, longitude);
    }

    static public int distanceTo(Loc loc1, Loc loc2){
        if(loc1 == null || loc2 == null) return 0;

        int earthRadius = 6371; // km

        double loc1LatInRadians = Math.toRadians(loc1.getLatitude());
        double loc2LatInRadians = Math.toRadians(loc2.getLatitude());
        double latDiffInRadians = Math.toRadians(loc1.getLatitude() - loc2.getLatitude());
        double lngDiffInRadians = Math.toRadians(loc1.getLongitude() - loc2.getLongitude());

        double a = Math.sin(latDiffInRadians/2) * Math.sin(latDiffInRadians/2) +
                Math.cos(loc1LatInRadians) * Math.cos(loc2LatInRadians) *
                        Math.sin(lngDiffInRadians/2) * Math.sin(lngDiffInRadians/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double distance = earthRadius * c * 1000;

        return (int) distance;
    }

    public int distanceTo(Loc targetLoc){
        if(targetLoc == null) return 0;

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

    public static Loc convertLocationToLoc(Location loc) {
        Loc result;
        long timeToPersist = System.currentTimeMillis();
        result = new Loc(loc.getLatitude(), loc.getLongitude(), timeToPersist);
        if (loc.hasAccuracy() && (loc.getAccuracy() > 0.0)) {
            result.setAccuracy(loc.getAccuracy());
            //result.setAccuracyMultiplier(LocationCache.getNormedAccuracyMultiplier(loc.getAccuracy()));
        }
        if (loc.hasAltitude()) {
            result.setAltitude((int) loc.getAltitude());
        }
        if (loc.hasSpeed()) {
            result.setSpeed((int) loc.getSpeed());
        }
        return result;
    }

    public static Loc convertLatLngToLoc(LatLng latLng) {
        Loc result = null;
        if ((latLng.latitude != 0) && (latLng.longitude != 0)) {
            long currentTimeInMinutes = (System.currentTimeMillis());
            result = new Loc(latLng.latitude, latLng.longitude, currentTimeInMinutes);
        }
        return result;
    }

    public boolean isNotOlderThan(long millisInPast){
        return System.currentTimeMillis()-millisInPast <= getTimestampInMillis();
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


    // Implemented methods of "Parcelable"

    private Loc(Parcel in) {
        latitude = in.readDouble();
        longitude = in.readDouble();
        timestampInMillis = in.readLong();
        accuracy = in.readDouble();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeDouble(latitude);
        out.writeDouble(longitude);
        out.writeLong(timestampInMillis);
        out.writeDouble(accuracy);
    }

    public static final Parcelable.Creator<Loc> CREATOR = new Parcelable.Creator<Loc>() {
        public Loc createFromParcel(Parcel in) {
            return new Loc(in);
        }

        public Loc[] newArray(int size) {
            return new Loc[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Loc loc = (Loc) o;

        if (Double.compare(loc.latitude, latitude) != 0) return false;
        return Double.compare(loc.longitude, longitude) == 0;
    }
}
