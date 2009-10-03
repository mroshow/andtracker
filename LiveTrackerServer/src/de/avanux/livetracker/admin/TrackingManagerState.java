package de.avanux.livetracker.admin;

public class TrackingManagerState {

    private int activeTrackings;
    private int activeTrackers;
    
    public TrackingManagerState(int activeTrackings, int activeTrackers) {
        this.activeTrackings = activeTrackings;
        this.activeTrackers = activeTrackers;
    }

    public int getActiveTrackings() {
        return activeTrackings;
    }

    public int getActiveTrackers() {
        return activeTrackers;
    }
}
