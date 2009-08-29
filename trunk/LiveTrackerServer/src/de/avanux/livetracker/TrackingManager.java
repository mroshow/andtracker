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
package de.avanux.livetracker;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import de.avanux.livetracker.statistics.PeriodicStatistics;
import de.avanux.livetracker.statistics.StatisticsManager;

public class TrackingManager implements Runnable {

    private static Log log = LogFactory.getLog(TrackingManager.class);

    private final static long REMOVE_EXPIRED_TRACKINGS_INTERVAL_SECONDS = 24 * 60 * 60;
    
    private static Map<Integer, Tracking> trackings = new HashMap<Integer, Tracking>();

    public static synchronized Tracking createTracking() {
        int trackingID = getTrackingID();
        Tracking tracking = new Tracking(trackingID);
        trackings.put(trackingID, tracking);
        return tracking;
    }

    private static int getTrackingID() {
        int trackingID = 0;
        int testTrackingID = 1;
        while(trackingID == 0) {
            if(! trackings.keySet().contains(testTrackingID)) {
                trackingID = testTrackingID;
            }
            else {
                testTrackingID++;
            }
        }
        log.debug(trackingID + " created.");
        return trackingID;
    }

    public static synchronized void removeTracking(Tracking tracking) {
        trackings.remove(tracking.getTrackingID());
        log.debug(tracking.getTrackingID() + " tracking removed.");
    }

    public static Tracking getTracking(int trackingID) {
        return trackings.get(trackingID);
    }

    public static Collection<Tracking> getTrackings() {
        return trackings.values();
    }

    @Override
    public void run() {
        long intervalMillis = REMOVE_EXPIRED_TRACKINGS_INTERVAL_SECONDS * 1000; 
        while (true) {
            log.debug("Expired trackings will be removed every " + REMOVE_EXPIRED_TRACKINGS_INTERVAL_SECONDS + " seconds.");
            synchronized (this) {
                try {
                    log.debug("Waiting for " + intervalMillis + " millis");
                    wait(intervalMillis);
                    removedExpiredTrackings();
                } catch (Exception e) {
                    log.warn("Interrupted.", e);
                    break;
                }
            }
        }
    }
    
    private void removedExpiredTrackings() {
        PeriodicStatistics periodicStatistics = new PeriodicStatistics();
        log.debug("There are " + trackings.size() + " trackings before clean-up.");
        Collection<Integer> expiredTrackingIDs = new HashSet<Integer>();
        for (Tracking tracking : trackings.values()) {
            if(tracking.isExpired()) {
                periodicStatistics.addTrackingStatistics(tracking.getStatistics());
                expiredTrackingIDs.add(tracking.getTrackingID());
            }
        }
        trackings.keySet().removeAll(expiredTrackingIDs);
        log.debug("Removed " + expiredTrackingIDs.size() + " trackings.");
        StatisticsManager.addPeriodicStatistics(periodicStatistics);
    }
    
}
