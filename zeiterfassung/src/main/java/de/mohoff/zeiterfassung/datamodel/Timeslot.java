package de.mohoff.zeiterfassung.datamodel;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;


@DatabaseTable(tableName = "timeslots")//, daoClass = Timeslot.class)
public class Timeslot {
    @DatabaseField(generatedId = true) // autoincrement primary key
    private int _id;
    @DatabaseField(canBeNull = false, unique = true)
    private long starttime; // in seconds
    // TODO: Set ..IsVague flags to 'true' when the corresponding start- or endtime is set within 5-10mins after LocationService got started. Display IsVague visually in "Overview" fragment saying measurement might be inaccurate / wrong.
    private boolean starttimeIsVague = false;
    @DatabaseField
    private long endtime = 0; // in seconds
    private boolean endtimeIsVague = false;
    @DatabaseField(canBeNull = false, foreign = true, foreignAutoRefresh = true, columnName = "tla_id")
    private Zone tla;

    public Timeslot(){}

    public Timeslot(long starttime, Zone tla){
        this.starttime = starttime;
        this.tla = tla;
    }

    public Timeslot(long starttime, long endtime, Zone tla){
        this.starttime = starttime;
        this.endtime = endtime;
        this.tla = tla;
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

    public Zone getTLA(){
        return tla;
    }

    public void setTLA(Zone tla){
        this.tla = tla;
    }

    private String getReadableDate(long time){
        if (isSameDay(System.currentTimeMillis(), time)){
            return "Today";
        }
        if (isSameDay(System.currentTimeMillis() - 1000 * 60 * 60 * 24, time)){
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
        if(this.endtime == 0){
            return getReadableDuration(this.starttime, System.currentTimeMillis(), true);
        } else {
            return getReadableDuration(this.starttime,  this.endtime, true);
        }
    }

    // TODO: add method getReadableDuration(...) that takes timeinterval as argument

    public static String getReadableDuration(long time1, long time2, boolean ignoreSeconds){
        int secInMillis = 1000,
            minInMillis = secInMillis * 60,
            hourInMillis = minInMillis * 60,
            dayInMillis = hourInMillis * 24,
            yearInMillis = dayInMillis * 365;
        int years, days, hours, mins, secs;
        String output = "";

        long diff = time1 > time2 ? time1-time2 : time2-time1;

        years = (int) (diff / yearInMillis);
        diff = diff - years * yearInMillis;
        days = (int) (diff / dayInMillis);
        diff = diff - days * dayInMillis;
        hours = (int) (diff / hourInMillis);
        diff = diff - hours * hourInMillis;
        mins = (int) (diff / minInMillis);
        diff = diff - mins * minInMillis;
        secs = (int) (diff / secInMillis);

        // generate output string
        if(years != 0){
            output += days + " y";
        }
        if(days != 0){
            if(!output.equals("")){
                output += "\n";
            }
            output += days + " d";
        }
        if(hours != 0){
            if(!output.equals("")){
                output += "\n";
            }
            output += hours + " h";
        }
        if(mins != 0){
            if(!output.equals("")){
                output += "\n";
            }
            output += mins + " min";
        }
        if(mins != 0 && !ignoreSeconds){
            if(!output.equals("")){
                output += "\n";
            }
            output += secs + " sec";
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

        long days = durationInMinutes / (60*24);
        long rest = durationInMinutes % (60*24);
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
