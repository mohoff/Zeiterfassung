package de.mohoff.zeiterfassung.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import com.j256.ormlite.android.AndroidDatabaseResults;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.CloseableIterator;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.UpdateBuilder;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import de.mohoff.zeiterfassung.R;
import de.mohoff.zeiterfassung.datamodel.Loc;
import de.mohoff.zeiterfassung.datamodel.TargetLocationArea;
import de.mohoff.zeiterfassung.datamodel.Timeslot;

import java.sql.SQLException;
import java.util.*;


/**
 * Database helper class used to manage the creation and upgrading of your database. This class also usually provides
 * the DAOs used by the other classes.
 */
public class DatabaseHelper extends OrmLiteSqliteOpenHelper {
    // name of the database file for your application -- change to something appropriate for your app
    private static final String DATABASE_NAME = "database.db";

    // any time you make changes to your database objects, you may have to increase the database version
    private static final int DATABASE_VERSION = 6;

    // the DAO object we use to access the SimpleData table
    private Dao<Timeslot, Integer> timeslotDAO = null;
    private RuntimeExceptionDao<Timeslot, Integer> timeslotREDAO = null;
    private Dao<TargetLocationArea, Integer> targetareasDAO = null;
    private RuntimeExceptionDao<TargetLocationArea, Integer> targetareasREDAO = null;
    private Dao<Loc, Integer> locDAO = null;
    private RuntimeExceptionDao<Loc, Integer> locREDAO = null;


    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION, R.raw.ormlite_config);
    }

    public int getAmountOfTimeslots(){
        // approach 1, so we can give better return values
        int result = -1;
        try {
            getTimeslotDAO();
            result = (int)timeslotDAO.countOf();
        } catch(SQLException e){
            e.printStackTrace();
        }
        return result;

        // approach 2
        /*RuntimeExceptionDao<Timeslot, Integer> dao2 = getTimeslotREDAO();
        return (int) dao2.countOf();*/
    }

    public int startNewTimeslot(long millis, TargetLocationArea tla){
        getTimeslotREDAO();

        // check if timeslots are unsealed and already existing for passed activityName and locationName
        List<Timeslot> existingTimeslotsForActAndLoc = new ArrayList<Timeslot>();
        QueryBuilder<Timeslot, Integer> queryBuilder = timeslotREDAO.queryBuilder();
        try{
            queryBuilder.where().eq("endtime", 0).and().eq("tla_id", tla);
            PreparedQuery<Timeslot> preparedQuery = queryBuilder.prepare();
            existingTimeslotsForActAndLoc = timeslotREDAO.query(preparedQuery); // assumed there is only one "open" timeslot allowed for any time t
        } catch (SQLException e){
            e.printStackTrace();
        }

        if(existingTimeslotsForActAndLoc.isEmpty()){
            // timeslot not yet created/open
            Timeslot ts = new Timeslot(millis, tla);
            int result = -1;
            result = timeslotREDAO.create(ts);
            //amountOfTimeslots++;
            return result;
        } else {
            // timeslot with this locationName and activityName is already open --> don't create new one
            return -1;
        }
    }

    public int createNewTLA(double latitude, double longitude, int radius, String activity, String location){
        getTargetLocationAreaREDAO();

        TargetLocationArea tla = new TargetLocationArea((float)latitude, (float)longitude, radius, activity, location);
        int result = -1;

        try {
            getTargetLocationAreaDAO();
            result = targetareasDAO.create(tla);
        } catch(SQLException e){

        }

        return result;
    }

    public int updateTLALocationName(int id, String newLocationName){
        UpdateBuilder<TargetLocationArea, Integer> updateBuilder = targetareasREDAO.updateBuilder();
        try {
            updateBuilder.updateColumnValue("locationName", newLocationName);
            updateBuilder.where().eq("_id", id);
            updateBuilder.update();
            return 1;
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public int updateTLAActivityName(String oldActivityName, String newActivityName){
        UpdateBuilder<TargetLocationArea, Integer> updateBuilder = targetareasREDAO.updateBuilder();
        try {
            updateBuilder.updateColumnValue("activityName", newActivityName);
            updateBuilder.where().eq("activityName", oldActivityName);
            updateBuilder.update();
            return 1;
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public TargetLocationArea getTLAById(int id){
        QueryBuilder<TargetLocationArea, Integer> queryBuilder = targetareasREDAO.queryBuilder();
        try{
            queryBuilder.where().eq("_id", id);
            PreparedQuery<TargetLocationArea> preparedQuery = queryBuilder.prepare();
            return targetareasREDAO.query(preparedQuery).get(0);
        } catch (SQLException e){
            e.printStackTrace();
            return null;
        }
    }

    public int deleteTLA(String activity, String location){
        DeleteBuilder<TargetLocationArea, Integer> deleteBuilder = targetareasREDAO.deleteBuilder();
        try {
            deleteBuilder.where().eq("activityName", activity).and().eq("locationName", location);
            deleteBuilder.delete();
            return 1;
        } catch(SQLException e){
            e.printStackTrace();
            return -1;
        }
    }

    public int deleteTLAById(int id){
        DeleteBuilder<TargetLocationArea, Integer> deleteBuilder = targetareasREDAO.deleteBuilder();
        try {
            deleteBuilder.where().eq("_id", id);
            deleteBuilder.delete();
            return 1;
        } catch(SQLException e){
            e.printStackTrace();
            return -1;
        }
    }

    public int deleteTLAsByActivity(String activity){
        DeleteBuilder<TargetLocationArea, Integer> deleteBuilder = targetareasREDAO.deleteBuilder();
        try {
            deleteBuilder.where().eq("activityName", activity);
            deleteBuilder.delete();
            return 1;
        } catch(SQLException e){
            e.printStackTrace();
            return -1;
        }
    }

    public int deleteAllTimeslots(){
        try {
            // Dropping and creating table will reset autoincrement for _id.
            TableUtils.dropTable(connectionSource, Timeslot.class, true);
            TableUtils.createTable(connectionSource, Timeslot.class);
            return 1;
        } catch (SQLException e){
            e.printStackTrace();
            return -1;
        }
    }

    public int deleteAllTLAs(){
        try {
            // Dropping and creating table will reset autoincrement for _id.
            TableUtils.dropTable(connectionSource, TargetLocationArea.class, true);
            TableUtils.createTable(connectionSource, TargetLocationArea.class);
            return 1;
        } catch (SQLException e){
            e.printStackTrace();
            return -1;
        }
    }

    public int sealCurrentTimeslot(long millis){ // seals newest TS
        getTimeslotREDAO();

        /*
        // approach 1
        Timeslot toUpdate = timeslotREDAO.queryForId(getAmountOfTimeslots());     // does this work? does it count from 0 or 1 or not even autoincrement?
        toUpdate.setEndtime(millis);
        timeslotREDAO.update(toUpdate);
        */

        // approach 2
        UpdateBuilder<Timeslot, Integer> updateBuilder = timeslotREDAO.updateBuilder();
        try {
            updateBuilder.updateColumnValue("endtime", millis);
            updateBuilder.where().eq("endtime", 0);
            //updateBuilder.where().isNull("endtime");
            updateBuilder.update();
            return 1;
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public int closeTimeslotById(int id, long millis){
        getTimeslotREDAO();

        Timeslot toClose = timeslotREDAO.queryForId(id);
        toClose.setEndtime(millis);
        return timeslotREDAO.update(toClose);
    }

    public void _createSampleTLAs(){
        int status;
        status = createNewTLA(48.743715, 9.095967, 50, "Home", "Vaihingen allmandring 26d");
        status = createNewTLA(48.243962, 9.928635, 75, "Home", "Burgrieden mittelweg 10");
        status = createNewTLA(48.742120, 9.101002, 100, "Uni", "HdM");
        status = createNewTLA(48.745847, 9.105381, 50, "VVS", "Station Uni");
        status = createNewTLA(48.74319107, 9.10227019, 50, "Bars", "Wuba");
        status = createNewTLA(48.74642511, 9.10120401, 50, "Bars", "Sansibar");
        status = createNewTLA(48.74506944, 9.09997154, 50, "Bars", "Boddschi");
        status = createNewTLA(48.74311678, 9.09741271, 50, "Bars", "Unithekle");
        status = createNewTLA(48.77232266, 9.15882993, 50, "Freizeit", "mgfitness");
        status = createNewTLA(48.73002512, 9.111964, 75, "Freizeit", "Corso Kino");
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
            TableUtils.createTable(connectionSource, TargetLocationArea.class);
            TableUtils.createTable(connectionSource, Loc.class);
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
            TableUtils.dropTable(connectionSource, TargetLocationArea.class, true);
            TableUtils.dropTable(connectionSource, Loc.class, true);
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
    public Dao<Timeslot, Integer> getTimeslotDAO() throws SQLException {
        if (timeslotDAO == null) {
            timeslotDAO = getDao(Timeslot.class);
        }
        return timeslotDAO;
    }
    public Dao<TargetLocationArea, Integer> getTargetLocationAreaDAO() throws SQLException {
        if (targetareasDAO == null) {
            targetareasDAO = getDao(TargetLocationArea.class);
        }
        return targetareasDAO;
    }
    public Dao<Loc, Integer> getLocDAO() throws SQLException {
        if (locDAO == null) {
            locDAO = getDao(Loc.class);
        }
        return locDAO;
    }
    /**
     * Returns the RuntimeExceptionDao (Database Access Object) version of a Dao for our SimpleData class. It will
     * create it or just give the cached value. RuntimeExceptionDao only through RuntimeExceptions.
     */
    public RuntimeExceptionDao<Timeslot, Integer> getTimeslotREDAO() {
        if (timeslotREDAO == null) {
            timeslotREDAO = getRuntimeExceptionDao(Timeslot.class);
        }
        return timeslotREDAO;
    }
    public RuntimeExceptionDao<TargetLocationArea, Integer> getTargetLocationAreaREDAO() {
        if (targetareasREDAO == null) {
            targetareasREDAO = getRuntimeExceptionDao(TargetLocationArea.class);
        }
        return targetareasREDAO;
    }
    public RuntimeExceptionDao<Loc, Integer> getLocREDAO() {
        if (locREDAO == null) {
            locREDAO = getRuntimeExceptionDao(Loc.class);
        }
        return locREDAO;
    }
    /**
     * Close the database connections and clear any cached DAOs.
     */
    @Override
    public void close() {
        super.close();
        timeslotDAO = null;
        timeslotREDAO = null;
        targetareasDAO = null;
        targetareasREDAO = null;
        locDAO = null;
        locREDAO = null;
    }


    public List<Timeslot> getTimeslotsBetween(int starttime, int endtime){
        getTimeslotREDAO();
        List<Timeslot> timeslots = new ArrayList<Timeslot>();

        QueryBuilder<Timeslot, Integer> queryBuilder = timeslotREDAO.queryBuilder();
        try{
            queryBuilder.where().gt("starttime", starttime).and().lt("endtime", endtime);
            PreparedQuery<Timeslot> preparedQuery = queryBuilder.prepare();
            timeslots = timeslotREDAO.query(preparedQuery);
        } catch (SQLException e){
            e.printStackTrace();
        }

        return timeslots;
    }

    public Timeslot getOpenTimeslot(){
        getTimeslotREDAO();

        QueryBuilder<Timeslot, Integer> queryBuilder = timeslotREDAO.queryBuilder();
        try{
            queryBuilder.where().eq("endtime", 0);
            PreparedQuery<Timeslot> preparedQuery = queryBuilder.prepare();
            return timeslotREDAO.query(preparedQuery).get(0);
        } catch (SQLException | IndexOutOfBoundsException e){
            e.printStackTrace();
        }
        return null;
    }

    public List<TargetLocationArea> getAllTLAs(){
        getTargetLocationAreaREDAO();
        List<TargetLocationArea> tla = new ArrayList<TargetLocationArea>();

        try {
            tla = targetareasREDAO.queryForAll();
        } catch (Exception e){
            e.printStackTrace();
            tla = null;
        }
        return tla;
    }

    public List<Timeslot> getAllTimeslots(){
        getTimeslotREDAO();
        List<Timeslot> timeslots = new ArrayList<>();

        try {
            timeslots = timeslotREDAO.queryForAll();
        } catch (Exception e){
            e.printStackTrace();
        }
        return timeslots;
    }

    public Cursor getCursorForTLAs(){
        Cursor c = null;
        QueryBuilder<TargetLocationArea, Integer> queryBuilder = targetareasREDAO.queryBuilder();
        //qb.where()...;

        try {
            PreparedQuery<TargetLocationArea> query = queryBuilder.prepare();
            CloseableIterator<TargetLocationArea> iterator = targetareasDAO.iterator(query);
            AndroidDatabaseResults results =
                    (AndroidDatabaseResults)iterator.getRawResults();
            c = results.getRawCursor();
            iterator.closeQuietly();
        } catch(SQLException e){
            e.printStackTrace();
        }
        return c;
    }

    public List<String> getDistinctActivityNames(){
        getTargetLocationAreaREDAO();
        List<String> activities = new ArrayList<String>();

        QueryBuilder<TargetLocationArea, Integer> queryBuilder = targetareasREDAO.queryBuilder();
        try{
            List<TargetLocationArea> matches = queryBuilder.distinct().selectColumns("activityName").query();
            for(TargetLocationArea tla : matches){
                activities.add(tla.getActivityName());
            }
        } catch (SQLException e){
            e.printStackTrace();
        }
        return activities;
    }

    /*public List<Timeslot> getTimeslotsBetweenActivity(int starttime, int endtime, String activityName){
        getTimeslotREDAO();
        List<Timeslot> timeslots = new ArrayList<Timeslot>();

        QueryBuilder<Timeslot, Integer> queryBuilder = timeslotREDAO.queryBuilder();
        try{
            queryBuilder.where().gt("starttime", starttime).and().lt("endtime", endtime).and().eq("activity", activityName);
            PreparedQuery<Timeslot> preparedQuery = queryBuilder.prepare();
            timeslots = timeslotREDAO.query(preparedQuery);
        } catch (SQLException e){
            e.printStackTrace();
        }

        return timeslots;
    }*/

    public List<Timeslot> getTimeslotsBetweenLocation(int starttime, int endtime, String locationName){
        getTimeslotREDAO();
        List<Timeslot> timeslots = new ArrayList<Timeslot>();

        QueryBuilder<Timeslot, Integer> queryBuilder = timeslotREDAO.queryBuilder();
        try{
            queryBuilder.where().gt("starttime", starttime).and().lt("endtime", endtime).and().eq("location", locationName);
            PreparedQuery<Timeslot> preparedQuery = queryBuilder.prepare();
            timeslots = timeslotREDAO.query(preparedQuery);
        } catch (SQLException e){
            e.printStackTrace();
        }

        return timeslots;
    }

    /*public List<Timeslot> getTimeslotsBetweenActivityLocation(int starttime, int endtime, String activityName, String locationName){
        getTimeslotREDAO();
        List<Timeslot> timeslots = new ArrayList<Timeslot>();

        QueryBuilder<Timeslot, Integer> queryBuilder = timeslotREDAO.queryBuilder();
        try{
            queryBuilder.where().gt("starttime", starttime).and().lt("endtime", endtime).and().eq("activity", activityName).and().eq("location", locationName);
            PreparedQuery<Timeslot> preparedQuery = queryBuilder.prepare();
            timeslots = timeslotREDAO.query(preparedQuery);
        } catch (SQLException e){
            e.printStackTrace();
        }

        return timeslots;
    }*/

    public int dumpLocs(CircularFifoQueue<Loc> cache){
        getLocREDAO();
        int result = -1;

        // Remove existing records first
        try {
            // Dropping and creating table will reset autoincrement for _id.
            TableUtils.dropTable(connectionSource, Loc.class, true);
            TableUtils.createTable(connectionSource, Loc.class);
            //TableUtils.clearTable(connectionSource, Loc.class);
        } catch (SQLException e){
            e.printStackTrace();
            return result;
        }

        // Dump new records
        for(Loc record : cache){
            try {
                getLocDAO();
                result = locDAO.create(record);
            } catch(SQLException e){
                e.printStackTrace();
                return result;
            }
        }
        return result;
    }

    public List<Loc> getLocs(long maxAge){
        getLocREDAO();
        List<Loc> locList = new ArrayList<>();

        QueryBuilder<Loc, Integer> queryBuilder = locREDAO.queryBuilder();
        try {
            queryBuilder.where().gt("timestampInMillis", maxAge);
            // Smallest (=older) timestamps first, so they are put in queue first later on.
            queryBuilder.orderBy("timestampInMillis", true);
            PreparedQuery<Loc> preparedQuery = queryBuilder.prepare();
            locList = locREDAO.query(preparedQuery);
        } catch (Exception e){
            e.printStackTrace();
            return locList;
        }
        return locList;
    }

}