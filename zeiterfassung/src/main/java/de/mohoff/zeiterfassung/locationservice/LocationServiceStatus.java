package de.mohoff.zeiterfassung.locationservice;

import java.util.ArrayList;

/**
 * LocationServiceStatus serves as wrapper for a boolean variable which keeps
 * track of the background service LocationService. Apart from a getter and setter,
 * this class also provides listener management. The used ServiceChangeListener will
 * be called when the service status changes. All registered classes can receive
 * such an event now.
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
