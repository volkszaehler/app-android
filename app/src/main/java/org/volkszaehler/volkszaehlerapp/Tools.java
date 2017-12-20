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
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

class Tools {

    static final String TAG_UUID = "uuid";
    static final String TAG_TYPE = "type";
    static final String TAG_COLOR = "color";
    static final String TAG_COST = "cost";
    static final String TAG_TITLE = "title";
    static final String TAG_DESCRIPTION = "description";
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
    static final String JSON_CHANNELS = "JSONChannels";
    static final String JSON_CHANNEL_PREFS = "JSONChannelPrefs";
    static final String JSON_DEFINITIONS = "JSONDefinitions";
    static final DecimalFormat f = new DecimalFormat("#0");
    static final DecimalFormat f0 = new DecimalFormat("#0.0");
    static final DecimalFormat f00 = new DecimalFormat("#0.00");
    static final DecimalFormat f000 = new DecimalFormat("#0.000");
    static final String TAG_ENTITIES = "entities";
    static final String AllCheckedChannels = "allCheckedChannels";
    static final String TAG_UNIT = "unit";
    static final String TAG_SCALE = "scale";
    private static final String TAG_ACTIVE = "active";
    private static final String TAG_FILLSTYLE = "fillstyle";
    private static final String TAG_PUBLIC = "public";
    private static final String TAG_STYLE = "style";
    private static final String TAG_YAXIS = "yaxis";
    private static final String TAG_CHILDREN = "children";
    private static final String BACKUP_FILENAME = "volkszaehler_settings_backup.txt";
    private static final Object TAG_GROUP = "group";
    private static String definitionValue = "";

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(JSON_CHANNEL_PREFS, Activity.MODE_PRIVATE);
    }

    static String getDefinitionValue(Context context, String type, String uuid, String definitionName) {

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
                            definitionValue = "null".equals(entity.getString(definitionName)) ? "" : entity.getString(definitionName);
                            break;
                        }

                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        } else if (uuid != null && !"".equals(uuid)) {
            String rType = getPropertyOfChannel(context, uuid, TAG_TYPE);
            getDefinitionValue(context, rType, null, definitionName);
        }

        return definitionValue;

    }

    static String getHashMapBasedPropertyOfChannel(Context context, String uuid, String property) {
        SharedPreferences prefs = getPrefs(context);
        String sJSONChannels = prefs.getString(JSON_CHANNELS, "");
        for (HashMap<String, String> channelMap : getChannelsFromJSONStringEntities(sJSONChannels, context)) {

            if (uuid.equals(channelMap.get(TAG_UUID))) {
                return channelMap.containsKey(property) ? channelMap.get(property) : "";
            }
        }
        return "";
    }
    static String getPropertyOfChannel(Context context, String uuid, String property) {
        try {
            JSONObject JSON_Channels = new JSONObject(getPrefs(context).getString(JSON_CHANNELS,""));
            JSONArray channels = JSON_Channels.getJSONArray(TAG_ENTITIES);
            boolean isDrin;
            // looping through All channels
            for (int i = 0; i < channels.length(); i++) {
                JSONObject channel = channels.getJSONObject(i);
                if (uuid.equals(channel.get(TAG_UUID))) {
                    return channel.has(property) ? channel.get(property).toString() : "";
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
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

    static double getMillisValueFromDisplayPoint(Context context, float xValue, double startX, double endX) {
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

    static ArrayList<HashMap<String, String>> getChannelsFromJSONStringEntities(String jSONStringEntities, Context myContext) {
        ArrayList<HashMap<String, String>> channelMapList = new ArrayList<>();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(myContext);
        String sortMode = prefs.getString("sortChannelMode", "off");

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
        if(sortMode.equals("plain")) {
            Collections.sort(channelMapList, new MyHashMapComparator());
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

    private static void getGroupChannels(JSONObject channel, HashMap<String, String> channelMap, ArrayList<HashMap<String, String>> channelMapList) {

        try {
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
        catch (JSONException jex)
        {
            Log.e("getGroupChannels:", String.format("no children found for UUID: %s", channel));
            jex.printStackTrace();
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

    static String getDataOfChannel(Context context, String uuid, String jSONString, String property) {
        String Wert = "";

        try {
            JSONObject jsonObj = new JSONObject(jSONString);
            JSONObject werte = jsonObj.getJSONObject(TAG_DATA);
            //for (int i = 0; i < werte.length(); i++) {
                //jSONObj = werte.getJSONObject(i);
                if (werte.get(TAG_UUID).equals(uuid)) {
                    try {
                        switch (property) {
                            case TAG_MIN:
                                Wert = werte.has(TAG_MIN) ? (werte.getJSONArray(TAG_MIN)).toString() : "";
                                break;
                            case TAG_MAX:
                                Wert = werte.has(TAG_MAX) ? (werte.getJSONArray(TAG_MAX)).toString() : "";
                                break;
                            case TAG_LAST:
                                if (werte.has(TAG_TUPLES)) {
                                    JSONArray jArray = (JSONArray) werte.getJSONArray(TAG_TUPLES).get(werte.getJSONArray(TAG_TUPLES).length() - 1);
                                    Wert = jArray.toString();
                                }
                                break;
                            default:
                                Wert = werte.has(property) ? werte.getString(property) : "";
                                break;
                        }
                    } catch (Exception e) {
                        Log.d("Exception", e.getMessage());
                    }

                    //break;
                }
            //}

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return Wert;
    }

    static boolean saveFile(Context context) {
        //external storage availability check
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            return false;
        }
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), BACKUP_FILENAME);

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
            fw.write("privateChannelUUIDs" + "=" + sharedPrefs.getString("privateChannelUUIDs", "") + "\n");
            fw.write("ZeroBasedYAxis" + "=" + (sharedPrefs.getBoolean("ZeroBasedYAxis", false) ? "true" : "false") + "\n");
            fw.write("autoReload" + "=" + (sharedPrefs.getBoolean("autoReload", false) ? "true" : "false") + "\n");
            fw.write("allCheckedChannels" + "=" + sharedPrefs.getString(AllCheckedChannels, "") + "\n");
            fw.write("sortChannelMode" + "=" + sharedPrefs.getString("sortChannelMode", "") + "\n");
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
            if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                return false;
            }
            File file = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS), BACKUP_FILENAME);

            //fallback to old file location
            if(!file.exists())
            {
                File sdcard = Environment.getExternalStorageDirectory();
                file = new File(sdcard, BACKUP_FILENAME);
            }

            br = new BufferedReader(new FileReader(file));
            String line;
            String allCheckedChannels="";
            while ((line = br.readLine()) != null) {
                if (line.startsWith(JSON_CHANNELS)) {
                    try {
                        line = line.split("=")[1];
                    } catch (IndexOutOfBoundsException iobx) {
                        continue;
                    }
                    context.getSharedPreferences(Tools.JSON_CHANNEL_PREFS, Activity.MODE_PRIVATE).edit().putString(Tools.JSON_CHANNELS, line).apply();
                } else if (line.startsWith(JSON_DEFINITIONS)) {
                    try {
                        line = line.split("=")[1];
                    } catch (IndexOutOfBoundsException iobx) {
                        continue;
                    }
                    context.getSharedPreferences(Tools.JSON_CHANNEL_PREFS, Activity.MODE_PRIVATE).edit().putString(Tools.JSON_DEFINITIONS, line).apply();
                } else if (line.startsWith("volkszaehlerURL")) {
                    try {
                        line = line.split("=")[1];
                    } catch (IndexOutOfBoundsException iobx) {
                        continue;
                    }
                    PreferenceManager.getDefaultSharedPreferences(context).edit().putString("volkszaehlerURL", line).apply();
                } else if (line.startsWith("Tuples")) {
                    try {
                        line = line.split("=")[1];
                    } catch (IndexOutOfBoundsException iobx) {
                        continue;
                    }
                    PreferenceManager.getDefaultSharedPreferences(context).edit().putString("Tuples", line).apply();
                } else if (line.startsWith("privateChannelUUIDs")) {
                    try {
                        line = line.split("=")[1];
                    } catch (IndexOutOfBoundsException iobx) {
                        continue;
                    }
                    PreferenceManager.getDefaultSharedPreferences(context).edit().putString("privateChannelUUIDs", line).apply();
                } else if (line.startsWith(AllCheckedChannels)) {
                    try {
                        line = line.split("=")[1];
                        allCheckedChannels = line;
                    } catch (IndexOutOfBoundsException iobx) {
                        continue;
                    }
                    PreferenceManager.getDefaultSharedPreferences(context).edit().putString(AllCheckedChannels, line).apply();
                } else if (line.startsWith("ZeroBasedYAxis")) {
                    try {
                        line = line.split("=")[1];
                    } catch (IndexOutOfBoundsException iobx) {
                        continue;
                    }
                    PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("ZeroBasedYAxis", Boolean.parseBoolean(line)).apply();
                } else if (line.startsWith("sortChannelMode")) {
                    try {
                        line = line.split("=")[1];
                    } catch (IndexOutOfBoundsException iobx) {
                        continue;
                    }
                    PreferenceManager.getDefaultSharedPreferences(context).edit().putString("sortChannelMode", line).apply();
                } else if (line.startsWith("autoReload")) {
                    try {
                        line = line.split("=")[1];
                    } catch (IndexOutOfBoundsException iobx) {
                        continue;
                    }
                    PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean("autoReload", Boolean.parseBoolean(line)).apply();
                }
            }
            //set checkboxes
            checkChannels(allCheckedChannels,context);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;

        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    static String getCheckedChannels(Context myContext, boolean bSorted)
    {
        List<String> channelList = new ArrayList<String>();
        for (String preference : PreferenceManager.getDefaultSharedPreferences(myContext).getAll().keySet()) {
            // assume its a UUID of a channel
            if (preference.contains("-") && preference.length() == 36) {
                // is preference checked?
                if (PreferenceManager.getDefaultSharedPreferences(myContext).getBoolean(preference, false)) {
                    channelList.add(preference);
                }
            }
        }
        //always sort by Title (for Chart Popup)
        if(bSorted) {
            Collections.sort(channelList,new UUIDbyTitleComparator(myContext));
        }
        return android.text.TextUtils.join(",", channelList);
    }

    private static void checkChannels(String allCheckedChannels, Context myContext)
    {
        for(String checkedChannel : allCheckedChannels.split(",")) {
            PreferenceManager.getDefaultSharedPreferences(myContext).edit().putBoolean(checkedChannel, true).apply();
        }
    }

    static Boolean showAboutDialog(Context context) {
        String app_ver;
        try {
            app_ver = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("ChannelDetails", "strange VersionName");
            return false;
        }
        AlertDialog d = new AlertDialog.Builder(context).setTitle(context.getString(R.string.app_name) + ", Version " + app_ver).setMessage(R.string.aboutLinks).setNeutralButton(context.getString(R.string.Close), null).show();
        ((TextView) d.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
        return true;
    }

    static String getDateTimeString(long localfrom, long localto, String range, Context myContext) {
        String dateTimeString="";
        DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(myContext);
        DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(myContext);

        switch (range) {
            case "day":
                dateTimeString = timeFormat.format(localfrom) + " - " + timeFormat.format(localto);
                break;
            case "7days":
                dateTimeString = dateFormat.format(localfrom) + " - " + dateFormat.format(localto);
                break;
            case "week":
                dateTimeString = dateFormat.format(localfrom) + " - " + dateFormat.format(localto);
                break;
            case "month":
                dateTimeString = dateFormat.format(localfrom) + " - " + dateFormat.format(localto);
                break;
            case "year":
                dateTimeString = dateFormat.format(localfrom) + " - " + dateFormat.format(localto);
                break;
            default:
                Log.e("TableDetails", "Unknown 'Range': " + range);
        }

        return dateTimeString;
    }

    static String determineColor(float anzahlStufen, float stufe)
    {
        float gruenTeil;
        float rotTeil;
        String color;

        if (stufe > anzahlStufen/4) {
            gruenTeil = 255 - (255 * ((stufe - anzahlStufen / 4)/(anzahlStufen * 3 / 4)));
            String s = Integer.toHexString((int) gruenTeil);
            s = s.length() < 2 ? s = "0" + s : s;
            color = "#ff" + s + "00";
        }
        else {
            rotTeil = 255 * (stufe * 4  / anzahlStufen);
            String s = Integer.toHexString((int) rotTeil);
            s = s.length() < 2 ? s = "0" + s : s;
            color = "#" + s + "ff00";
        }
        if(color.length() != 7)
        {
            color = color + color;
        }
        return color;
    }

    public static float determineStufe(float min, float max, float stufen, String value)
    {
        float stufengroesse = (max - min)/stufen;
        float stufe = (Float.parseFloat(value) - min) / stufengroesse;
        return Math.round(stufe);
    }

    static JSONObject sortJSONChannels(JSONObject JSONChannels, String sortWhat, String sortMode)
    {
        ArrayList<JSONObject> list = null;
        JSONArray what_Array = null;
        JSONArray group_Array = null;
        try {
            what_Array = (JSONArray) JSONChannels.get(sortWhat);
            list = new ArrayList<>();
            for (int i = 0; i < what_Array.length(); i++) {
                JSONObject channel = (JSONObject) what_Array.get(i);
                if(TAG_GROUP.equals(channel.get(TAG_TYPE)))
                {
                    if(channel.get(TAG_CHILDREN)!="" && channel.get(TAG_CHILDREN) != null) {
                        if(sortMode.equals("groups")) {
                            channel = sortJSONChannels(channel, TAG_CHILDREN, "groups");
                        }
                    }
                }
                list.add(channel);
            }
            Collections.sort(list, new MyJSONComparator());
            JSONChannels.remove(sortWhat);
            JSONChannels.put(sortWhat, list);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return JSONChannels;
    }
}

class  MyJSONComparator implements Comparator<JSONObject> {
    @Override
    public int compare(JSONObject jo1, JSONObject jo2) {
        String v1 = "", v2 = "";
        try {
            v1 = (String) jo1.get(Tools.TAG_TITLE);
            v2 = (String) jo2.get(Tools.TAG_TITLE);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return v1.compareTo(v2);
    }
}

class  MyHashMapComparator implements Comparator<HashMap> {
    @Override
    public int compare(HashMap channel1, HashMap channel2) {
        String v1 = (String) channel1.get(Tools.TAG_TITLE);
        String v2 = (String) channel2.get(Tools.TAG_TITLE);
        return v1.compareTo(v2);
    }
}

class  UUIDbyTitleComparator implements Comparator<String> {
    private Context context;
    public UUIDbyTitleComparator(Context myContext) {
        context = myContext;
    }
    @Override
    public int compare(String channel1, String channel2) {
        String v1 = Tools.getPropertyOfChannel(context,channel1,Tools.TAG_TITLE);
        String v2 = Tools.getPropertyOfChannel(context,channel2,Tools.TAG_TITLE);
        return v1.compareTo(v2);
    }
}

class  MyCharSequenceComparator implements Comparator<CharSequence> {
    @Override
    public int compare(CharSequence charSequence1, CharSequence charSequence2) {
        return charSequence1.toString().compareTo(charSequence2.toString());
    }
}
