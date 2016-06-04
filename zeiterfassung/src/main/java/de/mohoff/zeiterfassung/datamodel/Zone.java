package de.mohoff.zeiterfassung.datamodel;

import com.google.android.gms.maps.model.LatLng;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "zones")
public class Zone {
    // If you change MIN_RADIUS, also change hardcoded strings in 'strings-settings.xml' and
    // 'strings-snacks.xml'.
    public static int MIN_RADIUS = 30; // m

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
    @DatabaseField(canBeNull = false)
    private int color; // e.g. 0xFF000000 as black

    public Zone() {
    }

    public Zone(float latitude, float longitude, int radius, String activityName, String locationName, int color) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.radius = radius;
        this.activityName = activityName;
        this.locationName = locationName;
        this.color = color;
    }

    public Zone(float latitude, float longitude, int radius, String activityName, String locationName) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.radius = radius;
        this.activityName = activityName;
        this.locationName = locationName;
        // TODO: check default color
        this.color = 0; // or some default gray
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

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public LatLng getLatLng() {
        return new LatLng(latitude, longitude);
    }

    public Loc getLoc() {
        return new Loc(latitude, longitude);
    }
}
