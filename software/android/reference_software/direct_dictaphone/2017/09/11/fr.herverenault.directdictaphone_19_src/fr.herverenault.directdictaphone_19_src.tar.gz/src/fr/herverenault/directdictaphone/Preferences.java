package fr.herverenault.directdictaphone;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.widget.Toast;

import java.util.regex.Pattern;

public class Preferences extends PreferenceActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        findPreference("max_recording_time").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                if (newValue == null
                        || !Pattern.matches("^\\d{1,5}$", newValue.toString())
                        || Integer.parseInt(newValue.toString()) < 1) {
                    Toast.makeText(getApplicationContext(), getString(R.string.invalid_number), Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;
            }
        });
    }
}