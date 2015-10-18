package de.mohoff.zeiterfassung.datamodel;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
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

    private int dayInMillis = 1000 * 60 * 60 * 24;

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

    private String getReadableDate(long time){
        if (isSameDay(System.currentTimeMillis(), time)){
            return "Today";
        }
        if (isSameDay(System.currentTimeMillis() - dayInMillis, time)){
            return "Yesterday";
        }
        return new SimpleDateFormat("dd.MM.yyyy").format(new Date(time));
    }

    private String getReadableTime(long time){
        return new SimpleDateFormat("HH:mm").format(new Date(time));
    }

    public String getReadableStartDate(){
        return getReadableDate(this.starttime);
    }

    public String getReadableStartTime(){
        return getReadableTime(this.starttime);
    }

    public String getReadableEndDate(){
        if(endtime != 0){
            return getReadableDate(this.endtime);
        } else {
            return "pending";
        }
    }

    public String getReadableEndTime(){
        if(endtime != 0){
            return getReadableTime(this.endtime);
        } else {
            return "pending";
        }
    }

    public String getReadableDuration(){
        long minutesStart = this.starttime/(1000*60);
        long minutesEnd;
        long durationInMinutes;

        if(this.endtime == 0){
            minutesEnd = System.currentTimeMillis()/(1000*60);
        } else {
            minutesEnd = this.endtime/(1000*60);
        }
        durationInMinutes = minutesEnd - minutesStart;

        // split time into days, hours and minutes
        long days = durationInMinutes/(60*24);
        long rest = durationInMinutes%(60*24);
        long hours = rest/60;
        rest = rest%60;
        long minutes = rest;

        // generate output string
        String output = "";
        if(days != 0){
            output += days + " d";
        }
        if(hours != 0){
            if(!output.equals("")){
                output += "\n";
            }
            output += hours + " h";
        }
        if(minutes != 0){
            if(!output.equals("")){
                output += "\n";
            }
            output += minutes + " min";
        }

        return output;
    }

    public static boolean isSameDay(long timestamp1, long timestamp2){
        Date startDate = new Date(timestamp1);
        Date endDate = new Date(timestamp2);

        Calendar cal = Calendar.getInstance();
        cal.setTime(startDate);
        int startDay = cal.get(Calendar.DAY_OF_MONTH);
        cal.setTime(endDate);
        int endDay = cal.get(Calendar.DAY_OF_MONTH);

        return startDay == endDay;
    }

    public static String getDurationReadableGeneric(long millis1, long millis2){
        long durationInMinutes;
        long minutes1 = millis1/(1000*60);
        long minutes2 = millis2/(1000*60);
        if(minutes1 >= minutes2){
            durationInMinutes = minutes1 - minutes2;
        } else {
            durationInMinutes = minutes2 - minutes1;
        }

        long days = durationInMinutes/(60*24);
        long rest = durationInMinutes%(60*24);
        long hours = rest/60;
        rest = rest%60;
        long minutes = rest;

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
