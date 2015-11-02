package de.mohoff.zeiterfassung.datamodel;

import android.graphics.Color;
import com.google.android.gms.maps.model.LatLng;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "zones")//, daoClass = Zone.class)
public class Zone {
    //private LatLng center; // not usable coz we want to store position in db, so we fallback to 2 floats/doubles

    @DatabaseField(generatedId = true) // autoincrement primary key
    private int _id;
    @DatabaseField(canBeNull = false)
    private float latitude;
    @DatabaseField(canBeNull = false)
    private float longitude;
    @DatabaseField(canBeNull = false)
    private int radius; // meters
    @DatabaseField(canBeNull = false, uniqueCombo = true)
    private String activityName;
    @DatabaseField(canBeNull = false, uniqueCombo = true)
    private String locationName;
    private Color colorScheme;

    public Zone(){}

    public Zone(float latitude, float longitude, int radius, String activityName, String locationName, Color colorScheme) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.radius = radius;
        this.activityName = activityName;
        this.locationName = locationName;
        this.colorScheme = colorScheme;
    }

    public Zone(float latitude, float longitude, int radius, String activityName, String locationName) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.radius = radius;
        this.activityName = activityName;
        this.locationName = locationName;
        this.colorScheme = null; // or some default gray
    }

    public int get_id() {
        return _id;
    }

    public void set_id(int _id) {
        this._id = _id;
    }

    public float getLatitude() {
        return latitude;
    }

    public void setLatitude(float latitude) {
        this.latitude = latitude;
    }

    public float getLongitude() {
        return longitude;
    }

    public void setLongitude(float longitude) {
        this.longitude = longitude;
    }

    public int getRadius() {
        return radius;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public String getActivityName() {
        return activityName;
    }

    public void setActivityName(String activityName) {
        this.activityName = activityName;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public Color getColorScheme() {
        return colorScheme;
    }

    public void setColorScheme(Color colorScheme) {
        this.colorScheme = colorScheme;
    }
    //float dataQuality; // indicator for accuracy ratings over time in this target location area




}
