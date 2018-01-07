package org.cerion.tasklist.ui;

import android.content.Context;
import android.databinding.ObservableArrayList;
import android.databinding.ObservableField;
import android.databinding.ObservableList;
import android.text.format.DateUtils;

import org.cerion.tasklist.data.Prefs;
import org.cerion.tasklist.data.TaskList;

import java.util.Date;

public class TasksViewModel {

    private Prefs prefs;

    public ObservableList<TaskList> lists = new ObservableArrayList<>();
    public ObservableField<TaskList> currList = new ObservableField<>();

    public ObservableField<String> lastSync = new ObservableField<>("");
    public ObservableField<Boolean> isOutOfSync = new ObservableField<>(false);

    public TasksViewModel(Context context) {
        prefs = Prefs.getInstance(context);
    }

    public void updateLastSync() {
        String sText = "Last Sync: ";
        Date lastSyncTime = prefs.getDate(Prefs.KEY_LAST_SYNC);
        if (lastSyncTime == null || lastSyncTime.getTime() == 0)
            sText += "Never";
        else {
            long now = new Date().getTime();
            if(now - lastSyncTime.getTime() < 60 * 1000)
                sText += "Less than 1 minute ago";
            else
                sText += DateUtils.getRelativeTimeSpanString(lastSyncTime.getTime(), now, DateUtils.SECOND_IN_MILLIS).toString();

            if(now - lastSyncTime.getTime() > (24 * 60 * 60 * 1000))
                isOutOfSync.set(true);
            else
                isOutOfSync.set(false);
        }

        lastSync.set(sText);
    }
}
