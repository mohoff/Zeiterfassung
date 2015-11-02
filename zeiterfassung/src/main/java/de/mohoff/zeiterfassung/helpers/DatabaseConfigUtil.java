package de.mohoff.zeiterfassung.helpers;

import com.j256.ormlite.android.apptools.OrmLiteConfigUtil;

import de.mohoff.zeiterfassung.datamodel.Loc;
import de.mohoff.zeiterfassung.datamodel.Stat;
import de.mohoff.zeiterfassung.datamodel.Zone;
import de.mohoff.zeiterfassung.datamodel.Timeslot;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Created by Moritz on 28.09.2014.
 */
public class DatabaseConfigUtil extends OrmLiteConfigUtil {

    private static final Class<?>[] classes = new Class[] {
            Timeslot.class,
            Zone.class,
            Loc.class,
            Stat.class
    };
    public static void main(String[] args) throws SQLException, IOException {
        //writeConfigFile("ormlite_config.txt", classes);
        //writeConfigFile("C://Users/Moritz/GitHub/ZeiterfassungWithMap/zeiterfassung/src/main/res/raw/ormlite_config.txt", classes);
        //writeConfigFile("zeiterfassung/src/main/res/raw/ormlite_config.txt", classes);

        // worked!
        writeConfigFile(new File("/home/moo/git/Zeiterfassung/zeiterfassung/src/main/res/raw/ormlite_config.txt"), classes);


    }
}