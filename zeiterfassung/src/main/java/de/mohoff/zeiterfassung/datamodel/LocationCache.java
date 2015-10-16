package de.mohoff.zeiterfassung.datamodel;

import com.google.android.gms.maps.model.LatLng;

import org.apache.commons.collections4.queue.CircularFifoQueue;

/**
 * Created by Moritz on 03.10.2014.
 */
public class LocationCache {
    private int activeQueueSize, passiveQueueSize;
    private int updateInterval;
    private float interpolationVariance;

    // activeCache holds all locations which are used by the next interpolatedPosition calculation.
    private CircularFifoQueue activeCache;
    // activeInterpolatedCache should be as big as activeCache and holds all interpolated locations
    // which were computed from activeCache
    private CircularFifoQueue activeInterpolatedCache;
    // passiveCache serves as a bigger location cache which provides its data to maps for drawing markers.
    // For users it might be interesting to see a wider range covered by map markers compared to the
    // amount of locations in activeCache.
    private CircularFifoQueue passiveCache;
    private boolean firstPassiveCacheDropHappened;
    // interpolatedPosition holds the most recent interpolated position which is used to check for
    // "area-entered" and "area-left" events.
    private Loc interpolatedPosition;

    private static final LocationCache cache = new LocationCache();
    public static LocationCache getInstance(){
        return cache;
    }

    public void setParameters(int activeQueueSize, int passiveQueueSize, int updateInterval, float interpolationVariance){
        this.activeQueueSize = activeQueueSize;
        activeCache = new CircularFifoQueue<>(activeQueueSize);
        activeInterpolatedCache = new CircularFifoQueue<>(activeQueueSize);

        this.passiveQueueSize = passiveQueueSize;
        passiveCache = new CircularFifoQueue<>(passiveQueueSize);
        this.firstPassiveCacheDropHappened = false;

        this.updateInterval = updateInterval;
        this.interpolationVariance = interpolationVariance;
    }

    public CircularFifoQueue<Loc> getPassiveCache(){
        return passiveCache;
    }

    public void setPassiveCache(CircularFifoQueue<Loc> cache){
        passiveCache = cache;
    }

    public float validateInBoundsForTLA(TargetLocationArea tla){
        float positives = 0;

        for(int i=0; i< activeInterpolatedCache.size(); i++){
            Loc loc = (Loc) activeInterpolatedCache.get(i);
            int distanceToTLABorder = loc.distanceTo(new Loc(tla.getLatitude(), tla.getLongitude())) - tla.getRadius();
            if(distanceToTLABorder < 0){
                positives++;
            }
        }
        return positives/(float) activeInterpolatedCache.size();
    }

    public float getCurrentInBoundProxFor(TargetLocationArea tla){
        float positives = 0;
        float all = activeCache.size();

        for(int i=0; i< activeInterpolatedCache.size(); i++){
            Loc loc = (Loc) activeInterpolatedCache.get(i);
            //int distanceDebug = loc.distanceTo(new Loc(tla.getLatitude(), tla.getLongitude()));
            int distanceTLABorderToUser = loc.distanceTo(new Loc(tla.getLatitude(), tla.getLongitude())) - tla.getRadius();
            if(distanceTLABorderToUser <= 0){
                positives++;
            }
        }
        return positives/all;
    }


    public static double _getAccuracyMultiplier(double acc){
        double accuracyMultiplier = 0.5;
        if(acc > 30){  // dont apply penalty if accuracy is <= 30m
            double x = acc/100; // [0.31 - 30 ]
            accuracyMultiplier += 1.5 * (1 - (2*x / (1+Math.pow(x, 2)))); // term in outer brackets: [0 .. 1]
            // accuracyMultiplier between 0.5 and 2 right now
        } else if(acc == 0.0){     // if no accuracy provided, does that condition exist?
            accuracyMultiplier = 0.75;
        } else {
            accuracyMultiplier = 2;
        }
        return accuracyMultiplier;

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

    public void addLocationUpdate(Loc myLoc){
        activeCache.add(myLoc);
        if(isPassiveCacheFull()){
            firstPassiveCacheDropHappened = true;
        }
        passiveCache.add(myLoc);
        _calcInterpolatedPosition();

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

    public Loc getInterpolatedPosition() {
        return interpolatedPosition;
    }

    public LatLng getInterpolatedPositionInLatLng(){
        return new LatLng(getInterpolatedPosition().getLatitude(), getInterpolatedPosition().getLongitude());
    }

    private double _getStartWeight(int i, int size){
        return interpolationVariance * (((double)(i+1)/(double)activeCache.size()) + 0.5);
    }

    public double _getAgeMultiplier(Loc loc, long currentTime){
        long millisInPast = currentTime - loc.getTimestampInMillis();
        int optimalTimeInCache = activeQueueSize * updateInterval; // ms
        double ageMultiplier = 0; // default. If more than 15min in past, don't weight this location/timestamp anymore

        long tresholdTimeToScoreOfZero = activeQueueSize * updateInterval * 3; // 3 for 3*5=15min in past is treshold
        double slopeOfRegression = 1.5 / (double) tresholdTimeToScoreOfZero;
        if(millisInPast <= tresholdTimeToScoreOfZero){
            // [0 - 1,5]
            ageMultiplier = (tresholdTimeToScoreOfZero-millisInPast)*slopeOfRegression; // x * slope = y = score
        }
        return ageMultiplier;
    }

    private void _calcInterpolatedPosition(){
        int cacheSize = activeCache.size();
        float[] score = new float[cacheSize];
        float scoreSum = 0;
        double latSumCounter = 0, lngSumCounter = 0;
        long currentTime = System.currentTimeMillis();

        for(int i=0; i<cacheSize; i++){
            Loc loc = (Loc) activeCache.get(i);
            double ageMultiplier = _getAgeMultiplier(loc, currentTime);
            double accuracyMultiplier = loc.getAccuracyMultiplier();

            //score[i] = (float)(_getStartWeight(i, cacheSize) - loc.getAccuracyMultiplier());
            score[i] = (float)(1 * ageMultiplier * accuracyMultiplier);
            // weighted arithmethic mean, http://en.wikipedia.org/wiki/Weighted_arithmetic_mean > Mathematical definition
            scoreSum += score[i];
            latSumCounter += loc.getLatitude() * score[i];
            lngSumCounter += loc.getLongitude() * score[i];
        }

        interpolatedPosition = new Loc(latSumCounter/scoreSum, lngSumCounter/scoreSum);
        activeInterpolatedCache.add(interpolatedPosition);

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
