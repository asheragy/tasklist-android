package org.cerion.tasklist.ui;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.AccountPicker;

import org.cerion.tasklist.data.Database;
import org.cerion.tasklist.data.Prefs;
import org.cerion.tasklist.R;
import org.cerion.tasklist.data.Task;
import org.cerion.tasklist.data.TaskList;
import org.cerion.tasklist.dialogs.AlertDialogFragment;
import org.cerion.tasklist.dialogs.TaskListDialogFragment;
import org.cerion.tasklist.dialogs.TaskListDialogFragment.TaskListDialogListener;
import org.cerion.tasklist.sync.OnSyncCompleteListener;
import org.cerion.tasklist.sync.Sync;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends Activity implements TaskListDialogListener, TaskListsToolbar.TaskListsChangeListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int EDIT_TASK_REQUEST = 0;
    private static final int PICK_ACCOUNT_REQUEST = 1;

    private TextView mStatus;
    private SwipeRefreshLayout mSwipeRefresh;
    private int mDefaultTextColor;
    private Prefs mPrefs;

    private TaskListsToolbar mTaskListsToolbar;

    private final TaskListAdapter mTaskListAdapter = new TaskListAdapter(this);
    private static TaskList mCurrList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate " + (savedInstanceState == null ? "null" : "saveState"));
        mPrefs = Prefs.getInstance(this);
        if (mPrefs.getBool(Prefs.KEY_DARK_THEME))
            setTheme(R.style.AppTheme_Dark);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View debug = findViewById(R.id.layoutDebug);
        if(debug != null)
            debug.setVisibility(View.GONE);

        //Toolbar
        mTaskListsToolbar = (TaskListsToolbar) findViewById(R.id.toolbar);
        setActionBar(mTaskListsToolbar);
        if(getActionBar() != null)
            getActionBar().setDisplayShowTitleEnabled(false); //Hide app name, task lists replace title on actionbar


        mStatus = (TextView) findViewById(R.id.status);
        mDefaultTextColor = mStatus != null ? mStatus.getTextColors().getDefaultColor() : 0;

        RecyclerView rv = (RecyclerView) findViewById(android.R.id.list);
        if(rv != null) {
            rv.setLayoutManager(new LinearLayoutManager(this));
            rv.setAdapter(mTaskListAdapter);
        }

        mSwipeRefresh = (SwipeRefreshLayout) findViewById(R.id.swipeRefresh);

        mSwipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                onSync();
            }
        });

        FloatingActionButton fab = (FloatingActionButton)  findViewById(R.id.fab);
        if(fab != null)
            fab.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    onOpenTask(null);
                }
            });


        View logdb = findViewById(R.id.logdb);
        if(logdb != null)
            logdb.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Database db = Database.getInstance(MainActivity.this);
                    db.log();
                    mPrefs.log();
                }
            });

        setLastSync();
        refreshLists();
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    protected void onResume() {
        //Log.d(TAG,"onResume");
        setLastSync();
        super.onResume();
    }

    @Override
    protected void onPause() {
        //Log.d(TAG,"onPause");
        mPrefs.setString(Prefs.KEY_LAST_SELECTED_LIST_ID, mCurrList.id);
        super.onPause();
    }

    /*
    @Override
    protected void onStop() {
        Log.d(TAG,"onStop");
        super.onStop();
    }

    @Override
    protected void onRestart() {
        Log.d(TAG,"onRestart");
        super.onRestart();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG,"onDestroy");
        super.onDestroy();
    }
    */

    private void onSync() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        boolean isConnected = networkInfo != null && networkInfo.isConnected();

        if (isConnected) {
            setInSync(true);

            Sync.run(this, this, new OnSyncCompleteListener() {
                @Override
                public void onAuthError(Exception e) {
                    setInSync(false);

                    if (e == null) {
                        onChooseAccount();
                    } else if (e instanceof OperationCanceledException) {
                        Log.d(TAG, "User denied auth prompt");
                        //For some reason showing an AlertDialog here causes a crash
                    } else {
                        DialogFragment dialog = AlertDialogFragment.newInstance("Auth Error", e.getMessage());
                        dialog.show(getFragmentManager(), "dialog");
                    }
                }

                @Override
                public void onSyncFinish(boolean bSuccess, Exception e) {
                    setInSync(false);

                    if (bSuccess) {
                        setLastSync(); //Update last sync time only if successful
                    } else {
                        String message = "Sync Failed, unknown error";
                        if (e != null)
                            message = e.getMessage();

                        DialogFragment dialog = AlertDialogFragment.newInstance("Sync failed", message);
                        dialog.show(getFragmentManager(), "dialog");
                    }

                    refreshAll(); //refresh since data may have changed
                }

            });
        } else {
            DialogFragment dialog = AlertDialogFragment.newInstance("Error", "Internet not available");
            dialog.show(getFragmentManager(), "dialog");
            if (mSwipeRefresh.isRefreshing())
                mSwipeRefresh.setRefreshing(false);
        }
    }

    private void setLastSync() {
        String sText = "Last Sync: ";
        Date lastSync = mPrefs.getDate(Prefs.KEY_LAST_SYNC);
        if (lastSync == null || lastSync.getTime() == 0)
            sText += "Never";
        else {
            long now = new Date().getTime();
            if(now - lastSync.getTime() < 60 * 1000)
                sText += "Less than 1 minute ago";
            else
                sText += DateUtils.getRelativeTimeSpanString(lastSync.getTime(), now, DateUtils.SECOND_IN_MILLIS).toString();

            if(now - lastSync.getTime() > (24 * 60 * 60 * 1000))
                mStatus.setTextColor(Color.RED);
            else
                mStatus.setTextColor(mDefaultTextColor);
        }

        mStatus.setText(sText);
    }

    private void setInSync(boolean bSyncing) {
        if (!bSyncing && mSwipeRefresh.isRefreshing())
            mSwipeRefresh.setRefreshing(false);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult: " + resultCode);

        if (resultCode == RESULT_OK) {
            if (requestCode == EDIT_TASK_REQUEST)
                refreshTasks();
            else if (requestCode == PICK_ACCOUNT_REQUEST) {
                String currentAccount = mPrefs.getString(Prefs.KEY_ACCOUNT_NAME);
                String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);

                //If current account is set and different than selected account, logout first
                if (currentAccount.length() > 0 && !currentAccount.contentEquals(accountName))
                    onLogout();

                mPrefs.setString(Prefs.KEY_ACCOUNT_NAME, accountName);
            }
        }
    }

    public void onOpenTask(Task task) {
        Intent intent = new Intent(this, TaskActivity.class);

        //Send with task or tasklist parameter if new
        if (task != null)
            intent.putExtra(TaskActivity.EXTRA_TASK, task);
        else {
            TaskList list = mCurrList;
            if(list.isAllTasks())
                list = mTaskListsToolbar.getDefaultList();
            intent.putExtra(TaskActivity.EXTRA_TASKLIST, list);
        }

        startActivityForResult(intent, EDIT_TASK_REQUEST);
    }

    @Override
    public void onFinishTaskListDialog(TaskList current) {
        mCurrList = current; //This list was added or updated
        refreshLists();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_rename).setVisible(!mCurrList.isAllTasks()); //Hide rename if "All Tasks" list

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch(id) {
            case R.id.action_add: onAddTaskList(); break;
            case R.id.action_account: onChooseAccount(); break;
            case R.id.action_logout: onLogout(); break;
            case R.id.action_theme: onChangeTheme(); break;
            case R.id.action_clear_completed: onClearCompleted(); break;
            case R.id.action_rename:
                TaskListDialogFragment dialog = TaskListDialogFragment.newInstance(TaskListDialogFragment.TYPE_RENAME, mCurrList);
                dialog.show(getFragmentManager(), "dialog");
                break;
            case R.id.action_view_log:
                Intent intent = new Intent(this, LogViewActivity.class);
                startActivity(intent);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void onClearCompleted() {
        Log.d(TAG,"onClearCompleted");
        Database db = Database.getInstance(this);
        db.tasks.clearCompleted(mCurrList);

        refreshTasks();
    }

    private void onChangeTheme() {
        //Toggle theme
        mPrefs.setBool(Prefs.KEY_DARK_THEME, !mPrefs.getBool(Prefs.KEY_DARK_THEME));

        //Restart activity
        finish();
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void onLogout() {
        Log.d(TAG, "onLogout");
        Database db = Database.getInstance(MainActivity.this);

        //Move un-synced task to this default list
        TaskList defaultList = mTaskListsToolbar.getDefaultList();

        //Delete all non-temp Id records, also remove records marked as deleted
        List<Task> tasks = db.tasks.getList(null);
        for (Task task : tasks) {
            if (!task.hasTempId() || task.deleted)
                db.tasks.delete(task);
            else {
                //Since we are also removing synced lists, check if we need to move this task to an un-synced list
                TaskList list = new TaskList(task.listId, "");
                if (!list.hasTempId() && defaultList != null) {
                    //Move this task to default list
                    db.setTaskIds(task, task.id, defaultList.id);
                }
            }
        }

        ArrayList<TaskList> lists = db.taskLists.getList();
        for (TaskList list : lists) {
            if (!list.hasTempId()) //don't delete un-synced lists
            {
                if (list.bDefault) { //Keep default but assign temp id
                    db.taskLists.setLastUpdated(list, new Date(0));
                    db.taskLists.setId(list, TaskList.generateId());
                } else
                    db.taskLists.delete(list);
            }
        }

        //Remove prefs related to sync/account
        mPrefs.remove(Prefs.KEY_LAST_SYNC);
        mPrefs.remove(Prefs.KEY_ACCOUNT_NAME);
        mPrefs.remove(Prefs.KEY_AUTHTOKEN);
        mPrefs.remove(Prefs.KEY_AUTHTOKEN_DATE);

        refreshAll();
        setLastSync();

        //Log data which should be empty except for un-synced records
        db.log();
        mPrefs.log();
    }

    private void onChooseAccount() {
        //Find current account
        AccountManager accountManager = AccountManager.get(this);
        Account[] accounts = accountManager.getAccountsByType("com.google");
        String accountName = mPrefs.getString(Prefs.KEY_ACCOUNT_NAME);
        Account account = null;
        for (Account tmpAccount : accounts) {
            if (tmpAccount.name.contentEquals(accountName))
                account = tmpAccount;
        }

        //Display account picker
        try {
            Intent intent = AccountPicker.newChooseAccountIntent(account, null, new String[]{"com.google"}, false, null, null, null, null);
            startActivityForResult(intent, PICK_ACCOUNT_REQUEST);
        } catch(Exception e) {
            Toast.makeText(this, "Google play services not available", Toast.LENGTH_LONG).show();
        }
    }

    private void onAddTaskList() {
        TaskListDialogFragment dialog = TaskListDialogFragment.newInstance(TaskListDialogFragment.TYPE_ADD, null);
        dialog.show(getFragmentManager(), "dialog");
    }


    public boolean onContextItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.complete || id == R.id.delete) {
            Task task = mTaskListAdapter.getItem(mTaskListAdapter.getPosition());
            Database db = Database.getInstance(this);

            if (id == R.id.complete)
                task.setCompleted(!task.completed);
            else
                task.setDeleted(!task.deleted);

            db.tasks.update(task);
            refreshTasks();
            return true;
        }

        return super.onContextItemSelected(item);
    }

    private void refreshAll() {
        refreshLists();
        refreshTasks();
    }

    private void refreshLists() {
        Log.d(TAG, "refreshLists");
        setLastSync(); //Relative time so update it as much as possible
        Database db = Database.getInstance(this);
        ArrayList<TaskList> dbLists = db.taskLists.getList();
        if (dbLists.size() == 0) {
            Log.d(TAG, "No lists, adding default");
            TaskList defaultList = new TaskList("Default");
            defaultList.bDefault = true;
            db.taskLists.add(defaultList);
            dbLists = db.taskLists.getList(); //re-get list
        }

        //If the current list is not set, try to restore last saved
        if (mCurrList == null)
            mCurrList = TaskList.get(dbLists, mPrefs.getString(Prefs.KEY_LAST_SELECTED_LIST_ID));

        //If nothing valid is saved default to "all tasks" list
        if (mCurrList == null)
            mCurrList = TaskList.ALL_TASKS;

        mTaskListsToolbar.refresh(dbLists);
    }

    private void refreshTasks() {
        Log.d(TAG, "refreshTasks");
        setLastSync(); //Relative time so update it as much as possible
        mTaskListAdapter.refresh(mCurrList);
    }


    @Override
    public TaskList getCurrentList() {
        return mCurrList;
    }

    @Override
    public void setCurrentList(TaskList list) {
        mCurrList = list;
    }

    @Override
    public void onListChanged() {
        refreshTasks();
    }
}
