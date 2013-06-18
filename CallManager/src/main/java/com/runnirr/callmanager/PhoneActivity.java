package com.runnirr.callmanager;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

/**
 * Created by Adam on 5/22/13.
 *
 * Create a call through standard phone app
 */
public class PhoneActivity extends Activity{

    void call(String phoneNumber) {
        try {
            Uri phoneUri = Uri.parse("tel:" + phoneNumber);
            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(phoneUri);
            startActivity(callIntent);
        } catch (Exception e) {
            Log.e("CallManager", "Call failed", e);
        }
    }

}
