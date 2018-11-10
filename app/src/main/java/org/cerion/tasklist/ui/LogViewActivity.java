package org.cerion.tasklist.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import org.cerion.tasklist.R;
import org.cerion.tasklist.data.Prefs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class LogViewActivity extends AppCompatActivity {

    private static final String TAG = LogViewActivity.class.getSimpleName();
    private TextView mText;
    private ScrollView mScroll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Prefs.getInstance(this).isDarkTheme())
            setTheme(R.style.AppTheme_Dark);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_view);

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        if(toolbar != null)
            setSupportActionBar(toolbar);

        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        mText = (TextView)findViewById(R.id.text);
        mScroll = (ScrollView)findViewById(R.id.scrollView) ;
        updateLog();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle arrow click here
        if (item.getItemId() == android.R.id.home) {
            finish(); // close this activity and return to preview activity (if there is any)
        }

        return super.onOptionsItemSelected(item);
    }

    private void updateLog() {
        Log.d(TAG, "updating log");

        try {
            Runtime rt = Runtime.getRuntime();
            Process process = rt.exec("logcat -d " + getApplicationContext().getPackageName() + ":D -v time");
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            StringBuilder log=new StringBuilder();
            String line;

            while ((line = bufferedReader.readLine()) != null) {
                String t = parseLine(line);
                if(t != null)
                    log.append(t);
            }

            mText.setText(log.toString());

        } catch (IOException e) {
            //Nothing for now
        }

        // Scroll to end
        mScroll.post(new Runnable() {
            @Override
            public void run() {
                mScroll.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    private String parseLine(String line) {
        if(line.contains("OpenGLRenderer") || line.contains("HostConnection::get()"))
            return null;

        Pattern r = Pattern.compile("\\d\\d-\\d\\d \\d\\d:\\d\\d:\\d\\d.\\d\\d\\d\\s+\\d+\\s+\\d+\\s+.\\s");
        Matcher m = r.matcher(line);

        if(m.find()) {
            line = line.substring(0, 14) + " " + line.charAt(m.end() - 2) + "/" + line.substring(m.end());
        }

        return line + "\n";
    }
}
