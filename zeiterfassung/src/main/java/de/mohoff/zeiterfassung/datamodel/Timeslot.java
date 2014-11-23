package de.mohoff.zeiterfassung.datamodel;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;


@DatabaseTable(tableName = "timeslots")//, daoClass = Timeslot.class)
public class Timeslot {
    @DatabaseField(generatedId = true) // autoincrement primary key
    private int _id;
    @DatabaseField(canBeNull = false, unique = true)
    private long starttime; // in seconds
    @DatabaseField
    private long endtime = 0; // in seconds
    @DatabaseField(canBeNull = false)
    private String activity;
    @DatabaseField(canBeNull = false)
    private String location;
    private boolean sealed = false;

    public Timeslot(){}

    public Timeslot(long starttime, String activity, String location){
        this.starttime = starttime;
        this.activity = activity;
        this.location = location;
    }

    public Timeslot(long starttime, long endtime, String activity, String location){
        this.starttime = starttime;
        this.endtime = endtime;
        this.activity = activity;
        this.location = location;
    }

    public int get_id() {
        return _id;
    }

    public void set_id(int _id) {
        this._id = _id;
    }

    public long getStarttime() {
        return starttime;
    }

    public void setStarttime(long starttime) {
        this.starttime = starttime;
    }

    public long getEndtime() {
        return endtime;
    }

    public void setEndtime(long endtime) {
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

    public static String getTimeReadableAll(long millis){
        Date date = new Date(millis);
        DateFormat df;
        df = DateFormat.getDateTimeInstance(/*dateStyle*/ DateFormat.MEDIUM, /*timeStyle*/ DateFormat.SHORT);

        return df.format(date);
    }

    public static String getTimeReadableDate(long millis){
        Date date = new Date(millis);
        DateFormat df;
        df = DateFormat.getDateInstance(DateFormat.MEDIUM);

        return df.format(date);
    }

    public static String getTimeReadableTime(long millis){
        Date date = new Date(millis);
        DateFormat df;
        df = DateFormat.getTimeInstance(DateFormat.SHORT);

        return df.format(date);
    }

    public static String getDurationReadable(long millis1, long millis2){
        long durationInMillis;
        if(millis1 >= millis2){
            durationInMillis = millis1 - millis2;
        } else {
            durationInMillis = millis2 - millis1;
        }

        /*String.format("%d min, %d sec",
                TimeUnit.MILLISECONDS.toMinutes(durationInMillis),
                TimeUnit.MILLISECONDS.toSeconds(durationInMillis) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
        );*/

        long days = durationInMillis/(1000*60*60*24);
        long rest = durationInMillis%(1000*60*60*24);

        long hours = rest/(1000*60*60);
        rest = rest%(1000*60*60);

        long minutes = rest/(1000*60);
        rest = rest%(1000*60);

        //long seconds = rest/(1000);
        //long millis = rest%(1000);

        String output = "";
        if(days != 0){
            output += hours + "d ";
        }
        if(hours != 0){
            output += hours + "h ";
        }
        if(minutes != 0){
            output += minutes + "min";
        }

        return output;
    }

}
