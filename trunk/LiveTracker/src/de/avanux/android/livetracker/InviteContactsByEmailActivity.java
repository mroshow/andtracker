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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.ExpandableListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.Contacts;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckedTextView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.ExpandableListView.OnChildClickListener;

/**
 * 
 * - check isStartable(Activity) before starting this activity : in forum fragen, ob es dafür standard-Lösung gibt
 * - filters out contacts having no email address 
 * 
 * @author axel
 *
 */
public class InviteContactsByEmailActivity extends ExpandableListActivity implements OnChildClickListener {

    private final static String TAG = "LiveTracker:InviteContactsByEmailActivity";
    
    private final String GROUP_NAME = "contactName";
    private final String CHILD_NAME = "contactEmail";

    public static final int MENU_ITEM_ID_SEND_INVITATION = Menu.FIRST;
    
    public static final String EXTRA_TRACKING_ID = "TRACKING_ID";
    
    private Collection<String> selectedEmailAddresses = new HashSet<String>();
    
    private String trackingID;

    
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        this.trackingID = getIntent().getStringExtra(EXTRA_TRACKING_ID);
        
        List<Map<String, String>> groups = new ArrayList<Map<String, String>>();
        List<List<Map<String, String>>> children = new ArrayList<List<Map<String, String>>>();
        List<Map<String, String>> childrenForAGroup = new ArrayList<Map<String, String>>();

        int previousId = -1;
        Cursor cursor = getContactCursor(this);
        do {
            int personId = cursor.getInt(cursor.getColumnIndex(Contacts.ContactMethods.PERSON_ID));

            // if this person is already added in list, do not add again but
            // just add it's information
            if (personId != previousId) {
                String personName = cursor.getString(cursor.getColumnIndex(Contacts.ContactMethods.DISPLAY_NAME));

                Map<String, String> groupMap = new HashMap<String, String>();
                groupMap.put(GROUP_NAME, personName);
                groups.add(groupMap);

                if (previousId != -1) {
                    children.add(childrenForAGroup);
                    childrenForAGroup = new ArrayList<Map<String, String>>();
                }
            }

            int dataKind = cursor.getInt(cursor.getColumnIndex(Contacts.ContactMethods.KIND));

            // Pick only data with id=1 which represents email address
            if (dataKind == 1) {
                String personEmail = cursor.getString(cursor.getColumnIndex(Contacts.ContactMethods.DATA));

                Map<String, String> curChildMap = new HashMap<String, String>();
                curChildMap.put(CHILD_NAME, personEmail);
                childrenForAGroup.add(curChildMap);
            }

            previousId = personId;

        } while (cursor.moveToNext());

        // add the last set of children to the children's list
        children.add(childrenForAGroup);

        // find out the indexes of empty children lists and remove them and
        // their respective groups
        List indexes = this.findEmptyChildrenLists(children);
        for (int i = 0; i < indexes.size(); i++) {
            groups.remove(Integer.parseInt(indexes.get(i).toString()));
            children.remove(Integer.parseInt(indexes.get(i).toString()));
        }

        setListAdapter(
            new SimpleExpandableListAdapter(
                this,
                groups,
                android.R.layout.simple_expandable_list_item_1,
                new String[] { GROUP_NAME },
                new int[] { android.R.id.text1 },
                children,
                android.R.layout.simple_list_item_multiple_choice,
                new String[] { CHILD_NAME },
                new int[] { android.R.id.text1 }
            )
        );
    }

    private List findEmptyChildrenLists(List<?> list) {
        List indexes = new ArrayList();
        for (int i = 0; i < list.size(); i++) {
            if (((List<?>) list.get(i)).isEmpty()) {
                indexes.add(i);
            }
        }
        return indexes;
    }

    private void removeEmptyGroupsAndChildren(List indexes, List groups, List children) {
        for (int i = 0; i < indexes.size(); i++) {
            groups.remove(indexes.get(i));
            children.remove(indexes.get(i));
        }
    }

    @Override
    public boolean onChildClick(android.widget.ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
        // update child view
        CheckedTextView tempView = (CheckedTextView) v.findViewById(android.R.id.text1);
        tempView.setChecked(!tempView.isChecked());
        
        // update list of selected email addresses
        String emailAddress = ((TextView) v).getText().toString();
        if(tempView.isChecked()) {
            this.selectedEmailAddresses.add(emailAddress);
        }
        else {
            this.selectedEmailAddresses.remove(emailAddress);
        }
        
        return super.onChildClick(parent, v, groupPosition, childPosition, id);
    }

    private static Cursor getContactCursor(Activity activity) {
        // the columns the query should return
        String[] returnColumns = new String[] {
                Contacts.ContactMethods.PERSON_ID,
                Contacts.ContactMethods.DISPLAY_NAME,
                Contacts.ContactMethods.KIND,
                Contacts.ContactMethods.DATA 
                };

        // select returnColumns of all contacts and sort by display name
        Cursor cursor = activity.managedQuery(Contacts.ContactMethods.CONTENT_URI, returnColumns, null, null, Contacts.ContactMethods.DISPLAY_NAME + " ASC");
        // move to the first row of the results table
        cursor.moveToFirst();
        return cursor;
    }
    
    public static boolean isStartable(Activity activity) {
        Cursor cursor = getContactCursor(activity);
        return cursor.getCount() > 0;
    }

    
    // ~ Options Menu ---------------------------------------------------------
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean result = super.onCreateOptionsMenu(menu);
        menu.add(MENU_ITEM_ID_SEND_INVITATION, MENU_ITEM_ID_SEND_INVITATION, 0, R.string.menu_send_invitation)
            .setIcon(R.drawable.ic_menu_send);
        return result;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if(this.selectedEmailAddresses.size() > 0) {
            menu.setGroupEnabled(MENU_ITEM_ID_SEND_INVITATION, true);
        }
        else {
            menu.setGroupEnabled(MENU_ITEM_ID_SEND_INVITATION, false);
        }
        
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_ITEM_ID_SEND_INVITATION:
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_EMAIL, (String[]) this.selectedEmailAddresses.toArray(new String[] {}));
            intent.putExtra(Intent.EXTRA_SUBJECT, getText(R.string.mail_subject));
            intent.putExtra(Intent.EXTRA_TEXT, getText(R.string.mail_body) + " " + Configuration.getServerBaseUrl() + "?id=" + this.trackingID);
            
            /* The "MessageCompose" activity of the default "Email" application
             * uses the following intent filters for action SEND:
             * 
             *  text/plain, image/*, video/*
             * 
             * The "MessageCompose" activity of the K9 email application
             * uses the following intent filters for action SEND:
             *  
             *  text/*, image/*, message/*
             * 
             * This leaves "text/plain" as the only choice in order to be able to use both
             * email applications. Unfortunately the "Messaging" (SMS) application is also
             * registered for "text/plain" :-(
             * Hopefully the default "Email" application will also support something like
             * "message/rfc822" in the future. 
             */
            intent.setType("text/plain");
            startActivity(Intent.createChooser(intent, "Send invitations"));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    
}
