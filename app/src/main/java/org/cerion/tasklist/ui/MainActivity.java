package org.cerion.tasklist.ui;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.support.v7.app.ActionBarActivity;

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

public class MainActivity extends ActionBarActivity
        implements TaskListDialogListener, AdapterView.OnItemClickListener
{
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int EDIT_TASK_REQUEST = 0;
    private static final int PICK_ACCOUNT_REQUEST = 1;

    private TextView mStatus;
    private ProgressBar mProgressBar;
    private SwipeRefreshLayout mSwipeRefresh;
    //private GestureDetector mGestureDetector;
    private ActionBar mActionBar;
    private ArrayList<TaskList> mTaskLists;
    private ArrayAdapter<TaskList> mActionBarAdapter;


    private static TaskList mCurrList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate " + (savedInstanceState == null ? "null" : "saveState"));
        setContentView(R.layout.activity_main);

        findViewById(R.id.layoutDebug).setVisibility(View.GONE);

        mActionBar = getSupportActionBar();
        mActionBar.setDisplayShowTitleEnabled(false); //Hide app name, task lists replace title on actionbar

        mStatus = (TextView) findViewById(R.id.status);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mProgressBar.setVisibility(View.INVISIBLE);
        getListView().setEmptyView(findViewById(android.R.id.empty));
        getListView().setOnItemClickListener(this);
        registerForContextMenu(getListView());
        mSwipeRefresh = (SwipeRefreshLayout)findViewById(R.id.swipeRefresh);

        mSwipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                onSync();
            }
        });

        /*
        findViewById(R.id.syncImage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSync();
            }
        });
        */

        findViewById(R.id.logdb).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Database db = Database.getInstance(MainActivity.this);
                db.log();
                Prefs.logPrefs(MainActivity.this);
            }
        });

        setLastSync();
        refreshLists();


        /* Not using for now, maybe switch to tab list or navigation drawer
        LinearLayout layout = (LinearLayout) findViewById(R.id.root);
        mGestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            public static final int MAJOR_MOVE = 50; to do change at runtime using DisplayMetrics() class

            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                //Log.d(TAG,"onFling");
                int dx = (int) (e2.getX() - e1.getX());
                if (Math.abs(dx) > MAJOR_MOVE && Math.abs(velocityX) > Math.abs(velocityY)) {
                    int index = mActionBar.getSelectedNavigationIndex();

                    if (velocityX > 0) {
                        Log.d(TAG, "onPrevious");
                        index--;
                    } else {
                        Log.d(TAG, "onNext");
                        index++;
                    }

                    index = (index + mActionBar.getNavigationItemCount()) % mActionBar.getNavigationItemCount();
                    mActionBar.setSelectedNavigationItem(index);

                    return true;
                } else {
                    return false;
                }
            }
        });

        layout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(final View view, final MotionEvent event) {
                mGestureDetector.onTouchEvent(event);
                return true;
            }
        });
        */



    }

    @Override
    public void onBackPressed()
    {
        //moveTaskToBack(true);
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
        Prefs.savePref(this,Prefs.KEY_LAST_SELECTED_LIST_ID,mCurrList.id);
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

    //----- List Activity functions
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Task task = (Task) getListView().getItemAtPosition(position);
        onOpenTask(task);
    }

    private ListView getListView() {
        return (ListView) findViewById(android.R.id.list);
    }

    private Adapter getListAdapter() {
        return getListView().getAdapter();
    }

    private void setListAdapter(ListAdapter adapter) {
        getListView().setAdapter(adapter);
    }

    //----- END List Activity functions

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
                    } else if (e instanceof android.accounts.OperationCanceledException) {
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
            if(mSwipeRefresh.isRefreshing())
                mSwipeRefresh.setRefreshing(false);
        }
    }

    private void setLastSync() {
        String sText = "Last Sync: ";
        Date lastSync = Prefs.getPrefDate(this, Prefs.KEY_LAST_SYNC);
        if (lastSync == null || lastSync.getTime() == 0)
            sText += "Never";
        else {
            long now = new Date().getTime();
            sText += DateUtils.getRelativeTimeSpanString(lastSync.getTime(), now, DateUtils.SECOND_IN_MILLIS).toString();
        }

        mStatus.setText(sText);
    }

    private void setInSync(boolean bSyncing) {
        //Stop using progress bar for now, swipe refresh has its own update
        //mProgressBar.setVisibility(bSyncing ? View.VISIBLE : View.INVISIBLE);
        getListView().setVisibility(bSyncing ? View.INVISIBLE : View.VISIBLE);
        //findViewById(R.id.syncImage).setVisibility(bSyncing ? View.INVISIBLE : View.VISIBLE);

        if(!bSyncing && mSwipeRefresh.isRefreshing())
            mSwipeRefresh.setRefreshing(false);
    }

    private void onOpenTask(Task task) {
        Intent intent = new Intent(this, TaskActivity.class);
        if(task != null)
            intent.putExtra(TaskActivity.EXTRA_TASK, task);

        TaskList defaultList = TaskList.getDefault(mTaskLists);

        intent.putExtra(TaskActivity.EXTRA_DEFAULT_LIST, defaultList);
        intent.putExtra(TaskActivity.EXTRA_TASKLIST, mCurrList);
        startActivityForResult(intent, EDIT_TASK_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult: " + resultCode);

        if(resultCode == RESULT_OK) {
            if (requestCode == EDIT_TASK_REQUEST)
                refreshTasks();
            else if (requestCode == PICK_ACCOUNT_REQUEST) {
                String currentAccount = Prefs.getPref(this,Prefs.KEY_ACCOUNT_NAME);
                String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);

                //If current account is set and different than selected account, logout first
                if(currentAccount.length() > 0 && !currentAccount.contentEquals(accountName))
                    onLogout();

                Prefs.savePref(this,Prefs.KEY_ACCOUNT_NAME,accountName);
            }
        }
    }

    /*
    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        boolean bResult = super.onTouchEvent(event);
        if (!bResult && mGestureDetector != null)
            bResult = mGestureDetector.onTouchEvent(event);
        return bResult;
    }
    */

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
        menu.findItem(R.id.action_rename).setVisible( !mCurrList.isAllTasks() ); //Hide rename if "All Tasks" list

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if(id == R.id.action_add) {
            onAddTaskList();
        }
        else if(id == R.id.action_rename) {
            TaskListDialogFragment dialog = TaskListDialogFragment.newInstance(TaskListDialogFragment.TYPE_RENAME,mCurrList);
            dialog.show(getFragmentManager(), "dialog");
        }
        else if(id == R.id.action_add_task) {
            onOpenTask(null);
        }
        else if(id == R.id.action_account) {
            onChooseAccount();
        }
        else if(id == R.id.action_logout) {
            onLogout();
        }

        return super.onOptionsItemSelected(item);
    }

    private void onLogout() {
        Log.d(TAG,"onLogout");
        Database db = Database.getInstance(MainActivity.this);

        //Move un-synced task to this default list
        TaskList defaultList = TaskList.getDefault(mTaskLists);

        //Delete all non-temp Id records, also remove records marked as deleted
        ArrayList<Task> tasks = db.tasks.getList(null);
        for(Task task : tasks)
        {
            if(!task.hasTempId() || task.deleted)
                db.tasks.delete(task);
            else {
                //Since we are also removing synced lists, check if we need to move this task to an un-synced list
                TaskList list = new TaskList(task.listId,"");
                if(!list.hasTempId() && defaultList != null) {
                    //Move this task to default list
                    db.setTaskIds(task,task.id,defaultList.id);
                }
            }
        }

        ArrayList<TaskList> lists = db.taskLists.getList();
        for(TaskList list : lists)
        {
            if(!list.hasTempId()) //don't delete un-synced lists
            {
                if(list.bDefault) { //Keep default but assign temp id
                    db.taskLists.setLastUpdated(list, new Date(0));
                    db.taskLists.setId(list, TaskList.generateId());
                }
                else
                    db.taskLists.delete(list);
            }
        }

        //Remove prefs related to sync/account
        Prefs.remove(this,Prefs.KEY_LAST_SYNC);
        Prefs.remove(this,Prefs.KEY_ACCOUNT_NAME);
        Prefs.remove(this,Prefs.KEY_AUTHTOKEN);
        Prefs.remove(this,Prefs.KEY_AUTHTOKEN_DATE);

        refreshAll();
        setLastSync();

        //Log data which should be empty except for un-synced records
        db.log();
        Prefs.logPrefs(MainActivity.this);
    }

    private void onChooseAccount() {
        //Find current account
        AccountManager accountManager = AccountManager.get(this);
        Account[] accounts = accountManager.getAccountsByType("com.google");
        String accountName = Prefs.getPref(this,Prefs.KEY_ACCOUNT_NAME);
        Account account = null;
        for(Account tmpAccount: accounts) {
            if(tmpAccount.name.contentEquals(accountName))
                account = tmpAccount;
        }

        //Display account picker
        Intent intent = AccountPicker.newChooseAccountIntent(account, null, new String[]{"com.google"}, false, null, null, null, null);
        startActivityForResult(intent, PICK_ACCOUNT_REQUEST);
    }

    private void onAddTaskList() {
        TaskListDialogFragment dialog = TaskListDialogFragment.newInstance(TaskListDialogFragment.TYPE_ADD,null);
        dialog.show(getFragmentManager(), "dialog");
    }

    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if (v.getId()==android.R.id.list) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.main_context, menu);

            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
            Task task = (Task) getListView().getItemAtPosition(info.position);

            if(task.completed) {
                MenuItem item = menu.findItem(R.id.complete);
                item.setTitle("Un-Complete");
            }
            if(task.deleted){
                MenuItem item = menu.findItem(R.id.delete);
                item.setTitle("Un-Delete");
            }
        }
    }

    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        int id = item.getItemId();

        if(id == R.id.complete || id == R.id.delete) {
            Task task = (Task) getListAdapter().getItem(info.position);
            Database db = Database.getInstance(this);

            if(id == R.id.complete)
                task.setCompleted(!task.completed);
            else if(id == R.id.delete)
                task.setDeleted(!task.deleted);

            db.tasks.update(task);
            refreshTasks();
            return true;
        }

        return super.onContextItemSelected(item);
    }

    private void refreshAll()
    {
        refreshLists();
        refreshTasks();
    }

    private void refreshLists()
    {
        Log.d(TAG, "refreshLists");
        setLastSync(); //Relative time so update it as much as possible
        Database db = Database.getInstance(this);
        ArrayList<TaskList> dbLists = db.taskLists.getList();
        if(dbLists.size() == 0)
        {
            Log.d(TAG,"No lists, adding default");
            TaskList defaultList = new TaskList("Default");
            defaultList.bDefault = true;
            db.taskLists.add(defaultList);
            dbLists = db.taskLists.getList(); //re-get list
        }

        if (mTaskLists == null)
            mTaskLists = new ArrayList<>();
        else
            mTaskLists.clear();

        //If the current list is not set, try to restore last saved
        if(mCurrList == null)
            mCurrList = TaskList.get(dbLists,Prefs.getPref(this,Prefs.KEY_LAST_SELECTED_LIST_ID));

        //If nothing valid is saved default to "all tasks" list
        if(mCurrList == null)
            mCurrList = TaskList.ALL_TASKS;

        mTaskLists.add(TaskList.ALL_TASKS);
        mTaskLists.addAll(dbLists);

        if(mActionBarAdapter == null) {
            mActionBarAdapter = new ArrayAdapter<>(mActionBar.getThemedContext(), android.R.layout.simple_spinner_dropdown_item, mTaskLists);
            ActionBar.OnNavigationListener navigationListener = new ActionBar.OnNavigationListener() {
                @Override
                public boolean onNavigationItemSelected(int itemPosition, long itemId) {
                    Log.d(TAG,"onNavigationItemSelected: " + itemPosition + " index = " + mActionBar.getSelectedNavigationIndex() );
                    mCurrList = mTaskLists.get(itemPosition);
                    refreshTasks();

                    return false;
                }
            };

            mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
            mActionBar.setListNavigationCallbacks(mActionBarAdapter, navigationListener);
        }
        else
            mActionBarAdapter.notifyDataSetChanged();

        mActionBar.setSelectedNavigationItem(getListPosition(mCurrList));
    }

    private void refreshTasks()
    {
        Log.d(TAG,"refreshTasks");
        setLastSync(); //Relative time so update it as much as possible
        Database db = Database.getInstance(this);
        ArrayList<Task> tasks = db.tasks.getList(mCurrList.id,true); //Get list with blank records excluded

        if(getListAdapter() == null) {
            TaskListAdapter myAdapter = new TaskListAdapter(this, tasks);
            setListAdapter(myAdapter);
        }
        else {
            ((TaskListAdapter)getListAdapter()).refresh(tasks);
        }

    }

    private int getListPosition(TaskList list)
    {
        String id = list.id;
        int index = 0;
        if(id != null) {
            for (int i = 1; i < mActionBarAdapter.getCount(); i++) { //Skip first since its default
                TaskList curr = mActionBarAdapter.getItem(i);
                if (curr.id.contentEquals(id))
                    index = i;
            }
        }

        Log.d(TAG,"listPosition = " + index);
        return index;
    }

}
