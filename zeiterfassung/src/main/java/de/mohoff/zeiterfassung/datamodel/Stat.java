package de.mohoff.zeiterfassung.datamodel;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

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

    public String getValue() {
        return value;
    }


}
