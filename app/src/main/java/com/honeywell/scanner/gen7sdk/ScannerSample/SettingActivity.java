package com.honeywell.scanner.gen7sdk.ScannerSample;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

public class SettingActivity extends Activity {
    private static final String TAG 				  = "SettingActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        this.getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(SettingActivity.this);

    }
}
