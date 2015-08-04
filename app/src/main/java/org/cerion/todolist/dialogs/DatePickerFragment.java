package org.cerion.todolist.dialogs;


import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.widget.DatePicker;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {

    private static final String DATE = "date";

    public static DatePickerFragment newInstance(Date date) {
        DatePickerFragment frag = new DatePickerFragment();

        Bundle args = new Bundle();
        args.putLong(DATE, date.getTime());
        frag.setArguments(args);

        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Calendar c = Calendar.getInstance();

        Bundle bundle = getArguments();
        c.setTimeZone(TimeZone.getTimeZone("UTC"));

        //If date is not set use default which is current day
        long date = bundle.getLong(DATE);
        if(date > 0)
            c.setTimeInMillis(date);

        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        return new DatePickerDialog(getActivity(), this, year, month, day);
    }


    //TODO, based on examples there might be a better prefered way of doing this callback...
    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        ((DatePickerDialog.OnDateSetListener) getActivity()).onDateSet(view,year,monthOfYear,dayOfMonth);
    }
}
