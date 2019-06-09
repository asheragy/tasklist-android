package org.cerion.tasklist.dialogs


import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.DatePicker
import androidx.fragment.app.DialogFragment
import java.util.*

class DatePickerFragment : DialogFragment(), DatePickerDialog.OnDateSetListener {

    private lateinit var listener: DatePickerListener

    interface DatePickerListener {
        fun onSelectDate(date: Date)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val fragment = targetFragment
        if (fragment != null && fragment is DatePickerListener)
            listener = fragment
        else
            throw RuntimeException("parent does not implement DatePickerListener")
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val c = Calendar.getInstance()

        val bundle = arguments
        c.timeZone = TimeZone.getTimeZone("UTC")

        //If date is not set use default which is current day
        val date = bundle!!.getLong(DATE)
        if (date > 0)
            c.timeInMillis = date

        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        val day = c.get(Calendar.DAY_OF_MONTH)

        return DatePickerDialog(activity!!, this, year, month, day)
    }

    override fun onDateSet(view: DatePicker, year: Int, monthOfYear: Int, dayOfMonth: Int) {
        val c = Calendar.getInstance()
        c.timeZone = TimeZone.getTimeZone("UTC")
        c.timeInMillis = 0
        c.set(Calendar.YEAR, year)
        c.set(Calendar.MONTH, monthOfYear)
        c.set(Calendar.DAY_OF_MONTH, dayOfMonth)

        listener.onSelectDate(c.time)
    }

    companion object {
        private const val DATE = "date"

        fun newInstance(date: Date?): DatePickerFragment {
            val frag = DatePickerFragment()

            val args = Bundle()
            if (date != null)
                args.putLong(DATE, date.time)
            else
                args.putLong(DATE, 0)

            frag.arguments = args

            return frag
        }
    }
}
