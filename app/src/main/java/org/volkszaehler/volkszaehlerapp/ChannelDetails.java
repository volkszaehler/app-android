package org.volkszaehler.volkszaehlerapp;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class ChannelDetails extends Activity {

    private String mUUID = "";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.details);
        Context myContext = this;
        // addListenerOnButton();
        Intent i = getIntent();
        boolean strom = false;
        boolean gas = false;
        boolean temp = false;
        mUUID = i.getStringExtra(Tools.TAG_UUID);

        String typeOfChannel = Tools.getPropertyOfChannel(myContext, mUUID, Tools.TAG_TYPE);
        switch (typeOfChannel) {
            case "power":
            case "powersensor":
                strom = true;
                break;
            case "temperature":
                temp = true;
                break;
            case "gas":
                gas = true;
                break;
        }

        String col = Tools.getPropertyOfChannel(myContext, mUUID, "color");

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
                // new
                // AlertDialog.Builder(MainActivity.this).setTitle("Error").setMessage("Unknown Color: "+
                // col).setNeutralButton("Close",null).show();
            }
        }
        String myTitel = Tools.getPropertyOfChannel(myContext, mUUID, Tools.TAG_TITLE);
        ((TextView) findViewById(R.id.textViewTitle)).setText(myTitel);
        ((TextView) findViewById(R.id.textViewValue)).setText(i.getStringExtra("tuplesWert"));
        try {
            String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date(Long.valueOf(i.getStringExtra("tuplesZeit"))));
            ((TextView) findViewById(R.id.textViewDateValue)).setText(currentDateTimeString);
        } catch (NumberFormatException nfe) {
            // strange millis
        }

        ((TextView) findViewById(R.id.textViewDescription)).setText(Tools.getPropertyOfChannel(myContext, mUUID, Tools.TAG_DESCRIPTION));
        try {
            DecimalFormat f = new DecimalFormat("#0.0");

            double sCost = Double.valueOf(Tools.getPropertyOfChannel(myContext, mUUID, Tools.TAG_COST));
            if (strom) {
                ((TextView) findViewById(R.id.textViewCost)).setText(f.format(sCost * 100) + Units.CENT);
            } else if (gas) {
                ((TextView) findViewById(R.id.textViewCost)).setText(f.format((sCost * 100)) + Units.CENT);
            } else if (temp) {
                // no cost
            }
        } catch (NumberFormatException nfe) {

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

    }

    public void chartsDetailsHandler(View view) {
        switch (view.getId()) {
            case R.id.textViewChartHour:
                callChart("hour");
                break;
            case R.id.textViewChartDay:
                callChart("day");
                break;
            case R.id.textViewChartWeek:
                callChart("week");
                break;
            case R.id.textViewChartMonth:
                callChart("month");
                break;
            default:
                callChart("day");
                break;
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

                }
                new AlertDialog.Builder(this).setTitle(getString(R.string.app_name)).setMessage(getString(R.string.version) + ": " + app_ver).setNeutralButton(getString(R.string.Close), null).show();
                return (true);

            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
