package org.volkszaehler.volkszaehlerapp;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import java.text.DateFormat;
import java.util.Calendar;

public class DateSelector extends CustomMenuActivity implements View.OnClickListener {

    private EditText editDFrom;
    private EditText editDTo;
    //private checkGroup;

    private Button set;

    private long from = 0;
    private long to = 0;
    private String mUUID = "";
    private boolean groupMessageBoxShowed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_date_selector);
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

        editDFrom = (EditText) findViewById(R.id.fromDateOnlyPicker);
        editDFrom.setText(DateFormat.getDateInstance().format(from));
        editDFrom.setInputType(InputType.TYPE_NULL);
        editDFrom.setOnClickListener(this);


        editDTo = (EditText) findViewById(R.id.toDateOnlyPicker);
        editDTo.setText(DateFormat.getDateInstance().format(to));
        editDTo.setInputType(InputType.TYPE_NULL);
        editDTo.setOnClickListener(this);

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
        String intervall;

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
        } else if (view == set) {
            if (from > to) {
                new AlertDialog.Builder(DateSelector.this).setTitle(getString(R.string.Error)).setMessage(getString(R.string.FromGreaterTo)).setNeutralButton(getString(R.string.Close), null).show();
            } else {
                CheckBox groupBox = (CheckBox) findViewById(R.id.group_checkbox);
                RadioGroup rg = (RadioGroup) findViewById(R.id.radioButtonGroup);
                String test = (String) ((RadioButton) findViewById(rg.getCheckedRadioButtonId())).getText();
                switch (rg.getCheckedRadioButtonId()) {
                    case R.id.radioButtonHour:
                        intervall = "hour";
                        break;
                    case R.id.radioButtonDay:
                        intervall = "day";
                        break;
                    case R.id.radioButtonWeek:
                        intervall = "week";
                        break;
                    case R.id.radioButtonMonth:
                        intervall = "month";
                        break;
                    default:
                        intervall = "day";
                        break;
                }

                Intent detailTableIntent = new Intent(DateSelector.this, TableDetails.class);
                detailTableIntent.putExtra("MUUID", mUUID);
                detailTableIntent.putExtra("Range", "custom");
                detailTableIntent.putExtra("From", from);
                detailTableIntent.putExtra("To", to);
                detailTableIntent.putExtra("Intervall", intervall);
                detailTableIntent.putExtra("Group", groupBox.isChecked());
                startActivity(detailTableIntent);
            }
        }
    }

    public void onGroupCheckboxClicked(View view) {
        if(!groupMessageBoxShowed) {
            new AlertDialog.Builder(DateSelector.this).setTitle(getString(R.string.Details)).setMessage(getString(R.string.groupHint)).setNeutralButton(getString(R.string.Close), null).show();
            groupMessageBoxShowed = true;
        }
    }
}
