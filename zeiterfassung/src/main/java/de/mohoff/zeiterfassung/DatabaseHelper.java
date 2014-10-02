package de.mohoff.zeiterfassung;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.UpdateBuilder;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import de.mohoff.zeiterfassung.datamodel.Timeslot;

import java.sql.SQLException;
import java.util.*;


/**
 * Database helper class used to manage the creation and upgrading of your database. This class also usually provides
 * the DAOs used by the other classes.
 */
public class DatabaseHelper extends OrmLiteSqliteOpenHelper {
    // name of the database file for your application -- change to something appropriate for your app
    private static final String DATABASE_NAME = "timeslots.db";

    // any time you make changes to your database objects, you may have to increase the database version
    private static final int DATABASE_VERSION = 1;

    private int amountOfTimeslots = 0;

    // the DAO object we use to access the SimpleData table
    private Dao<Timeslot, Integer> dao = null;
    private RuntimeExceptionDao<Timeslot, Integer> runtimeExceptionDao = null;


    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION, R.raw.ormlite_config);
    }

    public int getAmountOfTimeslots(){
        // approach 1, so we can give better return values
        int result = -1;
        try {
            Dao<Timeslot, Integer> dao = getDao();
            result = (int)dao.countOf();
        } catch(SQLException e){
            e.printStackTrace();
        }
        return result;

        // approach 2
        /*RuntimeExceptionDao<Timeslot, Integer> dao2 = getTimeslotDao();
        return (int) dao2.countOf();*/
    }

    public int startNewTimeslot(int minutes, String activityName, String locationName){
        RuntimeExceptionDao<Timeslot, Integer> dao = getTimeslotDao();
        Timeslot ts = new Timeslot(minutes, activityName, locationName);
        int result = -1;
        result = dao.create(ts);
        amountOfTimeslots++;

        return result;
    }

    public int sealCurrentTimeslot(int minutes){
        RuntimeExceptionDao<Timeslot, Integer> dao = getTimeslotDao();

        // approach 1
        Timeslot toUpdate = dao.queryForId(getAmountOfTimeslots());     // does this work? does it count from 0 or 1 or not even autoincrement?
        toUpdate.setEndtime(minutes);
        dao.update(toUpdate);

        // approach 2
        UpdateBuilder<Timeslot, Integer> updateBuilder = dao.updateBuilder();
        try {
            updateBuilder.updateColumnValue("endtime", minutes);
            updateBuilder.where().isNull("endtime");        // only update the rows where password is null
            updateBuilder.update();
            return 1;
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }


    /**
     * This is called when the database is first created. Usually you should call createTable statements here to create
     * the tables that will store your data.
     */
    @Override
    public void onCreate(SQLiteDatabase db, ConnectionSource connectionSource) {
        try {
            Log.i(DatabaseHelper.class.getName(), "onCreate");
            TableUtils.createTable(connectionSource, Timeslot.class);
        } catch (SQLException e) {
            Log.e(DatabaseHelper.class.getName(), "Can't create database", e);
            throw new RuntimeException(e);
        }
    }
    /**
     * This is called when your application is upgraded and it has a higher version number. This allows you to adjust
     * the various data to match the new version number.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, ConnectionSource connectionSource, int oldVersion, int newVersion) {
        try {
            Log.i(DatabaseHelper.class.getName(), "onUpgrade");
            TableUtils.dropTable(connectionSource, Timeslot.class, true);
            // after we drop the old databases, we create the new ones
            onCreate(db, connectionSource);
        } catch (SQLException e) {
            Log.e(DatabaseHelper.class.getName(), "Can't drop databases", e);
            throw new RuntimeException(e);
        }
    }
    /**
     * Returns the Database Access Object (DAO) for our SimpleData class. It will create it or just give the cached
     * value.
     */
    public Dao<Timeslot, Integer> getDao() throws SQLException {
        if (dao == null) {
            dao = getDao(Timeslot.class);
        }
        return dao;
    }
    /**
     * Returns the RuntimeExceptionDao (Database Access Object) version of a Dao for our SimpleData class. It will
     * create it or just give the cached value. RuntimeExceptionDao only through RuntimeExceptions.
     */
    public RuntimeExceptionDao<Timeslot, Integer> getTimeslotDao() {
        if (runtimeExceptionDao == null) {
            runtimeExceptionDao = getRuntimeExceptionDao(Timeslot.class);
        }
        return runtimeExceptionDao;
    }
    /**
     * Close the database connections and clear any cached DAOs.
     */
    @Override
    public void close() {
        super.close();
        dao = null;
        runtimeExceptionDao = null;
    }


    public List<Timeslot> getTimeslotsBetween(int starttime, int endtime){
        RuntimeExceptionDao<Timeslot, Integer> dao = getTimeslotDao();
        List<Timeslot> timeslots = new ArrayList<Timeslot>();

        QueryBuilder<Timeslot, Integer> queryBuilder = dao.queryBuilder();
        try{
            queryBuilder.where().gt("starttime", starttime).and().lt("endtime", endtime);
            PreparedQuery<Timeslot> preparedQuery = queryBuilder.prepare();
            timeslots = dao.query(preparedQuery);
        } catch (SQLException e){
            e.printStackTrace();
        }

        return timeslots;
    }

    public List<Timeslot> getTimeslotsBetweenActivity(int starttime, int endtime, String activityName){
        RuntimeExceptionDao<Timeslot, Integer> dao = getTimeslotDao();
        List<Timeslot> timeslots = new ArrayList<Timeslot>();

        QueryBuilder<Timeslot, Integer> queryBuilder = dao.queryBuilder();
        try{
            queryBuilder.where().gt("starttime", starttime).and().lt("endtime", endtime).and().eq("activity", activityName);
            PreparedQuery<Timeslot> preparedQuery = queryBuilder.prepare();
            timeslots = dao.query(preparedQuery);
        } catch (SQLException e){
            e.printStackTrace();
        }

        return timeslots;
    }

    public List<Timeslot> getTimeslotsBetweenLocation(int starttime, int endtime, String locationName){
        RuntimeExceptionDao<Timeslot, Integer> dao = getTimeslotDao();
        List<Timeslot> timeslots = new ArrayList<Timeslot>();

        QueryBuilder<Timeslot, Integer> queryBuilder = dao.queryBuilder();
        try{
            queryBuilder.where().gt("starttime", starttime).and().lt("endtime", endtime).and().eq("location", locationName);
            PreparedQuery<Timeslot> preparedQuery = queryBuilder.prepare();
            timeslots = dao.query(preparedQuery);
        } catch (SQLException e){
            e.printStackTrace();
        }

        return timeslots;
    }

    public List<Timeslot> getTimeslotsBetweenActivityLocation(int starttime, int endtime, String activityName, String locationName){
        RuntimeExceptionDao<Timeslot, Integer> dao = getTimeslotDao();
        List<Timeslot> timeslots = new ArrayList<Timeslot>();

        QueryBuilder<Timeslot, Integer> queryBuilder = dao.queryBuilder();
        try{
            queryBuilder.where().gt("starttime", starttime).and().lt("endtime", endtime).and().eq("activity", activityName).and().eq("location", locationName);
            PreparedQuery<Timeslot> preparedQuery = queryBuilder.prepare();
            timeslots = dao.query(preparedQuery);
        } catch (SQLException e){
            e.printStackTrace();
        }

        return timeslots;
    }

}