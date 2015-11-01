package de.mohoff.zeiterfassung.datamodel;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Created by moo on 11/1/15.
 */
@DatabaseTable(tableName = "statistics")
public class Statistics {
    @DatabaseField(generatedId = true) // autoincrement primary key
    private int _id;
    @DatabaseField(canBeNull = false) // autoincrement primary key
    private int timestamp;
    @DatabaseField(canBeNull = false)
    private int travelledDistance = 0;
    @DatabaseField(canBeNull = false)
    private long locationServiceUptime = 0;

    public int getTravelledDistance() {
        return travelledDistance;
    }

    public long getLocationServiceUptime() {
        return locationServiceUptime;
    }
}
