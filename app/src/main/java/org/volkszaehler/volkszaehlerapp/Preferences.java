package org.volkszaehler.volkszaehlerapp;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.util.Log;

public class Preferences extends PreferenceActivity {
    private boolean newChannels = false;
    private ProgressDialog pDialog;

    // URL to get contacts JSON
    private static String url = "http://demo.volkszaehler.org/middleware.php/entity.json";
    private static String uname;
    private static String pwd;
    private static String tuples;

    private ArrayList<HashMap<String, String>> channelList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstancesState) {
        super.onCreate(savedInstancesState);
        addPreferencesFromResource(R.xml.volkszaehler_preferences);

        // Preference button = (Preference)findPreference("");
        Preference button = getPreferenceManager().findPreference("getChannelsButton");
        if (button != null) {
            button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference arg0) {

                    // obviously the only way to remove old dynamic preferences
                    // is prefs.edit().clear().commit(); but it removes also
                    // other preferences
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    // so hold them in a variable
                    url = prefs.getString("volkszaehlerURL", "");
                    uname = prefs.getString("username", "");
                    pwd = prefs.getString("password", "");
                    boolean bZeroBased = prefs.getBoolean("ZeroBasedYAxis", false);
                    boolean bAutoReload = prefs.getBoolean("autoReload", false);
                    tuples = prefs.getString("Tuples","1000");


                    // remove all
                    prefs.edit().clear().commit();
                    // and put back
                    prefs.edit().putString("volkszaehlerURL", url).commit();
                    prefs.edit().putString("username", uname).commit();
                    prefs.edit().putString("password", pwd).commit();
                    prefs.edit().putBoolean("ZeroBasedYAxis", bZeroBased).commit();
                    prefs.edit().putBoolean("autoReload", bAutoReload).commit();
                    prefs.edit().putString("Tuples", tuples).commit();
                    // call Channels from VZ installation
                    new GetChannels().execute();
                    return true;
                }
            });
        }
        // fill PreferenceScreen dynamically with new channels
        addPreferenceChannels();
    }

    @Override
    public void onBackPressed() {
        // just for to know that preferences was maybe changed, resulting in possibility to reload data
        Intent intent = new Intent();
        setResult(RESULT_OK, intent);
        super.onBackPressed();
    }

    private void addPreferenceChannels() {
        SharedPreferences prefs = getSharedPreferences(Tools.JSON_CHANNEL_PREFS, Activity.MODE_PRIVATE);
        String JSONChannels = prefs.getString(Tools.JSON_CHANNELS, "");
        Log.d("Preferences", "JSONChannels" + JSONChannels);
        if (JSONChannels.equals("")) {
            return;
        }
        PreferenceCategory targetCategory = (PreferenceCategory) findPreference("channel_preference_category");
        if (newChannels) {
            targetCategory.removeAll();
            newChannels = false;
        }
        getChannelsFromJSON(JSONChannels);

        if (channelList != null) {
            for (HashMap<String, String> channel : channelList) {

                CheckBoxPreference checkBoxPreference = new CheckBoxPreference(this);

                if ("group".equals(channel.get(Tools.TAG_TYPE))) {
                    checkBoxPreference.setTitle(channel.get(Tools.TAG_TITLE) + " " + getString(R.string.group));
                    // checkBoxPreference.setEnabled(false);
                } else if (channel.containsKey(Tools.TAG_BELONGSTOGROUP) && !((channel.get(Tools.TAG_BELONGSTOGROUP)).equals("") || channel.get(Tools.TAG_BELONGSTOGROUP) == null)) {
                    for (HashMap<String, String> channel2 : channelList) {
                        if (channel.get(Tools.TAG_BELONGSTOGROUP).equals(channel2.get(Tools.TAG_UUID))) {
                            checkBoxPreference.setTitle(channel.get(Tools.TAG_TITLE) + " " + getString(R.string.belongsTo) + " " + channel2.get(Tools.TAG_TITLE) + ")");
                        }
                    }
                } else {
                    checkBoxPreference.setTitle(channel.get(Tools.TAG_TITLE));
                }
                checkBoxPreference.setKey(channel.get(Tools.TAG_UUID));
                checkBoxPreference.setSummary(channel.get(Tools.TAG_DESCRIPTION) + "\n" + channel.get(Tools.TAG_UUID));
                targetCategory.addPreference(checkBoxPreference);
            }

        }
    }

    private void getChannelsFromJSON(String jsonStr) {
        // try {
        int size = channelList.size();
        for (int i = 0; i < size; i++) {
            channelList.remove(0);
        }
        channelList = Tools.getChannelsFromJSONStringEntities(jsonStr);
        // channelList = Tools.removeExistingChannel(channelList);
        Log.d("Preferences", "channelList" + channelList);
    }

    private class GetChannels extends AsyncTask<Void, Void, String> {
        boolean JSONFehler = false;
        boolean JSONFehler2 = false;
        String fehlerAusgabe = "";
        String fehlerAusgabe2 = "";

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Showing progress dialog
            pDialog = new ProgressDialog(Preferences.this);
            pDialog.setMessage(getString(R.string.please_wait));
            pDialog.setCancelable(false);
            pDialog.show();

        }

        @Override
        protected String doInBackground(Void... arg0) {
            // Creating service handler class instance
            ServiceHandler sh = new ServiceHandler();
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(Preferences.this);
            url = sharedPref.getString("volkszaehlerURL", "");

            // get also definitions (Units)
            String urlDef = url + "/capabilities/definitions/entities.json";

            url = url + "/entity.json";
            String jsonStr;
            String jsonStrDef;

            uname = sharedPref.getString("username", "");
            pwd = sharedPref.getString("password", "");
            Log.d("Preferences", "url: " + url);
            Log.d("Preferences", "urlDef: " + urlDef);

            // Making a request to url and getting response
            if (uname.equals("")) {
                jsonStr = sh.makeServiceCall(url, ServiceHandler.GET);
                jsonStrDef = sh.makeServiceCall(urlDef, ServiceHandler.GET);
            } else {
                jsonStr = sh.makeServiceCall(url, ServiceHandler.GET, null, uname, pwd);
                jsonStrDef = sh.makeServiceCall(urlDef, ServiceHandler.GET, null, uname, pwd);
            }
            if (jsonStr != null) {
                if (!jsonStr.startsWith("{\"version\":\"0.3\",\"entities")) {
                    JSONFehler = true;
                    fehlerAusgabe = jsonStr;
                } else {
                    newChannels = true;
                    Log.d("Preferences", "jsonStr: " + jsonStr);
                    // store all channel stuff in a shared preference
                    getApplicationContext().getSharedPreferences(Tools.JSON_CHANNEL_PREFS, Activity.MODE_PRIVATE).edit().putString(Tools.JSON_CHANNELS, jsonStr).commit();
                }
                if (!jsonStrDef.startsWith("{\"version\":\"0.3\",\"capabilities")) {
                    JSONFehler2 = true;
                    fehlerAusgabe2 = jsonStrDef;
                } else {
                    Log.d("Preferences", "jsonStrDef: " + jsonStrDef);
                    // store all definitions stuff in a shared preference
                    getApplicationContext().getSharedPreferences(Tools.JSON_CHANNEL_PREFS, Activity.MODE_PRIVATE).edit().putString(Tools.JSON_DEFINITIONS, jsonStrDef).commit();

                }
            } else {
                Log.e("Preferences", "Couldn't get any data from the url");
            }

            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            try {
                // Dismiss the progress dialog
                if (pDialog.isShowing())
                    pDialog.dismiss();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                // handle Exception
            }
            if (JSONFehler || JSONFehler2) {
                if (JSONFehler) {
                    new AlertDialog.Builder(Preferences.this).setTitle(getString(R.string.Error)).setMessage(fehlerAusgabe).setNeutralButton(getString(R.string.Close), null).show();
                }
                if (JSONFehler2) {
                    new AlertDialog.Builder(Preferences.this).setTitle(getString(R.string.Error)).setMessage(fehlerAusgabe2).setNeutralButton(getString(R.string.Close), null).show();
                }
            } else {
                addPreferenceChannels();
            }
        }
    }
}
