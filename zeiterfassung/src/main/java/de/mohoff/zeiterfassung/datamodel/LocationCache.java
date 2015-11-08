package de.mohoff.zeiterfassung.datamodel;

import com.google.android.gms.maps.model.LatLng;

import org.apache.commons.collections4.queue.CircularFifoQueue;

import de.mohoff.zeiterfassung.locationservice.LocationService;

/**
 * Created by Moritz on 03.10.2014.
 */
public class LocationCache {
    // activeCache holds all locations which are used by the next interpolatedPosition calculation.
    private CircularFifoQueue<Loc> activeCache;
    // activeInterpolatedCache should be as big as activeCache and holds all interpolated locations
    // which were computed from activeCache
    private CircularFifoQueue<Loc> activeInterpolatedCache;
    // passiveCache serves as a bigger location cache which provides its data to maps for drawing markers.
    // For users it might be interesting to see a wider range covered by map markers compared to the
    // amount of locations in activeCache.
    private CircularFifoQueue<Loc> passiveCache;
    private boolean firstPassiveCacheDropHappened;
    // interpolatedPosition holds the most recent interpolated position which is used to check for
    // "area-entered" and "area-left" events.
    private Loc interpolatedPosition;

    private static final LocationCache cache = new LocationCache();
    public static LocationCache getInstance(){
        if(cache.activeCache == null){
            cache.activeCache = new CircularFifoQueue<>(LocationService.ACTIVE_CACHE_SIZE);
        }
        if(cache.passiveCache == null) {
            cache.passiveCache = new CircularFifoQueue<>(LocationService.PASSIVE_CACHE_SIZE);
        }
        if(cache.activeInterpolatedCache == null) {
            cache.activeInterpolatedCache = new CircularFifoQueue<>(LocationService.ACTIVE_CACHE_SIZE);
        }
        return cache;
    }

    /*public void setParameters(int activeQueueSize, int passiveQueueSize, int updateInterval, float interpolationVariance){
        this.activeQueueSize = activeQueueSize;
        activeCache = new CircularFifoQueue<>(activeQueueSize);
        activeInterpolatedCache = new CircularFifoQueue<>(activeQueueSize);

        this.passiveQueueSize = passiveQueueSize;
        passiveCache = new CircularFifoQueue<>(passiveQueueSize);
        this.firstPassiveCacheDropHappened = false;

        this.updateInterval = updateInterval;
        this.interpolationVariance = interpolationVariance;
    }*/

    public CircularFifoQueue<Loc> getPassiveCache(){
        return passiveCache;
    }
    public CircularFifoQueue<Loc> getActiveCache(){
        return activeCache;
    }

    public void setPassiveCache(CircularFifoQueue<Loc> cache){
        if(passiveCache.isEmpty()){
            passiveCache = cache;
        }
    }

    public float validateInBoundsForZone(Zone zone){
        float positives = 0;

        for(int i=0; i< activeInterpolatedCache.size(); i++){
            Loc loc = (Loc) activeInterpolatedCache.get(i);
            int distanceToZoneBorder = loc.distanceTo(new Loc(zone.getLatitude(), zone.getLongitude())) - zone.getRadius();
            if(distanceToZoneBorder < 0){
                positives++;
            }
        }
        return positives/(float) activeInterpolatedCache.size();
    }

    // Uses activeInterpolatedCache
    public float getCurrentInBoundProxFor(Zone zone){
        float positives = 0;
        float all = activeCache.size();

        for(int i=0; i< activeInterpolatedCache.size(); i++){
            Loc loc = (Loc) activeInterpolatedCache.get(i);
            //int distanceDebug = loc.distanceTo(new Loc(tla.getLatitude(), tla.getLongitude()));
            int distanceZoneBorderToUser = loc.distanceTo(new Loc(zone.getLatitude(), zone.getLongitude())) - zone.getRadius();
            if(distanceZoneBorderToUser <= 0){
                positives++;
            }
        }
        return positives/all;
    }

    // Uses activeCache
    public float getCurrentInBoundProxFor2(Zone zone){
        float positives = 0;
        float all = activeCache.size();

        for(int i=0; i< activeCache.size(); i++){
            Loc loc = (Loc) activeCache.get(i);
            //int distanceDebug = loc.distanceTo(new Loc(tla.getLatitude(), tla.getLongitude()));
            int distanceZoneBorderToUser = loc.distanceTo(new Loc(zone.getLatitude(), zone.getLongitude())) - zone.getRadius();
            if(distanceZoneBorderToUser <= 0){
                positives++;
            }
        }
        return positives/all;
    }




    public void addLocationUpdate(Loc newLoc){
        // Handle activeCache.
        if(newLoc.getAccuracy() < LocationService.ACCURACY_TRESHOLD){
            activeCache.add(newLoc);

            // !!! Interpolated positions are not used right now !!!
            // Compute interpolated position and update activeInterpolatedCache.
            computeInterpolatedPosition();
        }

        // Handle passiveCache.
        // Add newLoc to passiveCache if newLoc is a real update or there was a real update added
        // in the last call of this function.
        if(newLoc.isRealUpdate() || passiveCache.get(0).isRealUpdate()){
            passiveCache.add(newLoc);
        }
        if(isPassiveCacheFull()){
            firstPassiveCacheDropHappened = true;
        }
    }

    public boolean isActiveCacheFull(){
        // locationCache.isActiveCacheFull() always returns false in my case...
        return activeCache.size() == activeCache.maxSize();
    }

    public boolean isPassiveCacheFull(){
        // locationCache.isActiveCacheFull() always returns false in my case...
        return passiveCache.size() == passiveCache.maxSize();
    }

    public boolean hasFirstPassiveQueueDropHappened(){
        return firstPassiveCacheDropHappened;
    }

    public Loc getMostRecentLoc(){
        for(int i=activeCache.size()-1; i>=0; i--){
            try {
                return activeCache.get(i);
            } catch (Exception e){
                e.printStackTrace();
            }
        }
        return null;
    }

    public Loc getInterpolatedPosition() {
        return interpolatedPosition;
    }

    // Returns value between 0.0 (bad) and 1.0 (good).
    public static double getNormedAgeMultiplier(long millisInPast){
        long minsInPast = millisInPast/(1000*60);
        if(minsInPast >= 15) return 0;

        double x = (double)(15-minsInPast) / 15.0;
        return Math.pow(x, 4); // x^4

        /*
        //long millisInPast = currentTime - loc.getTimestampInMillis();
        int optimalTimeInCache = LocationService.ACTIVE_CACHE_SIZE * LocationService.REGULAR_UPDATE_INTERVAL; // ms
        double ageMultiplier = 0; // default. If more than 15min in past, don't weight this location/timestamp anymore

        long tresholdTimeToScoreOfZero = LocationService.ACTIVE_CACHE_SIZE * LocationService.REGULAR_UPDATE_INTERVAL * 3; // 3 for 3*5=15min in past is treshold
        double slopeOfRegression = 1.5 / (double) tresholdTimeToScoreOfZero;
        if(millisInPast <= tresholdTimeToScoreOfZero){
            // [0 - 1,5]
            ageMultiplier = (tresholdTimeToScoreOfZero-millisInPast)*slopeOfRegression; // x * slope = y = score
        }
        return ageMultiplier;
        */
    }

    // Returns value between 0.0 (bad) and 1.0 (good).
    public static double getNormedAccuracyMultiplier(double acc){
        if(acc < 50) return 1;
        if(acc > 3050) return 0;

        double x = (3050-acc)/3050;
        return Math.pow(x, 4);


        /*
        double accuracyMultiplier = 0.5;
        if(acc > 30){  // dont apply penalty if accuracy is <= 30m
            double x = acc/100; // [0.31 - 30 ]
            accuracyMultiplier += 1.5 * (1 - (2*x / (1+Math.pow(x, 2)))); // term in outer brackets: [0 .. 1]
            // accuracyMultiplier between 0.5 and 2 right now
        } else if(acc == 0.0){     // If no accuracy provided (is sometimes 'True')
            accuracyMultiplier = 0.75;
        } else {
            accuracyMultiplier = 2;
        }
        return accuracyMultiplier;
        */

        //////// LEGACY
        /*double accuracyPenalty = 0.0;
        if(acc > 30){  // dont apply penalty if accuracy is <= 30m
            double x = acc/100;
            accuracyPenalty = 0.5*x / (1+Math.pow(x, 2));       //    0.5*x/(1+x^2) // bei x=2 schon fast gesättigt (0.5)
            // maybe change factor of 0.5 to 1 or 0.75?
        } else if(acc == 0.0){     // if no accuracy provided
            accuracyPenalty = 0.3;
        }
        accuracyPenalty *= interpolationVariance;
        return accuracyPenalty;*/
    }

    private void computeInterpolatedPosition(){
        double[] score = new double[activeCache.size()];
        double scoreSum = 0;
        double latSumCounter = 0, lngSumCounter = 0;
        long millisInPast = System.currentTimeMillis() - activeCache.get(0).getTimestampInMillis();

        if((millisInPast < 1000) && activeCache.get(0).getAccuracy() <= 50){
            interpolatedPosition = activeCache.get(0);
            activeInterpolatedCache.add(interpolatedPosition);
            return;
        }

        for(int i=0; i<activeCache.size(); i++){
            Loc loc = activeCache.get(i);
            double ageMultiplier = getNormedAgeMultiplier(millisInPast); // [0.0 - 1.0]
            double accuracyMultiplier = getNormedAccuracyMultiplier(loc.getAccuracy()); // [0.0 - 1.0]
            score[i] = ageMultiplier * accuracyMultiplier;

            // Apply weighted arithmethic mean, http://en.wikipedia.org/wiki/Weighted_arithmetic_mean > Mathematical definition
            scoreSum += score[i];
            latSumCounter += loc.getLatitude() * score[i];
            lngSumCounter += loc.getLongitude() * score[i];
        }
        interpolatedPosition = new Loc(latSumCounter/scoreSum, lngSumCounter/scoreSum);
        activeInterpolatedCache.add(interpolatedPosition);

        /*
        for(int i=0; i<activeCache.size(); i++){
            Loc loc = activeCache.get(i);
            double ageMultiplier = getNormedAgeMultiplier(millisInPast); // [0 - 1,5]
            double accuracyMultiplier = loc.getAccuracyMultiplier(); // [0.5 - 2]

            //score[i] = (float)(_getStartWeight(i, cacheSize) - loc.getAccuracyMultiplier());
            score[i] = (float)(1 * ageMultiplier * accuracyMultiplier);
            // weighted arithmethic mean, http://en.wikipedia.org/wiki/Weighted_arithmetic_mean > Mathematical definition
            scoreSum += score[i];
            latSumCounter += loc.getLatitude() * score[i];
            lngSumCounter += loc.getLongitude() * score[i];
        }
        interpolatedPosition = new Loc(latSumCounter/scoreSum, lngSumCounter/scoreSum);
        activeInterpolatedCache.add(interpolatedPosition);
        */
        // LEGACY
        /*
        for(int i=0; i<cacheSize; i++){
            Location currentLoc = (Location) locationCache.get(i);
            double baseFactor = interpolationVariance * ((double)(i+1)/(double)cacheSize) + 0.5; // [0.5, 1.5]

            double accuracyPenalty = 0.0;
            if(currentLoc.getAccuracy() > 30){  // dont apply penalty if accuracy is <= 30m
                float x = currentLoc.getAccuracy()/100;
                accuracyPenalty = 0.5*x / (1+Math.pow(x, 2));       //    0.5*x/(1+x^2) // bei x=2 schon fast gesättigt (0.5)
                // maybe change factor of 0.5 to 1 or 0.75?
            } else if(currentLoc.getAccuracy() == 0.0){     // if no accuracy provided
                accuracyPenalty = 0.3;
            }
            accuracyPenalty *= interpolationVariance;
            score[i] = (float)(baseFactor - accuracyPenalty);
            scoreSum += score[i];

        }

        // weighted arithmethic mean, http://en.wikipedia.org/wiki/Weighted_arithmetic_mean > Mathematical definition
        double latSumZaehler = 0, lngSumCounter = 0;

        for(int i=0; i<locationCache.size(); i++){
            Location currentLoc = (Location)locationCache.get(i);
            latSumZaehler += currentLoc.getLatitude() * score[i];
            lngSumCounter += currentLoc.getLongitude() * score[i];
        }
        LatLng result = new LatLng(latSumZaehler/scoreSum, lngSumCounter/scoreSum);
        return result;
        */
    }

}
