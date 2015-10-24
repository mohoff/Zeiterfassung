package de.mohoff.zeiterfassung.locationservice;

import java.util.ArrayList;

/**
 * Created by moo on 10/24/15.
 */
public class LocationServiceStatus {
    private boolean isServiceRunning;
    ArrayList<ServiceChangeListener> listeners = new ArrayList<>();
    public boolean get(){
        return isServiceRunning;
    }
    public void set(boolean newValue){
        if(isServiceRunning != newValue){
            isServiceRunning = !isServiceRunning;
            for(ServiceChangeListener listener : listeners){
                listener.onServiceStatusEvent(isServiceRunning);
            }
        }
    }
    public void addListener(ServiceChangeListener listener){
        listeners.add(listener);
    }
    public void removeListener(ServiceChangeListener listener){
        listeners.remove(listener);
    }
}
