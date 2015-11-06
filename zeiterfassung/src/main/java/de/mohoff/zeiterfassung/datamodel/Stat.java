package de.mohoff.zeiterfassung.datamodel;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.sql.Time;

import de.mohoff.zeiterfassung.helpers.GeneralHelper;

/**
 * Created by moo on 11/1/15.
 */
@DatabaseTable(tableName = "stat")
public class Stat {
    @DatabaseField(generatedId = true) // autoincrement primary key
    private int _id;
    //@DatabaseField(canBeNull = false)
    //private int timestamp;
    @DatabaseField(canBeNull = false, unique = true)
    private String identifier;          // e.g. "distanceTravelled"
    @DatabaseField(canBeNull = false)
    private String displayString;       // e.g. "Distance Travelled"
    @DatabaseField(canBeNull = false, defaultValue = "n.a.")
    private String value;               // e.g. "11.400 km"

    public Stat(){}

    public Stat(String identifier, String displayString){
        this.identifier = identifier;
        this.displayString = displayString;
    }

    public Stat(String identifier, String displayString, String value){
        this(identifier, displayString);
        this.value = value;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getDisplayString() {
        return displayString;
    }

    public String getValue(){
        return value;
    }

    // Return the integer representation of 'value' it it's parsable.
    public int getIntValue(){
        try {
            return Integer.parseInt(value);
        } catch (Exception e){
            e.printStackTrace();
        }
        return -1;
    }

    // Extension for serviceUptime: Add a certain duration on top of 'value' and return human
    // readable string.
    public String getDisplayValueWithExtraTime(int extraTimeInSeconds){
        int intValue = getIntValue();
        if(intValue != -1 && identifier.equals("serviceUptime")){
            // Multiply intValue by 1000 because the method works with millis, not seconds.
            return Timeslot.getReadableDuration((intValue+extraTimeInSeconds) * 1000, false, false);
        }
        return null;
    }

    // Apply specific string formats to 'value' to generate an output format that is human readable.
    public String getDisplayValue() {
        // Try to convert 'value' to an integer. If convertible situational return a custom string
        // format. If it's not convertible just return 'value'.

        int intValue = getIntValue();
        if(intValue != -1 && identifier.equals("distanceTravelled")){
            return getReadableDistance(intValue);
        }
        if(intValue != -1 && identifier.equals("serviceUptime")){
            // Multiply intValue by 1000 because the method works with millis, not seconds.
            return Timeslot.getReadableDuration(intValue * 1000, false, false);
        }
        return value;
    }

    // Generate an output format for distances that is human readable.
    // This method separates a distance into a kilometer and a meter part in the output string.
    public static String getReadableDistance(int distanceInMeter){
        // Transforms the integer 12345 to String "12 km 345 m"

        String output = "";
        if(distanceInMeter >= 1000){
            output += distanceInMeter/1000 + " km ";
            distanceInMeter = distanceInMeter - (distanceInMeter/1000)*1000;
        }
        output += (distanceInMeter - distanceInMeter%10) + " m";
        return output;
    }
}
