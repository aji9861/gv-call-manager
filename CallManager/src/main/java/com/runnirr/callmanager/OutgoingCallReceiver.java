package com.runnirr.callmanager;

import android.content.*;

import android.content.BroadcastReceiver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.PhoneLookup;
import android.util.Log;
import android.widget.Toast;


import java.util.*;

/**
 * Created by Adam on 5/21/13.
 *
 * Handles outgoing calls and checks if the called contact is in a google voice group
 * If they aren't, redirect the calls through the standard phone number
 */
public class OutgoingCallReceiver extends BroadcastReceiver {

    @Override
     public void onReceive(Context context, Intent intent) {
        final String phoneNumber = getResultData();//intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
        Uri phoneLookupURI = PhoneLookup.CONTENT_FILTER_URI;
        if(phoneLookupURI == null){
            return;
        }
        final Uri phoneUri = Uri.withAppendedPath(phoneLookupURI, Uri.encode(phoneNumber));
        String message = "Calling " + phoneNumber + " using ";

        if(isGoogleVoice(phoneUri, context)){
            message += "Google Voice.";
        } else{
            setResultData(null);
            message += "regular number.";

            new PhoneActivity().call(phoneNumber);
        }
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();

    }

    /**
     * Check if the specified number is part of a google voice group
     * @param number Uri number format
     * @param context Of the receiver
     * @return false if it cannot be determined
     */
    private boolean isGoogleVoice(Uri number, Context context){
        final String googleVoiceGroupTitle = "google voice";
        final ContentResolver resolver = context.getContentResolver();

        Set<String> contactIds = getContactId(number, resolver);
        Set<String> googleVoiceGroupIds = getGoogleVoiceGroupIds(resolver, googleVoiceGroupTitle);

        if(contactIds == null || googleVoiceGroupIds == null){
            return false;
        }

        Uri groupURI = ContactsContract.Data.CONTENT_URI;
        if(groupURI == null){
            return false;
        }

        // String to check
        String[] projection = new String[]{
                ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID ,
                ContactsContract.CommonDataKinds.GroupMembership.CONTACT_ID
        };

        // Build query for all contact ids
        StringBuilder query = new StringBuilder("(");
        for (int i = 0; i < contactIds.size(); i++){
            query.append(ContactsContract.CommonDataKinds.GroupMembership.CONTACT_ID + "=?");
            if(i < contactIds.size() - 1){
                query.append(" OR ");
            }
        }
        query.append(") AND (");
        // Add queries for group ids
        for (int i = 0; i < googleVoiceGroupIds.size(); i++){
            query.append(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID + "=?");
            if(i < googleVoiceGroupIds.size() - 1){
                query.append(" OR ");
            }
        }
        query.append(")");

        // Get an array of values for the built query
        List<String> listValues = new ArrayList<String>();
        listValues.addAll(contactIds);
        listValues.addAll(googleVoiceGroupIds);
        String[] values = listValues.toArray(new String[listValues.size()]);

        Cursor c = resolver.query(groupURI,
                projection,
                query.toString(),
                values,null);

        // If there is a first row then one of the contacts are in a google voice group
        if(c != null && c.moveToFirst()){
            log("We found a match somewhere");
            return true;
        }

        return false;
    }

    /**
     * Get a set of contact ids based on a specified number. A number can appear more than once
     * so we find all contacts with the number
     * @param number Uri format for the phone number
     * @param resolver for performing database queries
     * @return Set of contactIds or null if not found
     */
    private Set<String> getContactId(Uri number, ContentResolver resolver){
        Set<String> contactIds = new HashSet<String>();

        if(resolver == null){
            return null;
        }
        Cursor cc = resolver.query(number, new String[]{PhoneLookup._ID}, null, null, null);

        if(cc == null){
            return null;
        }

        while(cc.moveToNext()){
            contactIds.add(cc.getString(cc.getColumnIndex(PhoneLookup._ID)));
        }

        return contactIds;
    }

    /**
     * Get the android group id for groups that match this.useGoogleVoiceGroupTitle field
     * It is possible to have 2 groups with the same name and different id so we want to find
     * all of them
     * @param resolver for performing database queries
     * @return Set of group ids or null if not found
     */
    private Set<String> getGoogleVoiceGroupIds(ContentResolver resolver, String useGoogleVoiceGroupTitle){
        Set<String> googleVoiceGroupIds = new HashSet<String>();
        Uri groupURI = ContactsContract.Groups.CONTENT_URI;
        if(groupURI == null){
            return null;
        }
        Cursor gc = resolver.query(
                groupURI,
                new String[]{
                        ContactsContract.Groups._ID,
                        ContactsContract.Groups.TITLE,
                },
                null, null, null
        );

        if(gc == null){
            return null;
        }

        while(gc.moveToNext()){
            for(int i = 0; i < gc.getColumnCount(); i++){
                String groupId = gc.getString(gc.getColumnIndex(ContactsContract.Groups._ID));
                String groupTitle = gc.getString(gc.getColumnIndex(ContactsContract.Groups.TITLE));
                if(groupTitle != null && groupTitle.equals(useGoogleVoiceGroupTitle)){
                    googleVoiceGroupIds.add(groupId);
                }
            }
        }

        return googleVoiceGroupIds;
    }

    private void log(String msg){
        final String logTag = "CallManager";
        Log.d(logTag, msg);
    }
}