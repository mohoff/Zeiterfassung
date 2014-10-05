package de.mohoff.zeiterfassung.datamodel;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "timeslots")//, daoClass = Timeslot.class)
public class Timeslot {
    @DatabaseField(generatedId = true) // autoincrement primary key
    private int _id;
    @DatabaseField(canBeNull = false, unique = true)
    private int starttime; // in seconds
    @DatabaseField
    private int endtime; // in seconds
    @DatabaseField(canBeNull = false)
    private String activity;
    @DatabaseField(canBeNull = false)
    private String location;
    private boolean sealed = false;

    public Timeslot(){}

    public Timeslot(int starttime, String activity, String location){
        this.starttime = starttime;
        this.activity = activity;
        this.location = location;
    }

    public int get_id() {
        return _id;
    }

    public void set_id(int _id) {
        this._id = _id;
    }

    public int getStarttime() {
        return starttime;
    }

    public void setStarttime(int starttime) {
        this.starttime = starttime;
    }

    public int getEndtime() {
        return endtime;
    }

    public void setEndtime(int endtime) {
        this.endtime = endtime;
    }

    public String getActivity() {
        return activity;
    }

    public void setActivity(String activity) {
        this.activity = activity;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }




}
