package de.mohoff.zeiterfassung.helpers;

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
import de.mohoff.zeiterfassung.datamodel.Stat;
import de.mohoff.zeiterfassung.datamodel.Zone;
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
    private static final int DATABASE_VERSION = 10;

    // the DAO object we use to access the SimpleData table
    private Dao<Timeslot, Integer> timeslotDAO = null;
    private RuntimeExceptionDao<Timeslot, Integer> timeslotREDAO = null;
    private Dao<Zone, Integer> targetareasDAO = null;
    private RuntimeExceptionDao<Zone, Integer> zonesREDAO = null;
    private Dao<Loc, Integer> locDAO = null;
    private RuntimeExceptionDao<Loc, Integer> locREDAO = null;
    private Dao<Stat, Integer> statDAO = null;
    private RuntimeExceptionDao<Stat, Integer> statREDAO = null;


    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION, R.raw.ormlite_config);
    }

    public int getAmountOfTimeslots(){
        try {
            getTimeslotDAO();
            return (int)timeslotDAO.countOf();
        } catch(SQLException e){
            e.printStackTrace();
            return -1;
        }
    }

    public int getAmountOfZones(){
        try {
            getTargetLocationAreaDAO();
            return (int)targetareasDAO.countOf();
        } catch(SQLException e){
            e.printStackTrace();
            return -1;
        }
    }

    public int startNewTimeslot(long millis, Zone zone){
        getTimeslotREDAO();

        // check if timeslots are unsealed and already existing for passed activityName and locationName
        List<Timeslot> existingTimeslotsForActAndLoc = new ArrayList<Timeslot>();
        QueryBuilder<Timeslot, Integer> queryBuilder = timeslotREDAO.queryBuilder();
        try{
            queryBuilder.where().eq("endtime", 0).and().eq("zone_id", zone);
            PreparedQuery<Timeslot> preparedQuery = queryBuilder.prepare();
            existingTimeslotsForActAndLoc = timeslotREDAO.query(preparedQuery); // assumed there is only one "open" timeslot allowed for any time t
        } catch (SQLException e){
            e.printStackTrace();
        }

        if(existingTimeslotsForActAndLoc.isEmpty()){
            // timeslot not yet created/open
            Timeslot ts = new Timeslot(millis, zone);
            int result = -1;
            result = timeslotREDAO.create(ts);
            //amountOfTimeslots++;
            return result;
        } else {
            // timeslot with this locationName and activityName is already open --> don't create new one
            return -1;
        }
    }

    public int createNewZone(double latitude, double longitude, int radius, String activity, String location, int color){
        getTargetLocationAreaREDAO();
        Zone zone = new Zone((float)latitude, (float)longitude, radius, activity, location, color);

        try {
            getTargetLocationAreaDAO();
            return targetareasDAO.create(zone);
        } catch(SQLException e){
            return -1;
        }
    }

    public int updateZoneLocationName(int id, String newLocationName){
        UpdateBuilder<Zone, Integer> updateBuilder = zonesREDAO.updateBuilder();
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

    public int updateZoneActivityName(String oldActivityName, String newActivityName){
        UpdateBuilder<Zone, Integer> updateBuilder = zonesREDAO.updateBuilder();
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

    public int updateZone(Zone zone) {
        getTimeslotREDAO();
        return zonesREDAO.update(zone);
    }

    public Zone getZoneById(int id){
        QueryBuilder<Zone, Integer> queryBuilder = zonesREDAO.queryBuilder();
        try{
            queryBuilder.where().eq("_id", id);
            PreparedQuery<Zone> preparedQuery = queryBuilder.prepare();
            return zonesREDAO.query(preparedQuery).get(0);
        } catch (SQLException e){
            e.printStackTrace();
            return null;
        }
    }

    public int deleteZone(String activity, String location){
        DeleteBuilder<Zone, Integer> deleteBuilder = zonesREDAO.deleteBuilder();
        try {
            deleteBuilder.where().eq("activityName", activity).and().eq("locationName", location);
            deleteBuilder.delete();
            return 1;
        } catch(SQLException e){
            e.printStackTrace();
            return -1;
        }
    }

    public int deleteZoneById(int id){
        DeleteBuilder<Zone, Integer> deleteBuilder = zonesREDAO.deleteBuilder();
        try {
            deleteBuilder.where().eq("_id", id);
            deleteBuilder.delete();
            return 1;
        } catch(SQLException e){
            e.printStackTrace();
            return -1;
        }
    }

    public int deleteZonesByActivity(String activity){
        DeleteBuilder<Zone, Integer> deleteBuilder = zonesREDAO.deleteBuilder();
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

    public int deleteAllZones(){
        try {
            // Dropping and creating table will reset autoincrement for _id.
            TableUtils.dropTable(connectionSource, Zone.class, true);
            TableUtils.createTable(connectionSource, Zone.class);
            return 1;
        } catch (SQLException e){
            e.printStackTrace();
            return -1;
        }
    }

    public int deleteAllStats(){
        try {
            // Dropping and creating table will reset autoincrement for _id.
            TableUtils.dropTable(connectionSource, Stat.class, true);
            TableUtils.createTable(connectionSource, Stat.class);
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

    public void _createSampleZones(){
        int status;
        status = createNewZone(48.743715, 9.095967, 50, "Home", "Vaihingen Allmandring 26d", 0xFF025167);
        status = createNewZone(48.243962, 9.928635, 75, "Home", "Burgrieden Mittelweg 10", 0xFF025167);
        status = createNewZone(48.742120, 9.101002, 100, "Uni", "HdM", 0xFF025167);
        status = createNewZone(48.745847, 9.105381, 50, "VVS", "Station Uni", 0xFF025167);
        status = createNewZone(48.74319107, 9.10227019, 50, "Bars", "Wuba", 0xFF025167);
        status = createNewZone(48.74642511, 9.10120401, 50, "Bars", "Sansibar", 0xFF025167);
        status = createNewZone(48.74506944, 9.09997154, 50, "Bars", "Boddschi", 0xFF025167);
        status = createNewZone(48.74311678, 9.09741271, 50, "Bars", "Unithekle", 0xFF025167);
        status = createNewZone(48.77232266, 9.15882993, 50, "Freizeit", "mgfitness", 0xFF025167);
        status = createNewZone(48.73002512, 9.111964, 75, "Freizeit", "Corso Kino", 0xFF025167);
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
            TableUtils.createTable(connectionSource, Zone.class);
            TableUtils.createTable(connectionSource, Loc.class);
            TableUtils.createTable(connectionSource, Stat.class);
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
            TableUtils.dropTable(connectionSource, Zone.class, true);
            TableUtils.dropTable(connectionSource, Loc.class, true);
            TableUtils.dropTable(connectionSource, Stat.class, true);
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
    public Dao<Zone, Integer> getTargetLocationAreaDAO() throws SQLException {
        if (targetareasDAO == null) {
            targetareasDAO = getDao(Zone.class);
        }
        return targetareasDAO;
    }
    public Dao<Loc, Integer> getLocDAO() throws SQLException {
        if (locDAO == null) {
            locDAO = getDao(Loc.class);
        }
        return locDAO;
    }
    public Dao<Stat, Integer> getStatDAO() throws SQLException {
        if (statDAO == null) {
            statDAO = getDao(Stat.class);
        }
        return statDAO;
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
    public RuntimeExceptionDao<Zone, Integer> getTargetLocationAreaREDAO() {
        if (zonesREDAO == null) {
            zonesREDAO = getRuntimeExceptionDao(Zone.class);
        }
        return zonesREDAO;
    }
    public RuntimeExceptionDao<Loc, Integer> getLocREDAO() {
        if (locREDAO == null) {
            locREDAO = getRuntimeExceptionDao(Loc.class);
        }
        return locREDAO;
    }
    public RuntimeExceptionDao<Stat, Integer> getStatREDAO() {
        if (statREDAO == null) {
            statREDAO = getRuntimeExceptionDao(Stat.class);
        }
        return statREDAO;
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
        zonesREDAO = null;
        locDAO = null;
        locREDAO = null;
        statDAO = null;
        statREDAO = null;
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
        } catch (Exception e){
            // (SQLException | IndexOutOfBoundsException e)
            e.printStackTrace();
        }
        return null;
    }

    public List<Zone> getAllZones(){
        getTargetLocationAreaREDAO();

        try {
            return zonesREDAO.queryForAll();
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
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

    public List<Stat> getAllStats(){
        getStatREDAO();
        List<Stat> stats = new ArrayList<>();

        try {
            stats = statREDAO.queryForAll();
        } catch (Exception e){
            e.printStackTrace();
        }
        return stats;
    }

    public int initStatistics(){
        ArrayList<Stat> stats = new ArrayList<>();
        stats.add(new Stat("serviceUptime", "Background service uptime"));
        stats.add(new Stat("distanceTravelled", "Distance travelled"));
        stats.add(new Stat("numberOfTimeslots", "Zone movements tracked"));
        stats.add(new Stat("numberOfZones", "Active Zones"));

        // Insert 'stats' in DB
        for(Stat stat : stats){
            try {
                getStatDAO();
                statDAO.create(stat);
            } catch(SQLException e){
                e.printStackTrace();
                return -1;
            }
        }
        return 1;
    }

    private void checkForStat(String identifier){
        Stat result = null;
        try{
            getStatDAO();
            result = statDAO.queryForEq("identifier", identifier).get(0);
        } catch (SQLException | IndexOutOfBoundsException | NullPointerException e){
            e.printStackTrace();
        }

        if(result == null){
            initStatistics();
        }
    }

    public int updateStat(String identifier, int newValue){
        checkForStat(identifier);
        getStatREDAO();

        UpdateBuilder<Stat, Integer> updateBuilder = statREDAO.updateBuilder();
        try {
            updateBuilder.updateColumnValue("value", newValue);
            updateBuilder.where().eq("identifier", identifier);
            updateBuilder.update();
            return 1;
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    // updateStat for Stats that can be retrieved automatically with DatabaseHelper methods and
    // thus don't need a 'newValue' parameter.
    public int updateStat(String identifier){
        checkForStat(identifier);
        getStatREDAO();

        UpdateBuilder<Stat, Integer> updateBuilder = statREDAO.updateBuilder();
        int newValue;

        if(identifier.equals("numberOfTimeslots")){
            newValue = getAmountOfTimeslots();
        } else
        if(identifier.equals("numberOfZones")){
            newValue = getAmountOfZones();
        } else {
            // Do not update if identifier doesn't match any of the listed values
            return -1;
        }

        // Execute prepared update operation
        try {
            updateBuilder.updateColumnValue("value", newValue);
            updateBuilder.where().eq("identifier", identifier);
            updateBuilder.update();
            return 1;
        } catch (SQLException e){
            e.printStackTrace();
            return -1;
        }
    }

    public Cursor getCursorForZones(){
        Cursor c = null;
        QueryBuilder<Zone, Integer> queryBuilder = zonesREDAO.queryBuilder();
        //qb.where()...;

        try {
            PreparedQuery<Zone> query = queryBuilder.prepare();
            CloseableIterator<Zone> iterator = targetareasDAO.iterator(query);
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

        QueryBuilder<Zone, Integer> queryBuilder = zonesREDAO.queryBuilder();
        try{
            List<Zone> matches = queryBuilder.distinct().selectColumns("activityName").query();
            for(Zone zone : matches){
                activities.add(zone.getActivityName());
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

    public int cleanLocs(){
        try {
            // Dropping and creating table will reset autoincrement for _id.
            TableUtils.dropTable(connectionSource, Loc.class, true);
            TableUtils.createTable(connectionSource, Loc.class);
            return 1;
        } catch (SQLException e){
            e.printStackTrace();
            return -1;
        }
    }

    public int dumpLocs(CircularFifoQueue<Loc> cache){
        getLocREDAO();

        // Remove existing records first
        if(cleanLocs() != 1){
            return -1;
        }

        // Dump new records
        for(Loc record : cache){
            try {
                getLocDAO();
                locDAO.create(record);
            } catch(SQLException e){
                e.printStackTrace();
                return -1;
            }
        }
        return 1;
    }

    public List<Loc> getLocs(long maxAge){
        getLocREDAO();
        List<Loc> locList = new ArrayList<>();

        QueryBuilder<Loc, Integer> queryBuilder = locREDAO.queryBuilder();
        try {
            queryBuilder.where().gt("timestampInMillis", maxAge);
            // Smallest (=older) timestamps first, so they are put in queue first later on.
            // queryBuilder.orderBy(String columnname, boolean ascending)
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