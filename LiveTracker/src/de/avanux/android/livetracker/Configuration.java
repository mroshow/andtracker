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
package de.avanux.android.livetracker;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.util.Log;

public class Configuration extends PropertiesStringParser implements OnSharedPreferenceChangeListener {

	private static final long serialVersionUID = 1L;
	
	private static final int SERVER_API_VERSION = 1;
	
	private static final String TAG = "LiveTracker:Configuration";

    private static String serverBaseUrl;
    
	private Long timeInterval;

    private Long minTimeInterval;
	
	private static String timeIntervalPreferenceKey;

	private Float distance;
	
	private static String distancePreferenceKey;

	
	public Configuration(String propertiesString) throws IOException {
		super(propertiesString);
	}

	public static String getServerBaseUrl() {
        // FIXME: this is a hack during main development phase to switch easily between development server and deployment server
	    if(serverBaseUrl == null) {
	        String hostAddress = null;
	        try {
	            InetAddress addr = InetAddress.getByName("miraculix.localnet");
	            if(addr != null) {
	                hostAddress = addr.getHostAddress();
	            }
	        } catch (UnknownHostException e) {
	            e.printStackTrace();
	        }
	        
	        if(hostAddress.equals("192.168.70.5")) {
	            serverBaseUrl = "http://miraculix.localnet:8080/LiveTrackerServer";
	        }
	        else {
	            serverBaseUrl = "http://livetracker.dyndns.org";
	        }
	        Log.d(TAG, "serverBaseUrl=" + serverBaseUrl);
	    }
	    return serverBaseUrl;
	}
	
    public boolean isMatchingServerApiVersion() {
		if(Integer.parseInt(getProperties().getProperty(ConfigurationConstants.SERVER_API_VERSION)) == SERVER_API_VERSION) {
			return true;
		}
		else {
			return false;
		}
	}
	
	public String getID() {
		return getProperties().getProperty(ConfigurationConstants.ID);
	}

	
	public long getTimeInterval() {
		if (timeInterval != null) {
			return this.timeInterval;
		} else {
			return Long.parseLong(getProperties().getProperty(ConfigurationConstants.TIME_INTERVAL));
		}
	}
	public void setTimeInterval(long timeInterval) {
		this.timeInterval = timeInterval;
	}

    public static long getDefaultTimeInterval() {
        return 1;
    }
	
    public long getMinTimeInterval() {
        if (minTimeInterval != null) {
            return this.minTimeInterval;
        } else {
            return Long.parseLong(getProperties().getProperty(ConfigurationConstants.MIN_TIME_INTERVAL));
        }
    }
    public void setMinTimeInterval(long minTimeInterval) {
        this.minTimeInterval = minTimeInterval;
    }
	
	public float getDistance() {
		if (this.distance != null) {
			return distance;
		} else {
			return Float.parseFloat(getProperties().getProperty(ConfigurationConstants.DISTANCE));
		}
	}
	public void setDistance(float distance) {
		this.distance = distance;
	}


    public static long getDefaultDistance() {
        return 0;
    }
	
    public float getMinDistance() {
		return Float.parseFloat(getProperties().getProperty(ConfigurationConstants.MIN_DISTANCE));
    }

    public String getMessageToUsers() {
        return getProperties().getProperty(ConfigurationConstants.MESSAGE_TO_USERS);
    }

	public String getLocationReceiverUrl() {
        return getProperties().getProperty(ConfigurationConstants.LOCATION_RECEIVER_URL);
	}

	//
    // ~ Preferences ----------------------------------------------------------------------------------------------------
	//
	
    public static void setTimeIntervalPreferenceKey(String timeIntervalPreferenceKey) {
        Configuration.timeIntervalPreferenceKey = timeIntervalPreferenceKey;
    }

    public static void setDistancePreferenceKey(String distancePreferenceKey) {
        Configuration.distancePreferenceKey = distancePreferenceKey;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(key.equals(timeIntervalPreferenceKey)) {
            setTimeInterval(Long.parseLong(sharedPreferences.getString(timeIntervalPreferenceKey, "" + getDefaultTimeInterval())));
        }
        else if (key.equals(distancePreferenceKey)) {
            setDistance(Float.parseFloat(sharedPreferences.getString(distancePreferenceKey, "" + getDefaultDistance())));
        }
    }
}
