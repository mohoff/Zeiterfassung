package de.mohoff.zeiterfassung.datamodel;

import com.google.android.gms.maps.model.LatLng;
import org.apache.commons.collections4.queue.CircularFifoQueue;

/**
 * Created by Moritz on 03.10.2014.
 */
public class LocationCache {
    public int amountOfTemporarySavedLocations;
    private static float interpolationVariance = 1;

    private CircularFifoQueue locationCache; // fifo based queue
    private CircularFifoQueue interpolatedCache; // fifo based queue
    private Loc interpolatedPosition;
    // interpolated position cache anlegen


    public LocationCache(int amountOfFields){
        amountOfTemporarySavedLocations = amountOfFields;
        locationCache = new CircularFifoQueue<Loc>(amountOfTemporarySavedLocations);
        interpolatedCache = new CircularFifoQueue<Loc>(amountOfTemporarySavedLocations);
    }

    public float validateInBoundsForTLA(TargetLocationArea tla){
        float result = 0;
        float positives = 0;

        for(int i=0; i<interpolatedCache.size(); i++){
            Loc loc = (Loc)interpolatedCache.get(i);
            int distanceToTLABorder = loc.distanceTo(new Loc(tla.getLatitude(), tla.getLongitude())) - tla.getRadius();
            if(distanceToTLABorder < 0){
                positives++;
            }
        }

        return positives/(float)interpolatedCache.size();
    }

    public static double getPenaltyFromAccuracy(double acc){
        double accuracyPenalty = 0.0;
        if(acc > 30){  // dont apply penalty if accuracy is <= 30m
            double x = acc/100;
            accuracyPenalty = 0.5*x / (1+Math.pow(x, 2));       //    0.5*x/(1+x^2) // bei x=2 schon fast gesättigt (0.5)
            // maybe change factor of 0.5 to 1 or 0.75?
        } else if(acc == 0.0){     // if no accuracy provided
            accuracyPenalty = 0.3;
        }
        accuracyPenalty *= interpolationVariance;
        return accuracyPenalty;
    }

    public void addLocationUpdate(Loc myLoc){
        locationCache.add(myLoc);
        _calcInterpolatedPosition();

    }

    public boolean isFull(){
        return locationCache.size() == locationCache.maxSize();
    }

    public Loc getInterpolatedPosition() {
        return interpolatedPosition;
    }

    private double _getStartWeight(int i, int size){
        return interpolationVariance * (((double)(i+1)/(double)locationCache.size()) + 0.5);
    }

    private void _calcInterpolatedPosition(){
        int cacheSize = locationCache.size();
        float[] score = new float[cacheSize];
        float scoreSum = 0;
        double latSumZaehler = 0, lngSumZaehler = 0;


        for(int i=0; i<cacheSize; i++){
            Loc loc = (Loc) locationCache.get(i);
            score[i] = (float)(_getStartWeight(i, cacheSize) - loc.getAccuracyPenalty());
            // weighted arithmethic mean, http://en.wikipedia.org/wiki/Weighted_arithmetic_mean > Mathematical definition
            scoreSum += score[i];
            latSumZaehler += loc.getLatitude() * score[i];
            lngSumZaehler += loc.getLongitude() * score[i];
        }

        interpolatedPosition = new Loc(latSumZaehler/scoreSum, lngSumZaehler/scoreSum);
        interpolatedCache.add(interpolatedPosition);
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
        double latSumZaehler = 0, lngSumZaehler = 0;

        for(int i=0; i<locationCache.size(); i++){
            Location currentLoc = (Location)locationCache.get(i);
            latSumZaehler += currentLoc.getLatitude() * score[i];
            lngSumZaehler += currentLoc.getLongitude() * score[i];
        }
        LatLng result = new LatLng(latSumZaehler/scoreSum, lngSumZaehler/scoreSum);
        return result;
        */
    }

}