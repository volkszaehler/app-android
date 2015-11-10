package org.volkszaehler.volkszaehlerapp;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Locale;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.SeriesSelection;
import org.achartengine.model.TimeSeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;


public class ChartDetails extends Activity  {

    private ProgressDialog pDialog;

    private PopupWindow pw;

    private final XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();

    private final XYMultipleSeriesRenderer mRenderer = new XYMultipleSeriesRenderer();

    private GraphicalView mChartView;

    // int mColor = Color.BLUE;
    private String mUUID = "";
    private String jsonStr = "";

    private double xmin = 0;
    private double xmax = 0;
    private double from = 0;
    private double to = 0;
    private double keepFrom = 0;
    private double keepTo = 0;
    private long millisNow = 0;
    // double mCost = 0;
    private double minY = Double.MAX_VALUE;
    private double minX = 0;
    private double maxX = 0;
    // String mTitle = "";
    private String unit = "";

    private java.text.DateFormat dateFormat = null;
    private java.text.DateFormat timeFormat = null;
    private Context myContext = null;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.charts);
        myContext = this;
        // after OrientationChange
        if (savedInstanceState != null) {
            from = savedInstanceState.getDouble("From");
            to = savedInstanceState.getDouble("To");
            keepFrom = savedInstanceState.getDouble("KeepFrom");
            keepTo = savedInstanceState.getDouble("KeepTo");
            jsonStr = savedInstanceState.getString("JSONStr");
        }
        Button select = (Button) findViewById(R.id.buttonDate);

        select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent dateTimeSelector = new Intent(ChartDetails.this, DateTimeSelector.class);
                dateTimeSelector.putExtra("From", (long) from);
                dateTimeSelector.putExtra("To", (long) to);
                dateTimeSelector.putExtra("MUUID", mUUID);
                startActivity(dateTimeSelector);
            }
        });

        Intent inte = this.getIntent();
        mUUID = inte.getStringExtra("MUUID");
        unit = Tools.getUnit(myContext, null, mUUID);

        // from/to only the first time from intent, next time controlled by
        // buttons
        if (from == 0) {
            from = inte.getLongExtra("From", 0);
            to = inte.getLongExtra("To", 0);
        }

        dateFormat = android.text.format.DateFormat.getDateFormat(getApplicationContext());
        timeFormat = android.text.format.DateFormat.getTimeFormat(getApplicationContext());

        new GetChannelsDetails().execute();
    }

    private final ArrayList<String> uUIDSOfaddedCharts = new ArrayList<>();

    private void prepareChart(String uUID) {
        TimeSeries mCurrentSeries = Tools.getTimeSeries(myContext, uUID);
        //skip empty series
        if(mCurrentSeries.getItemCount()<=1)
        {
            //no data or out of time range
            return;
        }
        String localMTitle = Tools.getPropertyOfChannel(myContext, uUID, Tools.TAG_TITLE);
        mCurrentSeries.setTitle(localMTitle + " " + getString(R.string.from) + " " + dateFormat.format(mCurrentSeries.getMinX()) + " " + timeFormat.format(mCurrentSeries.getMinX()) + " "
                + getString(R.string.to) + " " + dateFormat.format(mCurrentSeries.getMaxX()) + " " + timeFormat.format(mCurrentSeries.getMaxX()));
        // get for minimal Y value
        minY = minY > mCurrentSeries.getMinY() ? minY = mCurrentSeries.getMinY() : minY;

        // x values should be the same for all childs
        minX = mCurrentSeries.getMinX();
        maxX = mCurrentSeries.getMaxX();
        dataset.addSeries(mCurrentSeries);
        uUIDSOfaddedCharts.add(uUID);
        int mColor = Color.BLUE;
        try {
                mColor = Color.parseColor(Tools.getPropertyOfChannel(myContext, uUID, Tools.TAG_COLOR).toUpperCase(Locale.getDefault()));
        } catch (Exception e) {
            Log.e("ChartDetails", e.getMessage());
        }
        XYSeriesRenderer renderer = new XYSeriesRenderer();
        renderer.setColor(mColor);
        renderer.setLineWidth(3);
        mRenderer.addSeriesRenderer(renderer);
    }

    private void prepareData() {
        dataset.clear();
        mRenderer.removeAllRenderers();

        if ((Tools.getPropertyOfChannel(myContext, mUUID, Tools.TAG_TYPE)).equals("group")) {
            String childUUIDs = Tools.getPropertyOfChannel(myContext, mUUID, Tools.TAG_CHUILDUUIDS);

            if (null != childUUIDs && !"".equals(childUUIDs)) {
                if (childUUIDs.contains("|")) {
                    String[] children = (childUUIDs.split("\\|"));

                    for (String child : children) {
                        prepareChart(child);
                    }
                } else {
                    // only one uuid in childs
                    prepareChart(childUUIDs);
                }
            }
        } else {
            // no group
            prepareChart(mUUID);
        }

        // the graph itself

        mRenderer.setMarginsColor(Color.argb(0xff, 0x00, 0x00, 0x00));
        mRenderer.setAxisTitleTextSize(20);
        mRenderer.setChartTitleTextSize(20);
        mRenderer.setLabelsTextSize(20);
        mRenderer.setLegendTextSize(20);
        mRenderer.setMargins(new int[]{10, 20, 10, 10});
        mRenderer.setXLabelsColor(Color.GRAY);
        mRenderer.setYLabelsColor(0, Color.GRAY);
        mRenderer.setZoomEnabled(false, false);
        mRenderer.setPanEnabled(false, false);
        mRenderer.setFitLegend(true);

        mRenderer.setClickEnabled(true);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(ChartDetails.this);
        boolean bZeroBased = sharedPref.getBoolean("ZeroBasedYAxis", false);
        if (bZeroBased && minY > 0) {
            mRenderer.setYAxisMin(0);
        } else {
            mRenderer.setYAxisMin(minY - 1);
        }
        mRenderer.setYLabelsColor(0, Color.LTGRAY);
        mRenderer.setYLabels(10);
        mRenderer.setXLabelsColor(Color.LTGRAY);
        mRenderer.setXLabels(6);
        mRenderer.setShowGrid(true);
        mRenderer.setSelectableBuffer(80);

    }

    private float eventXTouchDown = 0;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float eventX = event.getX();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                eventXTouchDown = eventX;
                from = Tools.getMillisValueFromDisplayPoint(myContext, eventX, minX, maxX);

                return true;
            case MotionEvent.ACTION_MOVE:
                // Toast.makeText(this, "move X: " +eventX + " Y: " +eventY ,
                // Toast.LENGTH_SHORT).show();
                break;
            case MotionEvent.ACTION_UP:
                to = Tools.getMillisValueFromDisplayPoint(myContext, eventX, minX, maxX);
                // move from right to left?
                if (from > to) {
                    double newFrom = to;
                    to = from;
                    from = newFrom;
                }
                // really a move or only a touch?
                if (eventXTouchDown != 0 && Math.abs(eventXTouchDown - eventX) > 50) {
                    new GetChannelsDetails().execute();
                } else {
                    SeriesSelection seriesSelection = mChartView.getCurrentSeriesAndPoint();
                    if (seriesSelection != null) {
                        buttonShowInfoHandler(mChartView, uUIDSOfaddedCharts.get(seriesSelection.getSeriesIndex()));
                    }
                }
                eventXTouchDown = 0;
                break;
            default:
                return false;
        }
        return true;
    }

    private void createChart() {
        LinearLayout lLayout = (LinearLayout) findViewById(R.id.chart);
        if (mChartView == null) {
            prepareData();
            mChartView = ChartFactory.getTimeChartView(this, dataset, mRenderer, dateFormat.toString());
            lLayout.removeAllViews();
            lLayout.addView(mChartView);
        } else {
            mChartView.repaint();
        }
    }

    public void buttonNextHandler(View view) {
        getCurrentlyDisplayedRange();
        from = xmax;
        to = xmax + (xmax - xmin);
        // new "to" in future
        if (millisNow < to) {
            from = millisNow - (xmax - xmin);
            to = millisNow;
        }
        new GetChannelsDetails().execute();
    }

    public void buttonInfoHandler(View view) {
        getCurrentlyDisplayedRange();
        from = xmax;
        to = xmax + (xmax - xmin);
        // new "to" in future
        if (millisNow < to) {
            from = millisNow - (xmax - xmin);
            to = millisNow;
        }
        new GetChannelsDetails().execute();
    }

    public void buttonLastHandler(View view) {
        getCurrentlyDisplayedRange();
        to = xmin;
        from = xmin - (xmax - xmin);
        // new "to" in future
        if (millisNow < to) {
            to = millisNow;
            from = millisNow - (xmax - xmin);
        }
        new GetChannelsDetails().execute();
    }

    public void buttonZoomOutHandler(View view) {
        getCurrentlyDisplayedRange();
        from = xmin - ((xmax - xmin) / 2);
        to = xmax + ((xmax - xmin) / 2);
        // new "to" in future
        if (millisNow < to) {
            to = millisNow;
            from = millisNow - 2 * (xmax - xmin);
        }
        new GetChannelsDetails().execute();
    }

    public void buttonZoomInHandler(View view) {
        getCurrentlyDisplayedRange();
        from = xmin + ((xmax - xmin) / 4);
        to = xmax - ((xmax - xmin) / 4);
        // new "to" in future
        if (millisNow < to) {
            to = millisNow;
            from = millisNow - 0.5 * (xmax - xmin);
        }
        new GetChannelsDetails().execute();
    }

    private void getCurrentlyDisplayedRange() {
        millisNow = System.currentTimeMillis();
        xmin = mRenderer.getXAxisMin();
        xmax = mRenderer.getXAxisMax();
        // Fallback because not zoomed/panned graph show wrong values
        if (xmin > 9999999999999L || xmin < -9999999999999L) {
            xmin = minX;
        }
        if (xmax > 9999999999999L || xmax < -9999999999999L) {
            xmax = maxX;
        }
    }

    private void buttonShowInfoHandler(View view, String UUID) {
        try {
            // We need to get the instance of the LayoutInflater, use the
            // context of this activity
            LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            // Inflate the view from a predefined XML layout
            View layout = inflater.inflate(R.layout.info_popup, (ViewGroup) findViewById(R.id.popup_element));

            pw = new PopupWindow(layout, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
            pw.showAtLocation(layout, Gravity.CENTER, 0, 0);

            Button cancelButton = (Button) layout.findViewById(R.id.end_data_send_button);
            cancelButton.setOnClickListener(cancel_button_click_listener);
            DecimalFormat f3 = new DecimalFormat("#0.000");
            DecimalFormat f2 = new DecimalFormat("#0.00");
            ((TextView) layout.findViewById(R.id.minWertIDValue)).setText(f3.format(Double.parseDouble(Tools.getDataOfChannel(myContext, UUID, Tools.TAG_MIN))) + " " + unit);
            ((TextView) layout.findViewById(R.id.maxWertIDValue)).setText(f3.format(Double.parseDouble(Tools.getDataOfChannel(myContext, UUID, Tools.TAG_MAX))) + " " + unit);
            ((TextView) layout.findViewById(R.id.avWertIDValue)).setText(f3.format(Double.parseDouble(Tools.getDataOfChannel(myContext, UUID, Tools.TAG_AVERAGE))) + " " + unit);
            ((TextView) layout.findViewById(R.id.lastWertIDValue)).setText(f3.format(Double.parseDouble(Tools.getDataOfChannel(myContext, UUID, "letzter"))) + " " + unit);
            ((TextView) layout.findViewById(R.id.rowWertIDValue)).setText(Tools.getDataOfChannel(myContext, UUID, Tools.TAG_ROWS));

            String consumptionWert = Tools.getDataOfChannel(myContext, UUID, Tools.TAG_CONSUMPTION);
            if (!"".equals(consumptionWert)) {
                if("gas".equals(Tools.getPropertyOfChannel(myContext,UUID,"type")))
                {
                    ((TextView) layout.findViewById(R.id.conWertIDValue)).setText(f3.format(Double.parseDouble(consumptionWert)) + " " + unit.substring(0,2));
                    ((TextView) layout.findViewById(R.id.costWertIDValue)).setText(f2.format(Double.valueOf(Tools.getPropertyOfChannel(myContext, UUID, Tools.TAG_COST)) * Double.valueOf(consumptionWert)) + " €");
                }
                else {
                    ((TextView) layout.findViewById(R.id.conWertIDValue)).setText(f3.format(Double.parseDouble(consumptionWert) + " " + unit + "h"));
                    ((TextView) layout.findViewById(R.id.costWertIDValue)).setText(f2.format(Double.valueOf(Tools.getPropertyOfChannel(myContext, UUID, Tools.TAG_COST)) / 1000 * Double.valueOf(consumptionWert)) + " €");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final OnClickListener cancel_button_click_listener = new OnClickListener() {
        public void onClick(View v) {
            pw.dismiss();
        }
    };

    private class GetChannelsDetails extends AsyncTask<Void, Void, String> {
        JSONArray werte = null;
        boolean JSONFehler = false;
        String fehlerAusgabe = "";

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Showing progress dialog
            pDialog = new ProgressDialog(ChartDetails.this);
            pDialog.setMessage(getString(R.string.please_wait));
            pDialog.setCancelable(false);
            pDialog.show();

        }

        @Override
        protected String doInBackground(Void... arg0) {
            String uRLUUIDs = "";

            // really a reLoad necessary? or only an orientation change?
            if (!(from == keepFrom && to == keepTo)) {

                // Creating service handler class instance
                ServiceHandler sh = new ServiceHandler();
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(ChartDetails.this);
                String url = sharedPref.getString("volkszaehlerURL", "");

                DecimalFormat f = new DecimalFormat("#0");

                // use VZ-Aggregation for faster response
                String urlExtension = to - from > 604800000 ? "&group=hour" : "";

                // are there child uuids? (in case of a group)
                if ((Tools.getPropertyOfChannel(myContext, mUUID, Tools.TAG_TYPE)).equals("group")) {
                    String childUUIDs = Tools.getPropertyOfChannel(myContext, mUUID, Tools.TAG_CHUILDUUIDS);
                    Log.d("ChartDetails", "childUUIDs: " + childUUIDs);

                    if (null != childUUIDs && !"".equals(childUUIDs)) {
                        if (childUUIDs.contains("|")) {
                            String[] children = (childUUIDs.split("\\|"));

                            for (String child : children) {
                                uRLUUIDs = uRLUUIDs + "&uuid[]=" + child;
                            }
                        } else {
                            // only one uuid in childs
                            uRLUUIDs = "&uuid[]=" + childUUIDs;
                        }
                    }
                } else {
                    // no group
                    uRLUUIDs = "&uuid[]=" + mUUID;
                }

                url = url + "/data.json?from=" + f.format(from) + "&to=" + f.format(to) + "&tuples=1000" + uRLUUIDs + urlExtension;
                Log.d("CahrtDetails", "request url is: " + url);

                String uname = sharedPref.getString("username", "");
                String pwd = sharedPref.getString("password", "");

                // Making a request to url and getting response
                if (uname.equals("")) {
                    jsonStr = sh.makeServiceCall(url, ServiceHandler.GET);
                } else {
                    jsonStr = sh.makeServiceCall(url, ServiceHandler.GET, null, uname, pwd);
                }
            }

            if (jsonStr != null) {
                if (!jsonStr.startsWith("{\"version\":\"0.3\",\"data")) {
                    JSONFehler = true;
                    fehlerAusgabe = jsonStr;
                } else {

                    // store all data stuff in a shared preference
                    getApplicationContext().getSharedPreferences("JSONChannelPrefs", Activity.MODE_PRIVATE).edit().putString("JSONChannelsData", jsonStr).commit();

                    JSONObject jsonObj;
                    try {
                        jsonObj = new JSONObject(jsonStr);
                        werte = jsonObj.getJSONArray(Tools.TAG_DATA);
                        for (int l = 0; l < werte.length(); l++) {
                            JSONObject c = werte.getJSONObject(l);
                            if (c.has(Tools.TAG_TUPLES)) {
                                JSONArray tuples = c.getJSONArray(Tools.TAG_TUPLES);
                                // at least one with tuples
                                JSONFehler = false;
                                break;
                            } else {
                                JSONFehler = true;
                                fehlerAusgabe = "no tuples data";
                            }
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                Log.e("ChartDetails", "Couldn't get any data from the url");
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
            if (JSONFehler) {
                new AlertDialog.Builder(ChartDetails.this).setTitle(getString(R.string.Error)).setMessage(fehlerAusgabe).setNeutralButton(getString(R.string.Close), null).show();
            } else {
                mChartView = null;
                keepFrom = from;
                keepTo = to;
                createChart();
            }

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case R.id.action_settings:
                startActivity(new Intent(this, Preferences.class));
                return (true);
            case R.id.backup_settings:
                boolean saved = Tools.saveFile(getApplicationContext());
                if(saved)
                {
                    Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show();
                }
                else
                {
                    Toast.makeText(this, R.string.notsaved , Toast.LENGTH_SHORT).show();
                }
                return (true);
            case R.id.restore_settings:

                boolean restored = Tools.loadFile(getApplicationContext());
                if(restored)
                {
                    Toast.makeText(this, R.string.restored , Toast.LENGTH_SHORT).show();
                }
                else
                {
                    Toast.makeText(this, R.string.notrestored , Toast.LENGTH_SHORT).show();
                }
                return (true);

            case R.id.about:
                String app_ver = "";
                try {
                    app_ver = this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName;
                } catch (NameNotFoundException e) {
                    Log.e("ChannelDetails","strange VersionName");
                }
                new AlertDialog.Builder(this).setTitle(getString(R.string.app_name)).setMessage(getString(R.string.version) + ": " + app_ver).setNeutralButton(getString(R.string.Close), null).show();
                return (true);

            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putDouble("From", from);
        outState.putDouble("To", to);
        outState.putDouble("KeepFrom", keepFrom);
        outState.putDouble("KeepTo", keepTo);
        outState.putString("JSONStr", jsonStr);
        // outState.putDouble("MCost", mCost);
    }
}
