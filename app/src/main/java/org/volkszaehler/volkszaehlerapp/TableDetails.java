package org.volkszaehler.volkszaehlerapp;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import static org.volkszaehler.volkszaehlerapp.Tools.*;

public class TableDetails extends Activity {

    private String mUUID = "";
    private String jsonStr = "";
    private String range = "";

    private double from = 0;
    private double to = 0;
    private double keepFrom = 0;
    private double keepTo = 0;
    private Context myContext = null;

    private ProgressDialog pDialog;

    float minMin = 2147483647f;
    float maxMin = 0f;
    float minMax = 2147483647f;
    float maxMax = 0f;
    float minAverage = 2147483647f;
    float maxAverage = 0f;
    float minConsumption = 2147483647f;
    float maxConsumption = 0f;
    String cost = "";
    String resolution = "";

    boolean bConsumption = false;

    List<String[]> tabellenZeilenHolder = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_table_details);

        Intent inte = getIntent();
        mUUID = inte.getStringExtra("MUUID");
        range = inte.getStringExtra("Range");
        myContext = this;

        // after OrientationChange
        if (savedInstanceState != null) {
            from = savedInstanceState.getDouble("From");
            to = savedInstanceState.getDouble("To");
            keepFrom = savedInstanceState.getDouble("KeepFrom");
            keepTo = savedInstanceState.getDouble("KeepTo");
            jsonStr = savedInstanceState.getString("JSONStr");
            mUUID = savedInstanceState.getString("mUUID");
            range = savedInstanceState.getString("Range");
            int size = savedInstanceState.getInt("Size");
            for(int i = 0; i<size;i++) {
                tabellenZeilenHolder.add(savedInstanceState.getStringArray("Tabellenzeile"+i));
            }
            bConsumption = savedInstanceState.getBoolean("BConsumption");
            minMin = savedInstanceState.getFloat("MinMin");
            maxMin = savedInstanceState.getFloat("MaxMin");
            minMax = savedInstanceState.getFloat("MinMax");
            maxMax = savedInstanceState.getFloat("MaxMax");
            minAverage = savedInstanceState.getFloat("MinAv");
            maxAverage = savedInstanceState.getFloat("MaxAv");
            minConsumption = savedInstanceState.getFloat("MinCons");
            maxConsumption = savedInstanceState.getFloat("MaxCons");
        }

        cost = getPropertyOfChannel(myContext, mUUID, TAG_COST);
        resolution = getPropertyOfChannel(myContext, mUUID, TAG_RESOLUTION);

        new GetTableValues().execute();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    private class GetTableValues extends AsyncTask<String, Integer, String> {
        boolean JSONFehler = false;
        String fehlerAusgabe = "";

        GetTableValues() {
            pDialog = new ProgressDialog(TableDetails.this);
            pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            pDialog.setIndeterminate(false);
            pDialog.setMax(100);
            pDialog.setMessage(getString(R.string.please_wait_infinite));
            pDialog.setCancelable(true);
            pDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    // actually could set running = false; right here, but I'll
                    // stick to contract.
                    cancel(true);
                }
            });

        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            lockScreenOrientation();
            pDialog.show();

        }

        @Override
        protected void onCancelled() {
            unlockScreenOrientation();
            if (JSONFehler) {
                new AlertDialog.Builder(TableDetails.this).setTitle(getString(R.string.Error)).setMessage(fehlerAusgabe).setNeutralButton(getString(R.string.Close), null).show();
            } else {
                fillTable(tabellenZeilenHolder);
            }
        }

        @Override
        protected String doInBackground(String... arg0) {
            // really a reLoad necessary? or only an orientation change?
            if (from == 0) {

                // Creating service handler class instance
                ServiceHandler sh = new ServiceHandler();
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(TableDetails.this);
                String tuples = sharedPref.getString("Tuples", "1000");
                String url = sharedPref.getString("volkszaehlerURL", "");
                int durchlaeufe = 0;
                long localfrom = 0;
                long localto = 0;
                long nextValue = 0;
                long millisHour = 3600 * 1000;
                long millisDay = millisHour * 24;
                long millisWeek = millisDay * 7;

                long millisNow = System.currentTimeMillis();
                Calendar c=Calendar.getInstance();
                long millisNow2 = c.getInstance().getTimeInMillis();
                c.setTime(new Date());
                c.setTimeZone(TimeZone.getDefault());
                long millis3 = c.getTimeInMillis();
                tabellenZeilenHolder.clear();

                switch (range) {
                    case "day":
                        nextValue = millisHour;
                        localfrom = (long) Math.floor((millisNow - millisDay) / millisHour) * millisHour;
                        localto = localfrom + nextValue;
                        durchlaeufe = 24;
                        break;
                    case "week":
                        c.set(c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DATE),0,0,0);
                        c.add(Calendar.DATE, -c.get(Calendar.DAY_OF_WEEK));
                        c.add(Calendar.DATE, +c.getFirstDayOfWeek());
                        c.add(Calendar.DATE , -7);
                        long test = c.getTimeInMillis();
                        nextValue = millisDay;
                        long l = millisNow - 7 * millisDay;
                        double v = l / millisDay;
                        localfrom = (long) Math.floor(v) * millisDay;
                        localfrom = test;
                        localto = localfrom + nextValue;
                        durchlaeufe = 7;
                        break;
                    case "month":
                        c.add(Calendar.MONTH , -1);
                        c.set(c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DATE),0,0,0);
                        //c.add(Calendar.DATE, -c.get(Calendar.DAY_OF_WEEK));
                        //c.add(Calendar.DATE, +c.getFirstDayOfWeek());

                        nextValue = millisDay;
                        long test3 = c.getTimeInMillis();
                        localfrom = (long) Math.floor((millisNow - 30 * millisDay) / millisDay) * millisDay;
                        localfrom = test3;
                        localto = localfrom + nextValue;
                        durchlaeufe = (int) ((millis3 - localfrom)/millisDay);
                        break;
                    case "year":
                        c.add(Calendar.YEAR , -1);
                        c.set(c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DATE),0,0,0);
                        c.add(Calendar.DATE, -c.get(Calendar.DAY_OF_WEEK));
                        c.add(Calendar.DATE, +c.getFirstDayOfWeek());

                        long test2 = c.getTimeInMillis();
                        nextValue = millisWeek;
                        localfrom = (long) Math.floor((millisNow - 53 * millisWeek) / millisWeek) * millisWeek;
                        localfrom = test2;
                        localto = localfrom + nextValue;
                        durchlaeufe = 53;
                        break;
                    default:
                        Log.e("TableDetails", "Unknown 'Range': " + range);
                }
                from = localfrom;
                to = localfrom + durchlaeufe * nextValue;

                String min;
                String max;
                String average;
                String consumption;
                String rows;
                String jsonResult;
                String dateTimeString;
                minMin = 2147483647f;
                maxMin = 0f;
                minMax = 2147483647f;
                maxMax = 0f;
                minAverage = 2147483647f;
                maxAverage = 0f;
                minConsumption = 2147483647f;
                maxConsumption = 0f;

                String consumptionWert;


                for (int j = 0; j < durchlaeufe; j++) {

                    if(isCancelled())
                    {
                        break;
                    }

                    String urlDef = url + "/data/" + mUUID + ".json?from=" + localfrom + "&to=" + localto + "&tuples=" + tuples;// + "&group=day";

                    String uname = sharedPref.getString("username", "");
                    String pwd = sharedPref.getString("password", "");
                    Log.d("TableDetails", "urlDef: " + urlDef);

                    // Making a request to url and getting response
                    if (uname.equals("")) {
                        jsonResult = sh.makeServiceCall(urlDef, ServiceHandler.GET);
                    } else {
                        jsonResult = sh.makeServiceCall(urlDef, ServiceHandler.GET, null, uname, pwd);
                    }

                    if (jsonResult.startsWith("Error: ")) {
                        JSONFehler = true;
                        fehlerAusgabe = jsonResult;
                        break;
                    } else {
                        Log.d("TableDetails", "jsonResult: " + jsonResult);
                        try {
                            consumptionWert = getDataOfChannel(myContext, mUUID, jsonResult, TAG_CONSUMPTION);
                            bConsumption = !"".equals(consumptionWert);

                            JSONObject jsonO = new JSONObject(jsonResult).getJSONObject(TAG_DATA);
                            //are there really values
                            if(jsonO.has(TAG_MIN)) {
                                min = jsonO.getJSONArray(TAG_MIN).get(1).toString();
                                max = jsonO.getJSONArray(TAG_MAX).get(1).toString();
                                average = jsonO.get(TAG_AVERAGE).toString();
                                consumption = bConsumption ? jsonO.get(TAG_CONSUMPTION).toString() : "";
                                rows = jsonO.get(TAG_ROWS).toString();
                                dateTimeString = getDateTimeString(localfrom, localto, range, myContext);

                                tabellenZeilenHolder.add(new String[]{dateTimeString, min, max, average, consumption});

                                minMin = minMin < Float.parseFloat(min) ? minMin : Float.parseFloat(min);
                                maxMin = maxMin > Float.parseFloat(min) ? maxMin : Float.parseFloat(min);

                                minMax = minMax < Float.parseFloat(max) ? minMax : Float.parseFloat(max);
                                maxMax = maxMax > Float.parseFloat(max) ? maxMax : Float.parseFloat(max);

                                minAverage = minAverage < Float.parseFloat(average) ? minAverage : Float.parseFloat(average);
                                maxAverage = maxAverage > Float.parseFloat(average) ? maxAverage : Float.parseFloat(average);

                                if (bConsumption) {
                                    minConsumption = minConsumption < Float.parseFloat(consumption) ? minConsumption : Float.parseFloat(consumption);
                                    maxConsumption = maxConsumption > Float.parseFloat(consumption) ? maxConsumption : Float.parseFloat(consumption);
                                }
                            }


                        /*if (gas) {
                            jsonStrGesamt = String.valueOf(Tools.f000.format(new JSONObject(jsonStrGesamt).getJSONObject(Tools.TAG_DATA).getDouble(Tools.TAG_CONSUMPTION) + Double.valueOf(arg0[0])));
                        } else if (strom) {
                            jsonStrGesamt = String.valueOf(Tools.f000.format((new JSONObject(jsonStrGesamt).getJSONObject(Tools.TAG_DATA).getDouble(Tools.TAG_CONSUMPTION) + Double.valueOf(arg0[0]) * 1000) / 1000));
                        } else if (water) {
                            jsonStrGesamt = String.valueOf(Tools.f0.format(new JSONObject(jsonStrGesamt).getJSONObject(Tools.TAG_DATA).getDouble(Tools.TAG_CONSUMPTION) + Double.valueOf(arg0[0])));
                        }*/
                        } catch (JSONException je) {
                            Log.e("TableDetails", je.getMessage());
                            break;
                        }
                    }
                    publishProgress((int) (((j + 1.0) / durchlaeufe) * 100));
                    localfrom = localto;
                    localto = localfrom + nextValue;
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            pDialog.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            unlockScreenOrientation();
            try {
                if (pDialog.isShowing())
                    pDialog.dismiss();
            } catch (Exception e) {
                // handle Exception
            }
            if (JSONFehler) {
                new AlertDialog.Builder(TableDetails.this).setTitle(getString(R.string.Error)).setMessage(fehlerAusgabe).setNeutralButton(getString(R.string.Close), null).show();
            } else {
                fillTable(tabellenZeilenHolder);
            }
        }
    }

    private void fillTable(List<String[]> tabellenZeilenHolder) {
        ((TextView) findViewById(R.id.table_ChannelName)).setText(getPropertyOfChannel(myContext,mUUID, TAG_TITLE));
        findViewById(R.id.table_ChannelName).setBackgroundColor(Color.parseColor(getPropertyOfChannel(myContext, mUUID, TAG_COLOR).toUpperCase(Locale.getDefault())));
        try {
            String currentfromDateTimeString = DateFormat.getDateTimeInstance().format(new Date((long) from));
            String currenttoDateTimeString = DateFormat.getDateTimeInstance().format(new Date((long) to));
            ((TextView) findViewById(R.id.table_textViewDateValue)).setText(currentfromDateTimeString+" - "+currenttoDateTimeString);
            findViewById(R.id.table_textViewDateValue).setTag("from="+from+"&to="+to);
        } catch (NumberFormatException nfe) {
            Log.e("TableDetails","strange date: "+ from);
        }

        if (!bConsumption) {
            //remove consumption and costs from dialog
            findViewById(R.id.tableColumnConsumptionHead).setVisibility(View.GONE);
            findViewById(R.id.tableColumnCostHead).setVisibility(View.GONE);
        }

        TableLayout table = (TableLayout) findViewById(R.id.main_table);

        for (String[] zeitEinheitZeile : tabellenZeilenHolder) {
            addRow(zeitEinheitZeile, tabellenZeilenHolder.size(), table);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private void addRow(String[] rowValues, int anzahlZeilen, TableLayout table) {

        TableRow row = (TableRow) LayoutInflater.from(this).inflate(R.layout.table_row, null);

        ((TextView) row.findViewById(R.id.tableColumnTime)).setText(rowValues[0]);
        TextView viewByIdMin = (TextView) row.findViewById(R.id.tableColumnMin);
        viewByIdMin.setText(f0.format(Float.parseFloat(rowValues[1])));
        viewByIdMin.setBackgroundColor(Color.parseColor(determineColor(anzahlZeilen, determineStufe(minMin, maxMin, anzahlZeilen, rowValues[1]))));

        TextView viewByIdMax = (TextView) row.findViewById(R.id.tableColumnMax);
        viewByIdMax.setText(f0.format(Float.parseFloat(rowValues[2])));
        viewByIdMax.setBackgroundColor(Color.parseColor(determineColor(anzahlZeilen, determineStufe(minMax, maxMax, anzahlZeilen, rowValues[2]))));

        TextView viewByIdAverage = (TextView) row.findViewById(R.id.tableColumnAverage);
        viewByIdAverage.setText(f0.format(Float.parseFloat(rowValues[3])));
        String colorAverageConsumptionCost = determineColor(anzahlZeilen, determineStufe(minAverage, maxAverage, anzahlZeilen, rowValues[3]));
        viewByIdAverage.setBackgroundColor(Color.parseColor(colorAverageConsumptionCost));

        if (bConsumption) {
            TextView viewByIdConsumption = (TextView) row.findViewById(R.id.tableColumnConsumption);
            viewByIdConsumption.setText(f0.format(Float.parseFloat(rowValues[4])));
            viewByIdConsumption.setBackgroundColor(Color.parseColor(colorAverageConsumptionCost));

            TextView viewByIdCost = (TextView) row.findViewById(R.id.tableColumnCost);
            if(!"".equals((cost))) {
                viewByIdCost.setText(String.format("%s €", f00.format(Double.valueOf(cost) * Double.valueOf(rowValues[4]) / 1000)));
            }
            viewByIdCost.setBackgroundColor(Color.parseColor(colorAverageConsumptionCost));
        } else {
            //remove consumption and costs from dialog
            row.findViewById(R.id.tableColumnConsumption).setVisibility(View.GONE);
            row.findViewById(R.id.tableColumnCost).setVisibility(View.GONE);
        }
        table.addView(row);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putDouble("From", from);
        outState.putDouble("To", to);
        outState.putDouble("KeepFrom", keepFrom);
        outState.putDouble("KeepTo", keepTo);
        outState.putString("JSONStr", jsonStr);
        outState.putString("mUUID", mUUID);
        outState.putString("Range", range);
        int size = tabellenZeilenHolder.size();
        outState.putInt("Size", size);
        for(int i = 0; i< size; i++) {
            outState.putStringArray("Tabellenzeile"+i, tabellenZeilenHolder.get(i));
        }
        outState.putBoolean("BConsumption",bConsumption);
        outState.putFloat("MinMin", minMin);
        outState.putFloat("MaxMin", maxMin);
        outState.putFloat("MinMax", minMax);
        outState.putFloat("MaxMax", maxMax);
        outState.putFloat("MinAv", minAverage);
        outState.putFloat("MaxAv", maxAverage);
        outState.putFloat("MinCons", minConsumption);
        outState.putFloat("MaxCons", maxConsumption);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case R.id.action_settings:
                startActivity(new Intent(this, Preferences.class));
                return (true);
            case R.id.backup_settings:
                boolean saved = saveFile(getApplicationContext());
                if (saved) {
                    Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.notsaved, Toast.LENGTH_SHORT).show();
                }
                return (true);
            case R.id.restore_settings:

                boolean restored = loadFile(getApplicationContext());
                if (restored) {
                    Toast.makeText(this, R.string.restored, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.notrestored, Toast.LENGTH_SHORT).show();
                }
                return (true);

            case R.id.about:
                return showAboutDialog(myContext);

            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void lockScreenOrientation() {
        int currentOrientation = getResources().getConfiguration().orientation;
        if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
    }

    private void unlockScreenOrientation() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
    }
}
