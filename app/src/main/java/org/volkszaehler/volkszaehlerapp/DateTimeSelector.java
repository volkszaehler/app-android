package org.volkszaehler.volkszaehlerapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;

import java.text.DateFormat;
import java.util.Calendar;

public class DateTimeSelector extends Activity implements View.OnClickListener {

    private EditText editDFrom;
    private EditText editTFrom;
    private EditText editDTo;
    private EditText editTTo;

    private Button set;

    private long from = 0;
    private long to = 0;
    private String mUUID = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_date_time_selector);
        if (savedInstanceState != null) {
            from = savedInstanceState.getLong("From");
            to = savedInstanceState.getLong("To");
            mUUID = savedInstanceState.getString("MUUID");
        }

        Intent inte = this.getIntent();

        if (from == 0) {
            from = inte.getLongExtra("From", 0);
            to = inte.getLongExtra("To", 0);
            mUUID = inte.getStringExtra("MUUID");
        }

        editDFrom = (EditText) findViewById(R.id.fromDatePicker);
        editDFrom.setText(DateFormat.getDateInstance().format(from));
        editDFrom.setInputType(InputType.TYPE_NULL);
        editDFrom.setOnClickListener(this);

        editTFrom = (EditText) findViewById(R.id.fromTimePicker);
        editTFrom.setText(DateFormat.getTimeInstance().format(from));
        editTFrom.setInputType(InputType.TYPE_NULL);
        editTFrom.setOnClickListener(this);

        editDTo = (EditText) findViewById(R.id.toDatePicker);
        editDTo.setText(DateFormat.getDateInstance().format(to));
        editDTo.setInputType(InputType.TYPE_NULL);
        editDTo.setOnClickListener(this);

        editTTo = (EditText) findViewById(R.id.toTimePicker);
        editTTo.setText(DateFormat.getTimeInstance().format(to));
        editTTo.setInputType(InputType.TYPE_NULL);
        editTTo.setOnClickListener(this);

        // Launch Time Picker Dialog

        set = (Button) findViewById(R.id.btnSet);
        set.setOnClickListener(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putLong("From", from);
        outState.putLong("To", to);
        outState.putString("MUUID", mUUID);
    }

    @Override
    public void onClick(View view) {
        Calendar newCalendarFrom = Calendar.getInstance();
        newCalendarFrom.setTimeInMillis(from);
        final int yearFrom = newCalendarFrom.get(Calendar.YEAR);
        final int monthFrom = newCalendarFrom.get(Calendar.MONTH);
        final int dayFrom = newCalendarFrom.get(Calendar.DAY_OF_MONTH);
        final int hourFrom = newCalendarFrom.get(Calendar.HOUR_OF_DAY);
        final int minuteFrom = newCalendarFrom.get(Calendar.MINUTE);
        Calendar newCalendarTo = Calendar.getInstance();
        newCalendarTo.setTimeInMillis(to);
        final int yearTo = newCalendarTo.get(Calendar.YEAR);
        final int monthTo = newCalendarTo.get(Calendar.MONTH);
        final int dayTo = newCalendarTo.get(Calendar.DAY_OF_MONTH);
        final int hourTo = newCalendarTo.get(Calendar.HOUR_OF_DAY);
        final int minuteTo = newCalendarTo.get(Calendar.MINUTE);

        if (view == editDFrom) {
            DatePickerDialog fromDatePickerDialog = new DatePickerDialog(this, new OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                    Calendar newDate = Calendar.getInstance();
                    newDate.set(year, monthOfYear, dayOfMonth, hourFrom, minuteFrom);
                    editDFrom.setText(DateFormat.getDateInstance().format(newDate.getTime()));
                    from = newDate.getTimeInMillis();
                }
            }, yearFrom, monthFrom, dayFrom);
            fromDatePickerDialog.show();
        } else if (view == editTFrom) {
            TimePickerDialog fromTimePickerDialog = new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                    Calendar newDate = Calendar.getInstance();
                    newDate.set(yearFrom, monthFrom, dayFrom, hourOfDay, minute);
                    editTFrom.setText(DateFormat.getTimeInstance().format(newDate.getTime()));
                    from = newDate.getTimeInMillis();
                }
            }, hourFrom, minuteFrom, false);
            fromTimePickerDialog.show();
        } else if (view == editDTo) {
            DatePickerDialog toDatePickerDialog = new DatePickerDialog(this, new OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                    Calendar newDate = Calendar.getInstance();
                    newDate.set(year, monthOfYear, dayOfMonth, hourTo, minuteTo);
                    editDTo.setText(DateFormat.getDateInstance().format(newDate.getTime()));
                    to = newDate.getTimeInMillis();
                }
            }, yearTo, monthTo, dayTo);
            toDatePickerDialog.show();
        } else if (view == editTTo) {
            TimePickerDialog toTimePickerDialog = new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                    Calendar newDate = Calendar.getInstance();
                    newDate.set(yearTo, monthTo, dayTo, hourOfDay, minute);
                    editTTo.setText(DateFormat.getTimeInstance().format(newDate.getTime()));
                    to = newDate.getTimeInMillis();
                }
            }, hourTo, minuteTo, false);
            toTimePickerDialog.show();
        } else if (view == set) {
            if (from > to) {
                new AlertDialog.Builder(DateTimeSelector.this).setTitle(getString(R.string.Error)).setMessage(getString(R.string.FromGreaterTo)).setNeutralButton(getString(R.string.Close), null).show();
            } else {
                Intent detailChartIntent = new Intent(DateTimeSelector.this, ChartDetails.class);
                detailChartIntent.putExtra("MUUID", mUUID);
                detailChartIntent.putExtra("From", from);
                detailChartIntent.putExtra("To", to);
                startActivity(detailChartIntent);
            }
        }
    }
}
