/* Copyright (C) 2009  Axel Müller <axel.mueller@avanux.de> 
 * 
 * This file is part of LiveTracker.
 * 
 * LiveTracker is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * LiveTracker is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with LiveTracker.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.avanux.livetracker.admin;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.avanux.livetracker.Tracking;
import de.avanux.livetracker.TrackingManager;

public class LoadManager implements Runnable {

    private static Log log = LogFactory.getLog(LoadManager.class);

    private final static long CHECK_INTERVAL_SECONDS = 20;

    private static int activeTrackings = 0;
    
    private static int activeTrackers = 0;
    
    @Override
    public void run() {
        long intervalMillis = CHECK_INTERVAL_SECONDS * 1000; 
        while (true) {
            log.debug("Load will be checked every " + CHECK_INTERVAL_SECONDS + " seconds.");
            synchronized (this) {
                try {
                    log.debug("Waiting for " + intervalMillis + " millis");
                    wait(intervalMillis);
                    checkLoad();
                } catch (Exception e) {
                    log.warn("Interrupted.");
                    break;
                }
            }
        }
    }

    public void checkLoad() {
        log.debug("Checking load ...");
        int activeTrackings = 0;
        int activeTrackers = 0;
        for(Tracking tracking : TrackingManager.getTrackings()) {
            if(! tracking.isExpired()) {
                activeTrackings++;
                activeTrackers+=tracking.getTrackerCount();
            }
        }
        LoadManager.activeTrackings = activeTrackings;
        LoadManager.activeTrackers = activeTrackers;
        log.info("Current usage: trackings=" + activeTrackings + " / trackers=" + activeTrackers);
    }

    public static int getActiveTrackings() {
        return activeTrackings;
    }

    public static int getActiveTrackers() {
        return activeTrackers;
    }
}
