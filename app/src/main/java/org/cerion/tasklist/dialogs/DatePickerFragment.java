package org.cerion.tasklist.dialogs;


import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.widget.DatePicker;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

public class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {

    private static final String DATE = "date";
    private DatePickerListener listener;

    public interface DatePickerListener {
        void onSelectDate(Date date);
    }

    public static DatePickerFragment newInstance(Date date) {
        DatePickerFragment frag = new DatePickerFragment();

        Bundle args = new Bundle();
        if (date != null)
            args.putLong(DATE, date.getTime());
        else
            args.putLong(DATE, 0);
        
        frag.setArguments(args);

        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Fragment fragment = getTargetFragment();
        if (fragment != null && fragment instanceof DatePickerListener)
            listener = (DatePickerListener)fragment;
        else
            throw new RuntimeException("parent does not implement DatePickerListener");
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

    @Override
    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
        Calendar c = Calendar.getInstance();
        c.setTimeZone(TimeZone.getTimeZone("UTC"));
        c.setTimeInMillis(0);
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH,monthOfYear);
        c.set(Calendar.DAY_OF_MONTH, dayOfMonth);

        if (listener != null)
            listener.onSelectDate(c.getTime());
    }
}
