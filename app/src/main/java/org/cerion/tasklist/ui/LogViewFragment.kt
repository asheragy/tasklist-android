package org.cerion.tasklist.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import org.cerion.tasklist.R
import org.cerion.tasklist.common.TAG
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.regex.Pattern


class LogViewFragment : Fragment() {

    private lateinit var mText: TextView
    private lateinit var mScroll: ScrollView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_log_view, container, false)

        (requireActivity() as AppCompatActivity).supportActionBar?.title = "System Log"

        mText = view.findViewById(R.id.text)
        mScroll = view.findViewById(R.id.scrollView)
        updateLog()

        return view
    }

    private fun updateLog() {
        Log.d(TAG, "updating log")

        try {
            val rt = Runtime.getRuntime()
            val process = rt.exec("logcat -d ${android.os.Process.myPid()} -v time")
            val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))

            val log = StringBuilder()
            var line: String?

            while (true) {
                line = bufferedReader.readLine()
                if (line == null)
                    break

                val t = parseLine(line)
                if (t != null)
                    log.append(t)
            }

            mText.text = log.toString()

        } catch (e: IOException) {
            //Nothing for now
        }

        // Scroll to end
        mScroll.post { mScroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun parseLine(line: String): String? {
        var line = line
        if (line.contains("OpenGLRenderer") || line.contains("HostConnection::get()"))
            return null

        val r = Pattern.compile("\\d\\d-\\d\\d \\d\\d:\\d\\d:\\d\\d.\\d\\d\\d\\s+\\d+\\s+\\d+\\s+.\\s")
        val m = r.matcher(line)

        if (m.find()) {
            line = line.substring(0, 14) + " " + line[m.end() - 2] + "/" + line.substring(m.end())
        }

        return line + "\n"
    }
}