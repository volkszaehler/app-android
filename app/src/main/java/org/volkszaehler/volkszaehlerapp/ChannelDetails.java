package org.volkszaehler.volkszaehlerapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

public class ChannelDetails extends CustomMenuActivity {

    private Context myContext;
    private String mUUID = "";
    private ProgressDialog pDialog;
    private String unit;
    private String jsonStrGesamt = "";
    private boolean strom = false;
    private boolean gas = false;
    private boolean water = false;
    private boolean temp = false;
    private boolean group = false;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.details);
        myContext = this;
        // addListenerOnButton();
        Intent i = getIntent();
        mUUID = i.getStringExtra(Tools.TAG_UUID);

        // after OrientationChange
        if (savedInstanceState != null) {
            jsonStrGesamt = savedInstanceState.getString("jsonStrGesamt");
            unit = savedInstanceState.getString("unit");
        }

        String typeOfChannel = Tools.getPropertyOfChannel(myContext, mUUID, Tools.TAG_TYPE);
        switch (typeOfChannel) {
            case "power":
            case "heat": //really for all heat?
            case "powersensor":
                strom = true;
                break;
            case "temperature":
                temp = true;
                break;
            case "gas":
                gas = true;
                break;
            case "water":
                water = true;
                break;
            case "group":
                group = true;
                break;
            default:
                Log.e("ChannelDetails", "Unknown channel type: " + typeOfChannel);
        }

        //empty color, default = blue
        String col = "".equals(Tools.getPropertyOfChannel(myContext, mUUID, "color")) ? "blue" : Tools.getPropertyOfChannel(myContext, mUUID, "color");

        int cColor;
        if (col != null && col.startsWith("#")) {
            cColor = Color.parseColor(col.toUpperCase(Locale.getDefault()));
            ((TextView) findViewById(R.id.textViewTitle)).setTextColor(cColor);
            ((TextView) findViewById(R.id.textViewValue)).setTextColor(cColor);
            findViewById(R.id.editTextChannelDetails).setBackgroundColor(cColor);
        } else {
            try {
                cColor = Color.parseColor(col.toUpperCase(Locale.getDefault()));
                ((TextView) findViewById(R.id.textViewTitle)).setTextColor(cColor);
                ((TextView) findViewById(R.id.textViewValue)).setTextColor(cColor);
                findViewById(R.id.editTextChannelDetails).setBackgroundColor(cColor);
            } catch (IllegalArgumentException e) {
                Log.e("ChannelDetails", "Unknown Color: " + col);
            }
        }
        String myTitel = Tools.getPropertyOfChannel(myContext, mUUID, Tools.TAG_TITLE);
        ((TextView) findViewById(R.id.textViewTitle)).setText(myTitel);
        ((TextView) findViewById(R.id.textViewValue)).setText(i.getStringExtra("tuplesWert"));
        try {
            String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date(Long.valueOf(i.getStringExtra("tuplesZeit"))));
            ((TextView) findViewById(R.id.textViewDateValue)).setText(currentDateTimeString);
        } catch (NumberFormatException nfe) {
            Log.e("ChannelDetails", "strange Tuple Time: " + i.getStringExtra("tuplesZeit"));
        }

        ((TextView) findViewById(R.id.textViewDescription)).setText(Tools.getPropertyOfChannel(myContext, mUUID, Tools.TAG_DESCRIPTION));
        try {
            String sCost = Tools.getPropertyOfChannel(myContext, mUUID, Tools.TAG_COST);
            if (!"".equals(sCost)) {
                double dCost = Double.valueOf(sCost);
                if (strom || gas) {
                    ((TextView) findViewById(R.id.textViewCost)).setText(Tools.f0.format(dCost * 100) + Units.CENT);
                } else if (water) {
                    double sResolution = Double.valueOf(Tools.getPropertyOfChannel(myContext, mUUID, Tools.TAG_RESOLUTION));
                    ((TextView) findViewById(R.id.textViewCost)).setText(Tools.f00.format(dCost * sResolution * 1000) + Units.EUROpermmm);
                }
            } else {
                // no cost
                findViewById(R.id.textViewTitleCost).setVisibility(View.GONE);
                findViewById(R.id.textViewCost).setVisibility(View.GONE);
            }
        } catch (NumberFormatException nfe) {
            Log.e("ChannelDetails", "strange costs: " + Tools.getPropertyOfChannel(myContext, mUUID, Tools.TAG_COST));
        }

        ((TextView) findViewById(R.id.textViewUUID)).setText(mUUID);
        ((TextView) findViewById(R.id.textViewType)).setText(Tools.getPropertyOfChannel(myContext, mUUID, Tools.TAG_TYPE));
        String childUUIDs = Tools.getPropertyOfChannel(myContext, mUUID, Tools.TAG_CHUILDUUIDS);
        String childrenNames = "";
        if (null != childUUIDs && !"".equals(childUUIDs)) {
            if (childUUIDs.contains("|")) {
                String[] children = (childUUIDs.split("\\|"));

                for (String child : children) {
                    childrenNames = childrenNames + "\n" + Tools.getPropertyOfChannel(myContext, child, Tools.TAG_TITLE);
                }
            } else {
                childrenNames = Tools.getPropertyOfChannel(myContext, childUUIDs, Tools.TAG_TITLE);
            }
            ((TextView) findViewById(R.id.textViewChildren)).setText(childrenNames);
        } else {
            ((TextView) findViewById(R.id.textViewTitleChildren)).setText("");
        }

        String initialConsumption = Tools.getPropertyOfChannel(myContext, mUUID, Tools.TAG_INITIALCONSUMPTION);
        if ("".equals(jsonStrGesamt) && !"".equals(initialConsumption)) {
            unit = Tools.getDefinitionValue(myContext, typeOfChannel, mUUID, Tools.TAG_UNIT);
            if ("gas".equals(typeOfChannel)) {
                unit = unit.substring(0, 2);
            } else if ("water".equals(typeOfChannel)) {
                unit = unit.substring(0, 1);
            } else {
                unit = "k" + unit + "h";
            }
            new GetTotalConsumtion().execute(initialConsumption);
        } else if (!"".equals(jsonStrGesamt)) {
            ((TextView) findViewById(R.id.textViewGesamt)).setText(jsonStrGesamt + " " + unit);
        } else {
            //remove consumption from dialog
            findViewById(R.id.textViewTitleGesamt).setVisibility(View.GONE);
            findViewById(R.id.textViewGesamt).setVisibility(View.GONE);
        }

        //remove table for type Group
        if(group)
        {
            findViewById(R.id.linearLayout620).setVisibility(View.GONE);
        }

    }

    public void chartsDetailsHandler(View view) {
        switch (view.getId()) {
            case R.id.buttonViewChartHour:
                callChart("hour");
                break;
            case R.id.buttonViewChartDay:
                callChart("day");
                break;
            case R.id.buttonViewChartWeek:
                callChart("week");
                break;
            case R.id.buttonViewChartMonth:
                callChart("month");
                break;
            default:
                callChart("day");
                break;
        }
    }

    public void tableDetailsHandler(View view) {
        String range;
        switch (view.getId()) {
            case R.id.buttonViewTableDay:
                range = "day";
                break;
            case R.id.buttonViewTableWeek:
                range = "week";
                break;
            case R.id.buttonViewTable7Days:
                range = "7days";
                break;
            case R.id.buttonViewTableMonth:
                range = "month";
                break;
            case R.id.buttonViewTableYear:
                range = "year";
                break;
            case R.id.buttonViewTableCustom:
                range = "custom";
                break;
            default:
                range = "day";
                break;
        }
        if("custom".equals(range)) {
            Intent dateSelector = new Intent(ChannelDetails.this, DateSelector.class);
            dateSelector.putExtra("From", (long) System.currentTimeMillis() - 86400000);
            dateSelector.putExtra("To", (long) System.currentTimeMillis());
            dateSelector.putExtra("MUUID", mUUID);
            startActivity(dateSelector);
        }
        else {
            Intent detailTableIntent = new Intent(ChannelDetails.this, TableDetails.class);
            detailTableIntent.putExtra("MUUID", mUUID);
            detailTableIntent.putExtra("Range", range);
            startActivity(detailTableIntent);
        }
    }

    private void callChart(String zeitRaum) {
        long from;
        long to;
        long millisNow = System.currentTimeMillis();

        switch (zeitRaum) {
            case "hour":
                to = millisNow;
                from = millisNow - 3600000;
                break;
            case "day":
                to = millisNow;
                from = millisNow - 86400000;
                break;
            case "week":
                to = millisNow;
                from = millisNow - 604800000;
                break;
            case "month":
                to = millisNow;
                from = millisNow - 2419200000L;
                break;

            default:
                to = millisNow;
                from = millisNow - 86400000;
                break;
        }

        Intent detailChartIntent = new Intent(ChannelDetails.this, ChartDetails.class);
        detailChartIntent.putExtra("MUUID", mUUID);
        detailChartIntent.putExtra("From", from);
        detailChartIntent.putExtra("To", to);
        startActivity(detailChartIntent);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("jsonStrGesamt", jsonStrGesamt);
        outState.putString("unit", unit);
    }

    private class GetTotalConsumtion extends AsyncTask<String, Void, String> {
        boolean JSONFehler = false;
        String fehlerAusgabe = "";

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Showing progress dialog
            pDialog = new ProgressDialog(ChannelDetails.this);
            pDialog.setMessage(getString(R.string.please_wait));
            pDialog.setCancelable(false);
            pDialog.show();

        }

        @Override
        protected String doInBackground(String... arg0) {
            // Creating service handler class instance
            ServiceHandler sh = new ServiceHandler();
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(ChannelDetails.this);
            String url = sharedPref.getString("volkszaehlerURL", "");
            String urlDef = url + "/data/" + mUUID + ".json?from=0&tuples=1&group=day";

            String uname = sharedPref.getString("username", "");
            String pwd = sharedPref.getString("password", "");
            Log.d("ChannelDetails", "urlDef: " + urlDef);

            // Making a request to url and getting response
            if (uname.equals("")) {
                jsonStrGesamt = sh.makeServiceCall(urlDef, ServiceHandler.GET);
            } else {
                jsonStrGesamt = sh.makeServiceCall(urlDef, ServiceHandler.GET, null, uname, pwd);
            }

            if (jsonStrGesamt.startsWith("Error: ")) {
                JSONFehler = true;
                fehlerAusgabe = jsonStrGesamt;
            } else {
                Log.d("ChannelDetails", "jsonStrGesamt: " + jsonStrGesamt);
                try {
                    if (gas) {
                        jsonStrGesamt = String.valueOf(Tools.f000.format(new JSONObject(jsonStrGesamt).getJSONObject(Tools.TAG_DATA).getDouble(Tools.TAG_CONSUMPTION) + Double.valueOf(arg0[0])));
                    } else if (strom) {
                        jsonStrGesamt = String.valueOf(Tools.f000.format((new JSONObject(jsonStrGesamt).getJSONObject(Tools.TAG_DATA).getDouble(Tools.TAG_CONSUMPTION) + Double.valueOf(arg0[0]) * 1000) / 1000));
                    } else if (water) {
                        jsonStrGesamt = String.valueOf(Tools.f0.format(new JSONObject(jsonStrGesamt).getJSONObject(Tools.TAG_DATA).getDouble(Tools.TAG_CONSUMPTION) + Double.valueOf(arg0[0])));
                    }
                } catch (JSONException je) {
                    Log.e("ChannelDetails", je.getMessage());
                }
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
            if (JSONFehler) {
                new AlertDialog.Builder(ChannelDetails.this).setTitle(getString(R.string.Error)).setMessage(fehlerAusgabe).setNeutralButton(getString(R.string.Close), null).show();
            } else {
                ((TextView) findViewById(R.id.textViewGesamt)).setText(jsonStrGesamt + " " + unit);
            }
        }
    }

}
