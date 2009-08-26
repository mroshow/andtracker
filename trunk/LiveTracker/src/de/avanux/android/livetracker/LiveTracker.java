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

import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Contacts.People;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class LiveTracker extends Activity implements UpdatableDisplay {

    private static final String INSTANCE_STATE_SAVED = "INSTANCE_STATE_SAVED";

    private static final String CONFIGURATION_URL2 = "http://miraculix.localnet:8080/LiveTrackerServer/ConfigurationProvider";
    // private static final String CONFIGURATION_URL = "http://livetracker.dyndns.org/ConfigurationProvider";

    private final static String TAG = "LiveTracker:LiveTracker";

    
    public static final int MENU_ITEM_ID_INVITE = Menu.FIRST;

    public static final int MENU_ITEM_ID_PREFERENCES = Menu.FIRST + 1;
    
    public static final int MENU_ITEM_ID_ABOUT = Menu.FIRST + 2;

    
    public static final int REQUEST_CODE_PICK_CONTACT_FROM_LIST = 1;

    public static final int REQUEST_CODE_PICK_EMAIL = 2;
    
    
    private boolean instanceStateSaved;

    private LocationTracker locationTracker;

    
    // ~ Life cycle callbacks -------------------------------------------------

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate was called.");
        if(savedInstanceState != null && savedInstanceState.getBoolean(INSTANCE_STATE_SAVED)) {
            Log.d(TAG, "Saved instance state is available - we were probably restarted (e.g. display orientation changed or keypad (de-)activated).");
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }

    @Override
    protected void onRestart() {
        Log.d(TAG, "onRestart was called.");
        super.onRestart();
    }
    
    @Override
    protected void onStart() {
        Log.d(TAG, "onStart was called.");
        super.onStart();
        
        // make preference keys available to configuration
        Configuration.setTimeIntervalPreferenceKey(getString(R.string.preference_timeInterval_key));
        Configuration.setDistancePreferenceKey(getString(R.string.preference_distance_key));
        
        // explicit start of service in order to let it survive an unbind
        startService(new Intent(LiveTracker.this, LocationTracker.class));

        // bind service after it has been started
        bindService(new Intent(LiveTracker.this, LocationTracker.class), locationTrackerConnection, 0);

        Button startButton = (Button) findViewById(R.id.button_start);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Start button was pressed.");
                if (locationTracker != null) {
                    locationTracker.start();
                }
                updateButtons();
            }
        });

        Button stopButton = (Button) findViewById(R.id.button_stop);
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Stop button was pressed.");
                if (locationTracker != null) {
                    locationTracker.stop();
                }
                updateButtons();
            }
        });
    }
    
    @Override
    protected void onResume() {
        Log.d(TAG, "onResume was called.");
        super.onResume();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause was called.");
        super.onPause();
    }
    
    @Override
    protected void onStop() {
        Log.d(TAG, "onRestart was called.");
        super.onStop();
    }
    
    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy was called.");
        super.onDestroy();
        if (locationTracker != null) {
            Log.d(TAG, "Terminate service connection.");
            unbindService(locationTrackerConnection);
            if (this.instanceStateSaved) {
                Log.d(TAG, "We are going to be suspended temporarily (e.g. display orientation changed or keypad (de-)activated).");
            }
            else {
                if (locationTracker.isRunning()) {
                    Log.d(TAG, "Location tracker service is running - leave it running and send a notification as reminder.");
                    notify(R.string.notification_service_running_in_background,
                            getText(R.string.notification_service_running_in_background));
                } else {
                    Log.d(TAG, "The location tracker service is used but not running - it's safe to stop it.");
                    stopService(new Intent(LiveTracker.this, LocationTracker.class));
                }
            }
        } else {
            Log.d(TAG, "The location tracker service is not used - it's safe to stop it.");
            stopService(new Intent(LiveTracker.this, LocationTracker.class));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.d(TAG, "onSaveInstanceState was called.");
        super.onSaveInstanceState(outState);
        outState.putBoolean(INSTANCE_STATE_SAVED, true);
        this.instanceStateSaved = true;
    }

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        // TODO Auto-generated method stub
//        super.onActivityResult(requestCode, resultCode, data);
//        
//        switch (requestCode) {
//            case (REQUEST_CODE_PICK_CONTACT_FROM_LIST) :
//                if(data != null){
//                    final Uri personUri = data.getData();
//                    if(personUri != null){
//                         final Cursor c = getContentResolver().query(personUri, null, null, null, null);
//                         startManagingCursor(c);
//                         if(c != null && c.moveToFirst()){
//                              final long prefferedPhoneID = c.getLong(c.getColumnIndexOrThrow(People.PRIMARY_PHONE_ID));
//
//                              final Uri phoneUri = Uri.withAppendedPath(People.CONTENT_URI, "" + prefferedPhoneID).buildUpon().appendPath(People.Phones.CONTENT_DIRECTORY).build();
//                              final Cursor d = getContentResolver().query(phoneUri, null, null, null, null);
//                              startManagingCursor(c);
//                              if(d != null && d.moveToFirst()){
//                                   final String num = d.getString(d.getColumnIndexOrThrow(People.Phones.NUMBER));
//                                   Log.d(TAG, num);
//                              }
//                         }
//                    }
//               }                     
//                    
////                    Uri contactData = data.getData();
////            
////                    Intent intent = new Intent(Intent.ACTION_VIEW, contactData);
////                    
////                    startActivity(intent);            
//            
//            
////            Cursor c =  managedQuery(contactData, null, null, null, null);
////            if (c.moveToFirst()) {
////              String name = c.getString(c.getColumnIndexOrThrow(People.NAME));
////              // TODO Whatever you want to do with the selected contact name.
////            }
//                    break;
//            case (REQUEST_CODE_PICK_EMAIL) :
//                if (resultCode == Activity.RESULT_OK) {
//                    Object object = data.getData(); 
//                }
//        }
//      }
    
    
    // ~ UI handling ----------------------------------------------------------

    private void setStartButtonEnabled(boolean enabled) {
        Button button = (Button) findViewById(R.id.button_start);
        button.setEnabled(enabled);
        Log.d(TAG, "setStartButtonEnabled: " + enabled);
    }

    private void setStopButtonEnabled(boolean enabled) {
        Button button = (Button) findViewById(R.id.button_stop);
        button.setEnabled(enabled);
        Log.d(TAG, "setStopButtonEnabled: " + enabled);
    }

    public void updateTrackingID() {
        TextView idField = (TextView) findViewById(R.id.field_trackingID);
        idField.setText(getConfiguration().getID());
    }

    public void updateLocationsSentCount(Integer count) {
        String value = getString(R.string.field_locationsSentCount);
        if(count != null) {
            value = "" + count;
        }
        TextView field = (TextView) findViewById(R.id.field_locationsSentCount);
        field.setText(value);
    }

    public void updateLastLocationSentTime(Long timeInMillis) {
        String value = getString(R.string.field_lastLocationSentTime);
        if (timeInMillis != null) {
            String timeFormat = null;
            if (DateFormat.is24HourFormat(this)) {
                timeFormat = "HH:mm:ss";
            } else {
                timeFormat = "hh:mm:ss a";
            }

            Date date = new Date(timeInMillis);
            SimpleDateFormat formatter = new SimpleDateFormat(timeFormat);
            value = formatter.format(date); 
        }
        TextView field = (TextView) findViewById(R.id.field_lastLocationSentTime);
        field.setText(value);
    }

    public void updateTrackerCount(Integer count) {
        String value = getString(R.string.field_trackerCount);
        if(count != null) {
            value = "" + count; 
        }
        TextView field = (TextView) findViewById(R.id.field_trackerCount);
        field.setText(value);
    }

    private void updateButtons() {
        if (locationTracker.isRunning()) {
            setStartButtonEnabled(false);
            setStopButtonEnabled(true);
        } else {
            setStartButtonEnabled(true);
            setStopButtonEnabled(false);
        }
    }

    private UpdatableDisplay getUpdatableDisplay() {
        return this;
    }

    // ~ Service handling -----------------------------------------------------

    private ServiceConnection locationTrackerConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d(TAG, "Connection to location tracker service established.");

            // first of all: make the location tracker available
            locationTracker = ((LocationTracker.LocationTrackerBinder) service).getService();

            // let the location tracker know where to send display updates
            locationTracker.setUpdatableDisplay(getUpdatableDisplay());

            if (getConfiguration() == null) {
                Log.d(TAG, "Location tracker service is not yet configured.");
                new DownloadConfigurationTask().execute(Configuration.getServerBaseUrl() + "/ConfigurationProvider");
            } else {
                Log.d(TAG, "Location tracker service is already configured.");
                updateTrackingID();
                updateLocationsSentCount(locationTracker.getLocationsSent());
                updateLastLocationSentTime(locationTracker.getLastTimePosted());
                updateButtons();
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            locationTracker = null;
        }
    };

    
    // ~ Options menu ---------------------------------------------------------

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean result = super.onCreateOptionsMenu(menu);
        menu.add(MENU_ITEM_ID_INVITE, MENU_ITEM_ID_INVITE, 0, R.string.menu_invite)
            .setIcon(R.drawable.ic_menu_allfriends);
        menu.add(MENU_ITEM_ID_PREFERENCES, MENU_ITEM_ID_PREFERENCES, 1, R.string.menu_preferences)
            .setIcon(R.drawable.ic_menu_preferences);
        menu.add(MENU_ITEM_ID_ABOUT, MENU_ITEM_ID_ABOUT, 2, R.string.menu_about)
            .setIcon(R.drawable.ic_info);

        return result;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if(getConfiguration() != null) {
            menu.setGroupEnabled(MENU_ITEM_ID_PREFERENCES, true);
        }
        else {
            menu.setGroupEnabled(MENU_ITEM_ID_PREFERENCES, false);
        }
        
        if((getConfiguration() != null) && (InviteContactsByEmailActivity.isStartable(this))) {
            menu.setGroupEnabled(MENU_ITEM_ID_INVITE, true);
        }
        else {
            menu.setGroupEnabled(MENU_ITEM_ID_INVITE, false);
        }
        
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = null;
        switch (item.getItemId()) {
        case MENU_ITEM_ID_INVITE:
            intent = new Intent(this, InviteContactsByEmailActivity.class);
            intent.putExtra(InviteContactsByEmailActivity.EXTRA_TRACKING_ID, getConfiguration().getID());
            startActivity(intent);
            return true;
        case MENU_ITEM_ID_PREFERENCES:
            intent = new Intent(this, PreferencesActivity.class);
            intent.putExtra(ConfigurationConstants.MIN_TIME_INTERVAL, this.locationTracker.getConfiguration().getMinTimeInterval());
            intent.putExtra(ConfigurationConstants.MIN_DISTANCE, this.locationTracker.getConfiguration().getMinDistance());
            startActivity(intent);
            return true;
        case MENU_ITEM_ID_ABOUT:
            startActivity(new Intent(this, AboutActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    
    // ~ Configuration --------------------------------------------------------

    private void setConfiguration(Configuration configuration) {
        if (locationTracker != null) {
            locationTracker.setConfiguration(configuration);
            
            // make sure that changed preferences take effect
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            sharedPreferences.registerOnSharedPreferenceChangeListener(configuration);
        }
        if (configuration.getMessageToUsers() != null && configuration.getMessageToUsers().length() > 0) {
            notify(R.string.notification_message_to_users, configuration.getMessageToUsers());
        }
    }

    private Configuration getConfiguration() {
        if (locationTracker != null) {
            return locationTracker.getConfiguration();
        } else {
            return null;
        }
    }

    private class DownloadConfigurationTask extends AsyncTask<String, Void, Configuration> {

        private boolean configurationReceived = false;

        protected Configuration doInBackground(String... params) {
            Log.d(TAG, "Downloading configuration ...");
            Configuration configuration = null;
            try {
                String configurationString = HttpUtil.get(params[0]);
                Log.d(TAG, "... configuration downloaded.");
                if (configurationString != null) {
                    configuration = new Configuration(configurationString);
                    configurationReceived = true;
                }
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
            return configuration;
        }

        protected void onPostExecute(Configuration result) {
            if (!configurationReceived) {
                Toast.makeText(LiveTracker.this, getString(R.string.toast_serverNotAvailable), Toast.LENGTH_LONG)
                        .show();
            } else {
                if (result.isMatchingServerApiVersion()) {
                    setConfiguration(result);
                    updateTrackingID();
                    updateButtons();
                } else {
                    Toast.makeText(LiveTracker.this, getString(R.string.toast_versionCodeMismatch), Toast.LENGTH_LONG)
                            .show();
                }
            }
        }
    }

    // ~ Notifications --------------------------------------------------------

    private void notify(int id, CharSequence text) {
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, LiveTracker.class), 0);

        Notification notification = new Notification(R.drawable.icon, text, System.currentTimeMillis());
        notification.setLatestEventInfo(this, getText(R.string.app_name), text, contentIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(id, notification);
    }

}