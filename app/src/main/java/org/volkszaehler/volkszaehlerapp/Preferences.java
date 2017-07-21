package org.volkszaehler.volkszaehlerapp;

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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class Preferences extends PreferenceActivity {
    // URL to get contacts JSON
    private static String url = "http://demo.volkszaehler.org/middleware.php/entity.json";
    private static String uname;
    private static String pwd;
    private static String tuples;
    private static String privateChannelString;
    private static String sortChannelMode;
    private boolean newChannels = false;
    private ProgressDialog pDialog;
    private ArrayList<HashMap<String, String>> channelList = new ArrayList<>();
    private String allCheckedChannels = "";

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
                    sortChannelMode = prefs.getString("sortChannelMode", "off");
                    tuples = prefs.getString("Tuples", "1000");
                    privateChannelString = prefs.getString("privateChannelUUIDs", "");
                    //keep all checked channels
                    allCheckedChannels = Tools.getCheckedChannels(getApplicationContext());
                    // remove all
                    prefs.edit().clear().commit();
                    // and put back
                    prefs.edit().putString("volkszaehlerURL", url).commit();
                    prefs.edit().putString("username", uname).commit();
                    prefs.edit().putString("password", pwd).commit();
                    prefs.edit().putBoolean("ZeroBasedYAxis", bZeroBased).commit();
                    prefs.edit().putBoolean("autoReload", bAutoReload).commit();
                    prefs.edit().putString("sortChannelMode", sortChannelMode).commit();
                    prefs.edit().putString("Tuples", tuples).commit();
                    prefs.edit().putString("privateChannelUUIDs", privateChannelString).commit();
                    // call Channels from VZ installation
                    new GetChannels().execute();
                    return true;
                }
            });
        }
        // fill PreferenceScreen dynamically with new channels
        addPreferenceChannels(allCheckedChannels);
    }

    @Override
    public void onBackPressed() {
        // just for to know that preferences was maybe changed, resulting in possibility to reload data
        Intent intent = new Intent();
        setResult(RESULT_OK, intent);
        super.onBackPressed();
    }

    private void addPreferenceChannels(String allCheckedChannels) {
        SharedPreferences prefs = getSharedPreferences(Tools.JSON_CHANNEL_PREFS, Activity.MODE_PRIVATE);
        String JSONChannels = prefs.getString(Tools.JSON_CHANNELS, "");
        Log.d("Preferences", "JSONChannels: " + JSONChannels);
        if (JSONChannels.equals("")) {
            //Todo: sinnfrei?
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
                } else if (channel.containsKey(Tools.TAG_BELONGSTOGROUP) && !((channel.get(Tools.TAG_BELONGSTOGROUP)).equals("") || channel.get(Tools.TAG_BELONGSTOGROUP) == null)) {
                    for (HashMap<String, String> groupChannel : channelList) {
                        if (channel.get(Tools.TAG_BELONGSTOGROUP).equals(groupChannel.get(Tools.TAG_UUID))) {
                            checkBoxPreference.setTitle(channel.get(Tools.TAG_TITLE) + " " + getString(R.string.belongsTo) + " " + groupChannel.get(Tools.TAG_TITLE) + ")");
                            break;
                        }
                    }
                } else {
                    checkBoxPreference.setTitle(channel.get(Tools.TAG_TITLE));
                }
                checkBoxPreference.setKey(channel.get(Tools.TAG_UUID));
                checkBoxPreference.setSummary(channel.get(Tools.TAG_DESCRIPTION) + "\n" + channel.get(Tools.TAG_UUID));
                targetCategory.addPreference(checkBoxPreference);
                // check preference
                for(String checkedChannel : allCheckedChannels.split(",")) {
                    if (channel.get(Tools.TAG_UUID).equals(checkedChannel)) {
                        checkBoxPreference.setChecked(true);
                        prefs.edit().putBoolean(checkedChannel, true).commit();
                    }
                }
            }

        }
    }

    private void getChannelsFromJSON(String jsonStr) {
        // try {
        int size = channelList.size();
        for (int i = 0; i < size; i++) {
            channelList.remove(0);
        }
        channelList = Tools.getChannelsFromJSONStringEntities(jsonStr, getApplicationContext());
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

            //private channels
            String privateChannelString = sharedPref.getString("privateChannelUUIDs", "");
            String[] privateChannels = privateChannelString.split(",");
            String baseURL = url;

            // get also definitions (Units)
            String urlDef = url + "/capabilities/definitions/entities.json";

            url = url + "/entity.json";
            String jsonStr;
            String jsonStrDef;
            String jsonprivateStr;

            JSONObject jsonStrObj = null;

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

            if (jsonStr.startsWith("Error: ") || jsonStrDef.startsWith("Error: ")) {
                JSONFehler = true;
                fehlerAusgabe = jsonStr + " | " + jsonStrDef;
            } else {
                try {
                    jsonStrObj = new JSONObject(jsonStr);
                    JSONObject jsonStrDefObj = new JSONObject(jsonStrDef);
                    if (jsonStrObj.has("entities") && !jsonStrObj.getString("entities").equals("[]")) {
                        newChannels = true;
                        Log.d("Preferences", "jsonStr: " + jsonStr);
                        // store all channel stuff in a shared preference
                        //getApplicationContext().getSharedPreferences(Tools.JSON_CHANNEL_PREFS, Activity.MODE_PRIVATE).edit().putString(Tools.JSON_CHANNELS, jsonStr).commit();
                    }
                    if (jsonStrDefObj.has("capabilities")) {
                        Log.d("Preferences", "jsonStrDef: " + jsonStrDef);
                        // store all definitions stuff in a shared preference
                        getApplicationContext().getSharedPreferences(Tools.JSON_CHANNEL_PREFS, Activity.MODE_PRIVATE).edit().putString(Tools.JSON_DEFINITIONS, jsonStrDef).commit();
                    }
                } catch (JSONException e) {
                    JSONFehler = true;
                    fehlerAusgabe = jsonStr + " | " + jsonStrDef;
                }

            }

            String privatChannelURL;
            if (!"".equals(privateChannelString)) {
                //Loop over each private channel
                for (String channelUUID : privateChannels) {
                    channelUUID = channelUUID.trim();
                    privatChannelURL = baseURL + "/entity/" + channelUUID + ".json";
                    Log.d("Preferences", "privatChannelURL: " + privatChannelURL);
                    if (uname.equals("")) {
                        jsonprivateStr = sh.makeServiceCall(privatChannelURL, ServiceHandler.GET);
                    } else {
                        jsonprivateStr = sh.makeServiceCall(privatChannelURL, ServiceHandler.GET, null, uname, pwd);
                    }
                    if (jsonprivateStr.startsWith("Error: ")) {
                        JSONFehler = true;
                        fehlerAusgabe = jsonprivateStr;
                    } else {
                        try {
                            JSONObject jsonprivateStrObj = new JSONObject(jsonprivateStr);
                            if (jsonprivateStrObj.has("entity") && !jsonprivateStrObj.getString("entity").equals("[]")) {
                                newChannels = true;
                                Log.d("Preferences", "jsonprivateStr: " + jsonprivateStr);
                                JSONObject privateChannelEntity = jsonprivateStrObj.getJSONObject("entity");
                                JSONArray entitiesArray = jsonStrObj.getJSONArray("entities");
                                entitiesArray.put(privateChannelEntity);
                                jsonStrObj.put("entities", entitiesArray);
                            }
                        } catch (JSONException e) {
                            JSONFehler = true;
                            fehlerAusgabe = jsonprivateStr;
                        }
                    }
                }
            }
            // store all channel stuff in a shared preference
            if (jsonStrObj != null) {
                if(sharedPref.getString("sortChannelMode", "off").equals("off")) {
                    jsonStr = jsonStrObj.toString();
                }
                else if (sharedPref.getString("sortChannelMode", "off").equals("groups")) {
                    jsonStr = Tools.sortJSONChannels(jsonStrObj, Tools.TAG_ENTITIES, "groups").toString().replace("\\", "").replace("\"[", "[").replace("]\"","]"); //not sure why the quotes are escaped after put, so remove escaped quotes a.s.o.
                }
                else if (sharedPref.getString("sortChannelMode", "off").equals("plain")) {
                    jsonStr = Tools.sortJSONChannels(jsonStrObj, Tools.TAG_ENTITIES, "plain").toString().replace("\\", "").replace("\"[", "[").replace("]\"","]"); //not sure why the quotes are escaped after put, so remove escaped quotes a.s.o.
                }
                getApplicationContext().getSharedPreferences(Tools.JSON_CHANNEL_PREFS, Activity.MODE_PRIVATE).edit().putString(Tools.JSON_CHANNELS, jsonStr).commit();
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
                addPreferenceChannels(allCheckedChannels);
            }
        }
    }
}
