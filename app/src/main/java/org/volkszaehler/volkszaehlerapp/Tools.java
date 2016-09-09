package org.volkszaehler.volkszaehlerapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.widget.TextView;

import org.achartengine.model.TimeSeries;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

class Tools {

    private static final String TAG_ENTITIES = "entities";
    static final String TAG_UUID = "uuid";
    static final String TAG_TYPE = "type";
    private static final String TAG_ACTIVE = "active";
    static final String TAG_COLOR = "color";
    static final String TAG_COST = "cost";
    private static final String TAG_FILLSTYLE = "fillstyle";
    private static final String TAG_PUBLIC = "public";
    private static final String TAG_STYLE = "style";
    static final String TAG_TITLE = "title";
    private static final String TAG_YAXIS = "yaxis";
    static final String TAG_DESCRIPTION = "description";
    private static final String TAG_CHILDREN = "children";
    static final String TAG_CHUILDUUIDS = "childUUIDs";
    static final String TAG_DATA = "data";
    static final String TAG_TUPLES = "tuples";
    static final String TAG_BELONGSTOGROUP = "belongsToGroup";
    static final String TAG_MIN = "min";
    static final String TAG_MAX = "max";
    static final String TAG_LAST = "letzter";
    static final String TAG_AVERAGE = "average";
    static final String TAG_CONSUMPTION = "consumption";
    static final String TAG_ROWS = "rows";
    static final String TAG_FROM = "from";
    static final String TAG_TO = "to";
    static final String TAG_RESOLUTION = "resolution";
    static final String TAG_INITIALCONSUMPTION = "initialconsumption";
    private static final String BACKUP_FILENAME = "volkszaehler_settings_backup.txt";
    static final String JSON_CHANNELS = "JSONChannels";
    static final String JSON_CHANNEL_PREFS = "JSONChannelPrefs";
    static final String JSON_DEFINITIONS = "JSONDefinitions";

    private static String unit = "";

    static final DecimalFormat f = new DecimalFormat("#0");
    static final DecimalFormat f0 = new DecimalFormat("#0.0");
    static final DecimalFormat f00 = new DecimalFormat("#0.00");
    static final DecimalFormat f000 = new DecimalFormat("#0.000");

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(JSON_CHANNEL_PREFS, Activity.MODE_PRIVATE);
    }

    public static String getUnit(Context context, String type, String uuid) {

        SharedPreferences prefs = getPrefs(context);
        if (type != null && !"".equals(type)) {

            String sJSONDefinitions = prefs.getString(JSON_DEFINITIONS, "");

            if (!"".equals(sJSONDefinitions)) {
                JSONObject jsonObj;
                try {
                    jsonObj = new JSONObject(sJSONDefinitions);
                    JSONObject capabilities = jsonObj.getJSONObject("capabilities");
                    JSONObject definitions = capabilities.getJSONObject("definitions");// new
                    // JSONObject("definitions");
                    JSONArray entities = definitions.getJSONArray("entities");
                    for (int i = 0; i < entities.length(); i++) {
                        JSONObject entity = (JSONObject) entities.get(i);
                        if (entity.has("name") && entity.getString("name").equals(type)) {
                            unit = "null".equals(entity.getString("unit")) ? "" : entity.getString("unit");
                            break;
                        }

                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        } else if (uuid != null && !"".equals(uuid)) {
            String rType = getPropertyOfChannel(context, uuid, "type");
            getUnit(context, rType, null);
        }

        return unit;

    }

    public static String getPropertyOfChannel(Context context, String uuid, String property) {
        SharedPreferences prefs = getPrefs(context);
        String sJSONChannels = prefs.getString(JSON_CHANNELS, "");
        for (HashMap<String, String> channelMap : getChannelsFromJSONStringEntities(sJSONChannels)) {

            if (uuid.equals(channelMap.get(TAG_UUID))) {
                return channelMap.containsKey(property) ? channelMap.get(property) : "";
            }
        }
        return "";
    }

    // changes normal charts into Step charts by adding points
    private static JSONArray createStepfromLine(JSONArray jsonArray) {
        JSONArray newJSONArray = new JSONArray();
        JSONArray tupleWert;
        JSONArray nextTupleWert;

        try {
            for (int j = 0; j < jsonArray.length(); j++) {

                tupleWert = jsonArray.getJSONArray(j);

                newJSONArray.put(tupleWert);

                // stop when Array end is reached
                if (j + 1 >= jsonArray.length()) {
                    break;
                }
                // get next value pair
                nextTupleWert = jsonArray.getJSONArray(j + 1);

                // create new value pair for the steps
                JSONArray newTuplePaar = new JSONArray();

                // set the time 1 ms after the last 'real' value pair
                newTuplePaar.put(0, tupleWert.getLong(0) + 1);
                // set the value to the value of the next value pair
                newTuplePaar.put(1, nextTupleWert.getDouble(1));
                // and add
                newJSONArray.put(newTuplePaar);
            }
        } catch (JSONException e) {
            Log.e("Tools.createStepfLine", "JSONException " + e.getMessage());
        } catch (IndexOutOfBoundsException iobe) {
            // eins zuviel
        }

        return newJSONArray;
    }

    public static double getMillisValueFromDisplayPoint(Context context, float xValue, double startX, double endX) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;

        double timeRange = endX - startX;
        // 30 is the margin of the graph
        int pointRange = width - 30;
        // each point has ... ms
        double ratio = timeRange / pointRange;
        // gives the new start ms, 20 is the left margin

        return Math.round((ratio * (xValue - 20))) + startX;
    }

    public static ArrayList<HashMap<String, String>> getChannelsFromJSONStringEntities(String jSONStringEntities) {
        ArrayList<HashMap<String, String>> channelMapList = new ArrayList<>();

        try {
            JSONObject jsonObj = new JSONObject(jSONStringEntities);

            // Getting JSON Array node
            JSONArray channels = jsonObj.getJSONArray(TAG_ENTITIES);
            boolean isDrin;
            // looping through All channels
            for (int i = 0; i < channels.length(); i++) {
                JSONObject channel = channels.getJSONObject(i);

                HashMap<String, String> channelMap = getChannelValues(channel, false);
                if (channelMapList.isEmpty()) {
                    channelMapList.add(channelMap);
                } else {
                    isDrin = false;
                    for (HashMap<String, String> existingChannel : channelMapList) {
                        if (existingChannel.get(TAG_UUID).equals(channelMap.get(TAG_UUID))) {
                            isDrin = true;
                            break;
                        }
                    }
                    if (!isDrin) {
                        channelMapList.add(channelMap);
                    }
                }

                if (channelMap.get("type").equals("group")) {
                    getGroupChannels(channel, channelMap, channelMapList);
                } else {
                    channelMap.put(TAG_CHUILDUUIDS, "");
                    channelMap.put("belongsToGroup", "");
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return channelMapList;
    }

    private static HashMap<String, String> getChannelValues(JSONObject c, boolean inGroup) throws JSONException {
        String id = c.has(TAG_UUID) ? c.getString(TAG_UUID) : "";
        String title = c.has(TAG_TITLE) ? c.getString(TAG_TITLE) : "";
        String type = c.has(TAG_TYPE) ? c.getString(TAG_TYPE) : "";
        String color = c.has(TAG_COLOR) ? c.getString(TAG_COLOR) : "";
        String cost = c.has(TAG_COST) ? c.getString(TAG_COST) : "";
        String description = c.has(TAG_DESCRIPTION) ? c.getString(TAG_DESCRIPTION) : "";
        String yaxis = c.has(TAG_YAXIS) ? c.getString(TAG_YAXIS) : "";
        String style = c.has(TAG_STYLE) ? c.getString(TAG_STYLE) : "";
        String active = c.has(TAG_ACTIVE) ? c.getString(TAG_ACTIVE) : "";
        String ppublic = c.has(TAG_PUBLIC) ? c.getString(TAG_PUBLIC) : "";
        String fillstyle = c.has(TAG_FILLSTYLE) ? c.getString(TAG_FILLSTYLE) : "";
        String resolution = c.has(TAG_RESOLUTION) ? c.getString(TAG_RESOLUTION) : "";
        String initialconsumption = c.has(TAG_INITIALCONSUMPTION) ? c.getString(TAG_INITIALCONSUMPTION) : "";

        // tmp hashmap for single channel
        HashMap<String, String> channel = new HashMap<>();

        // adding each child node to HashMap key => value
        channel.put(TAG_TITLE, title);
        channel.put(TAG_UUID, id);
        channel.put(TAG_TYPE, type);
        channel.put(TAG_COLOR, color);
        channel.put(TAG_COST, cost);
        channel.put(TAG_DESCRIPTION, description);
        channel.put(TAG_YAXIS, yaxis);
        channel.put(TAG_STYLE, style);
        channel.put(TAG_ACTIVE, active);
        channel.put(TAG_PUBLIC, ppublic);
        channel.put(TAG_FILLSTYLE, fillstyle);
        channel.put(TAG_INITIALCONSUMPTION, initialconsumption);
        channel.put(TAG_RESOLUTION, resolution);
        return channel;
    }

    private static void getGroupChannels(JSONObject channel, HashMap<String, String> channelMap, ArrayList<HashMap<String, String>> channelMapList) throws JSONException {

        JSONArray children = channel.getJSONArray(TAG_CHILDREN);
        boolean isDrin;
        HashMap<String, String> existingChannel = new HashMap<>();
        for (int j = 0; j < children.length(); j++) {
            JSONObject childs = children.getJSONObject(j);
            HashMap<String, String> child = getChannelValues(childs, true);
            child.put("belongsToGroup", channelMap.get(TAG_UUID));
            String childUUIDs;
            // every group should know it's childs
            if (channelMap.containsKey(TAG_CHUILDUUIDS)) {
                childUUIDs = channelMap.get(TAG_CHUILDUUIDS) + "|" + child.get(TAG_UUID);
                channelMap.put(TAG_CHUILDUUIDS, childUUIDs);
            } else {
                channelMap.put(TAG_CHUILDUUIDS, child.get(TAG_UUID));
            }
            isDrin = false;
            for (HashMap<String, String> localExistingChannel : channelMapList) {
                if (localExistingChannel.get(TAG_UUID).equals(child.get(TAG_UUID))) {
                    existingChannel = localExistingChannel;
                    isDrin = true;
                }
            }
            if (isDrin) {
                channelMapList.remove(existingChannel);
            }
            channelMapList.add(child);
            // entities.json supports one level of groups only, so one level of
            // groups for now...
            // if(child.get("type").equals("group"))
            // {
            // getGroupChannels(childs, child);
            // }
        }

    }

    static TimeSeries getTimeSeries(Context context, String sUUID) {
        SharedPreferences prefs = getPrefs(context);
        String sJSONChannelsData = prefs.getString("JSONChannelsData", "");
        TimeSeries mCurrentSeries = new TimeSeries(getPropertyOfChannel(context, sUUID, TAG_TITLE));

        try {
            JSONObject jsonObj = new JSONObject(sJSONChannelsData);
            JSONArray werte = jsonObj.getJSONArray(TAG_DATA);
            JSONObject jSONObj;
            for (int i = 0; i < werte.length(); i++) {
                jSONObj = werte.getJSONObject(i);
                if (jSONObj.get(TAG_UUID).equals(sUUID)) {
                    if (jSONObj.has(TAG_TUPLES)) {
                        JSONArray tuples = jSONObj.getJSONArray(TAG_TUPLES);
                        if ("steps".equals(Tools.getPropertyOfChannel(context, sUUID, TAG_STYLE))) {
                            tuples = Tools.createStepfromLine(tuples);
                        }
                        for (int j = 0; j < tuples.length(); j++) {
                            JSONArray tupleWert = tuples.getJSONArray(j);
                            mCurrentSeries.add(new Date(tupleWert.getLong(0)), tupleWert.getDouble(1));
                        }
                    }
                    // UUID found
                    break;
                }

            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return mCurrentSeries;
    }

    static String getDataOfChannel(Context context, String uuid, String property) {
        SharedPreferences prefs = getPrefs(context);
        String sJSONChannelsData = prefs.getString("JSONChannelsData", "");
        Log.d("sJSONChannelsData: ", sJSONChannelsData);
        String Wert = "";

        try {
            JSONObject jSONObj;
            JSONObject jsonObj = new JSONObject(sJSONChannelsData);
            JSONArray werte = jsonObj.getJSONArray(TAG_DATA);
            for (int i = 0; i < werte.length(); i++) {
                jSONObj = werte.getJSONObject(i);
                if (jSONObj.get(TAG_UUID).equals(uuid)) {
                    try {
                        switch (property) {
                            case TAG_MIN:
                                Wert = jSONObj.has(TAG_MIN) ? (jSONObj.getJSONArray(TAG_MIN)).toString() : "";
                                break;
                            case TAG_MAX:
                                Wert = jSONObj.has(TAG_MAX) ? (jSONObj.getJSONArray(TAG_MAX)).toString() : "";
                                break;
                            case TAG_LAST:
                                if (jSONObj.has(TAG_TUPLES)) {
                                    JSONArray jArray = (JSONArray) jSONObj.getJSONArray(TAG_TUPLES).get(jSONObj.getJSONArray(TAG_TUPLES).length() - 1);
                                    Wert = jArray.toString();
                                }
                                break;
                            default:
                                Wert = jSONObj.has(property) ? jSONObj.getString(property) : "";
                                break;
                        }
                    } catch (Exception e) {
                        Log.d("Exception", e.getMessage());
                    }

                    break;
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return Wert;
    }

    static boolean saveFile(Context context) {
        File sdcard = Environment.getExternalStorageDirectory();
        File file = new File(sdcard, BACKUP_FILENAME);

        FileWriter fw;
        try {
            fw = new FileWriter(file, false);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
            //try {
            SharedPreferences prefs = getPrefs(context);
            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

            try {
                fw.write(JSON_CHANNELS + "=" + prefs.getString(Tools.JSON_CHANNELS, "") + "\n");
                fw.write(JSON_DEFINITIONS + "=" + prefs.getString(JSON_DEFINITIONS, "") + "\n");
                fw.write("volkszaehlerURL" + "=" + sharedPrefs.getString("volkszaehlerURL", "") + "\n");
                fw.write("Tuples" + "=" + sharedPrefs.getString("Tuples", "1000") + "\n");
                fw.write("ZeroBasedYAxis" + "=" + (sharedPrefs.getBoolean("ZeroBasedYAxis", false) ? "true" : "false") + "\n");
                fw.write("autoReload" + "=" + (sharedPrefs.getBoolean("autoReload", false) ? "true" : "false"));
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            } finally {
                try {
                    fw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        return true;
    }

     static boolean loadFile(Context context) {

         BufferedReader br = null;
         try {
             File sdcard = Environment.getExternalStorageDirectory();
             File file = new File(sdcard,BACKUP_FILENAME);

             br = new BufferedReader(new FileReader(file));
             String line;
             while ((line = br.readLine()) != null) {
                 if (line.startsWith(JSON_CHANNELS)) {
                     try {
                         line = line.split("=")[1];
                     }
                     catch (IndexOutOfBoundsException iobx)
                     {
                         continue;
                     }
                     context.getSharedPreferences(Tools.JSON_CHANNEL_PREFS, Activity.MODE_PRIVATE).edit().putString(Tools.JSON_CHANNELS, line).commit();
                 } else if (line.startsWith(JSON_DEFINITIONS)) {
                     try {
                         line = line.split("=")[1];
                     }
                     catch (IndexOutOfBoundsException iobx)
                     {
                         continue;
                     }
                     context.getSharedPreferences(Tools.JSON_CHANNEL_PREFS, Activity.MODE_PRIVATE).edit().putString(Tools.JSON_DEFINITIONS, line).commit();
                 } else if (line.startsWith("volkszaehlerURL")) {
                     try {
                         line = line.split("=")[1];
                     }
                     catch (IndexOutOfBoundsException iobx)
                     {
                         continue;
                     }
                     PreferenceManager.getDefaultSharedPreferences(context).edit().putString("volkszaehlerURL", line).commit();
                 } else if (line.startsWith("Tuples")) {
                     try {
                         line = line.split("=")[1];
                     }
                     catch (IndexOutOfBoundsException iobx)
                     {
                         continue;
                     }
                     PreferenceManager.getDefaultSharedPreferences(context).edit().putString("Tuples", line).commit();
                 }else if (line.startsWith("ZeroBasedYAxis")) {
                     try {
                         line = line.split("=")[1];
                     }
                     catch (IndexOutOfBoundsException iobx)
                     {
                         continue;
                     }
                     PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("ZeroBasedYAxis", Boolean.parseBoolean(line)).commit();
                 }
                 else if (line.startsWith("autoReload")) {
                     try {
                         line = line.split("=")[1];
                     }
                     catch (IndexOutOfBoundsException iobx)
                     {
                         continue;
                     }
                     PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("autoReload", Boolean.parseBoolean(line)).commit();
                 }
             }
             return true;
         }
         catch (IOException e) {
             e.printStackTrace();
             return false;

         }
         finally{
             if (br != null)
             {
                 try {
                     br.close();
                 } catch (IOException e) {
                     e.printStackTrace();
                 }
             }
         }
    }
      static Boolean showAboutDialog(Context context)
    {
        String app_ver;
        try {
            app_ver = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("ChannelDetails","strange VersionName");
            return false;
        }
        AlertDialog d = new AlertDialog.Builder(context).setTitle(context.getString(R.string.app_name) + ", Version " + app_ver).setMessage(R.string.aboutLinks).setNeutralButton(context.getString(R.string.Close), null).show();
        ((TextView)d.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
        return true;
    }
}
