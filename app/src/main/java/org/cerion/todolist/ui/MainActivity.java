package org.cerion.todolist.ui;


import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
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

import org.cerion.todolist.data.Database;
import org.cerion.todolist.data.Prefs;
import org.cerion.todolist.R;
import org.cerion.todolist.data.Sync;
import org.cerion.todolist.data.Task;
import org.cerion.todolist.data.TaskList;
import org.cerion.todolist.dialogs.AlertDialogFragment;
import org.cerion.todolist.dialogs.TaskListDialogFragment;
import org.cerion.todolist.dialogs.TaskListDialogFragment.TaskListDialogListener;


import java.util.ArrayList;
import java.util.Date;

/* Checklist

All fields both ways Tasks

Task add delete update WEB
Task add delete update Device
List add delete update WEB
List add delete update Device

conflict delete/update both directions
conflict delete both

Add list on web with tasks (should be fine)
Add list on db with tasks then sync (ids should all be correct)
Add list on db with tasks then delete task and sync (no leftover deletions)
delete list on Web, all tasks should get deleted

*/

public class MainActivity extends ActionBarActivity implements TaskListDialogListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    //private static final String STATE_NAV_INDEX = "stateNavIndex";

    private static final int EDIT_TASK_REQUEST = 0;
    private static final int PICK_ACCOUNT_REQUEST = 1;

    private static final String NEW_LISTID = "new";
    private TextView mStatus;
    private ProgressBar mProgressBar;
    //private GestureDetector mGestureDetector;
    private ActionBar mActionBar;
    private ArrayList<TaskList> mTaskLists;
    private ArrayAdapter<TaskList> mActionBarAdapter;

    private static String mCurrListId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG,"onCreate " + (savedInstanceState == null ? "null" : "saveState"));
        setContentView(R.layout.activity_main);

        mActionBar = getSupportActionBar();
        mStatus = (TextView) findViewById(R.id.status);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mProgressBar.setVisibility(View.INVISIBLE);
        getListView().setEmptyView(findViewById(android.R.id.empty));
        registerForContextMenu(getListView());

        findViewById(R.id.syncImage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSync();
            }
        });

        findViewById(R.id.logdb).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Database db = Database.getInstance(MainActivity.this);
                db.log();
            }
        });


        getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Task task = (Task) getListView().getItemAtPosition(position);
                onOpenTask(task);
            }
        });


        updateLastSync();
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

    public ListView getListView() {
        return (ListView) findViewById(android.R.id.list);
    }

    public Adapter getListAdapter() {
        return getListView().getAdapter();
    }

    public void setListAdapter(ListAdapter adapter) {
        getListView().setAdapter(adapter);
    }

    public void onSync() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        boolean isConnected = networkInfo != null && networkInfo.isConnected();

        if (isConnected) {
            setInSync(true);
            Sync.syncTaskLists(this, new Sync.Callback() {
                @Override
                public void onAuthError(int result, Exception e) {
                    setInSync(false);

                    if(result == Sync.RESULT_NO_GOOGLE_ACCOUNT) {
                        onChooseAccount(); //select or add google account
                    }
                    else {
                        DialogFragment dialog = AlertDialogFragment.newInstance("Error", "Auth Error");
                        dialog.show(getFragmentManager(), "dialog");
                    }
                }

                @Override
                //TODO, remove data changed paraemter and just always refresh
                public void onSyncFinish(int result, boolean bDataChanged, Exception e) {
                    setInSync(false);

                    if(result == Sync.RESULT_SUCCESS) {
                        updateLastSync(); //Update last sync time only if successful
                    }
                    else {
                        String message = "Sync Failed, unknown error";
                        if (e != null)
                            message = e.getMessage();

                        DialogFragment dialog = AlertDialogFragment.newInstance("Sync failed", message);
                        dialog.show(getFragmentManager(), "dialog");
                    }

                    if(bDataChanged)
                        refreshAll();
                }

            });
        } else {
            DialogFragment dialog = AlertDialogFragment.newInstance("Error", "Internet not available");
            dialog.show(getFragmentManager(), "dialog");
        }

    }

    public void updateLastSync() {
        String sText = "Last Sync: ";
        Date lastSync = Prefs.getPrefDate(this, Prefs.KEY_LAST_SYNC);
        if (lastSync == null || lastSync.getTime() == 0)
            sText += "Never";
        else
            sText += lastSync;

        mStatus.setText(sText);
    }

    public void setInSync(boolean bSyncing) {
        mProgressBar.setVisibility(bSyncing ? View.VISIBLE : View.INVISIBLE);
        getListView().setVisibility(bSyncing ? View.INVISIBLE : View.VISIBLE);
        findViewById(R.id.syncImage).setVisibility(bSyncing ? View.INVISIBLE : View.VISIBLE);
    }

    public void onOpenTask(Task task) {
        Intent intent = new Intent(this, TaskActivity.class);
        if(task != null)
            intent.putExtra(TaskActivity.EXTRA_TASK, task);
        intent.putExtra(TaskActivity.EXTRA_DEFAULT_LIST, getDefaultList());
        intent.putExtra(TaskActivity.EXTRA_TASKLIST, mTaskLists.get(mActionBar.getSelectedNavigationIndex()));
        startActivityForResult(intent, EDIT_TASK_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        Log.d(TAG, "onActivityResult: " + resultCode);

        if(resultCode == RESULT_OK) {
            if (requestCode == EDIT_TASK_REQUEST)
                refreshTasks();
            else if (requestCode == PICK_ACCOUNT_REQUEST) {
                String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
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
    public void onFinishTaskListDialog() {
        refreshLists();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
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
            TaskListDialogFragment dialog = TaskListDialogFragment.newInstance(TaskListDialogFragment.TYPE_RENAME,getCurrentList());
            dialog.show(getFragmentManager(), "dialog");
        }
        else if(id == R.id.action_add_task) {
            onOpenTask(null);
        }
        else if(id == R.id.action_account) {
            onChooseAccount();
        }

        return super.onOptionsItemSelected(item);
    }

    public void onChooseAccount()
    {
        Intent intent = AccountPicker.newChooseAccountIntent(null, null, new String[]{"com.google"}, false, null, null, null, null);
        startActivityForResult(intent, PICK_ACCOUNT_REQUEST);
    }

    public void onAddTaskList() {
        TaskListDialogFragment dialog = TaskListDialogFragment.newInstance(TaskListDialogFragment.TYPE_ADD,null);
        dialog.show(getFragmentManager(), "dialog");
    }

    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if (v.getId()==android.R.id.list) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.list_context_menu, menu);
        }
    }

    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        Task task = (Task)getListAdapter().getItem(info.position);

        Database db;

        switch (item.getItemId())
        {
            case R.id.delete:
                Log.d(TAG,"onDelete: " + task.title);
                db = Database.getInstance(this);
                task.setDeleted(true);
                db.updateTask(task);
                refreshTasks();
                return true;
            case R.id.undelete:
                Log.d(TAG,"onUnDelete: " + task.title);
                db = Database.getInstance(this);
                task.setDeleted(false);
                db.updateTask(task);
                refreshTasks();
                return true;

            case R.id.modify:
                Log.d(TAG,"onModify: " + task.title);
                db = Database.getInstance(this);
                task.title += "x";
                task.setModified();
                db.updateTask(task);
                refreshTasks();
                return true;

            default:
                return super.onContextItemSelected(item);
        }
    }

    public void refreshAll()
    {
        refreshLists();
        refreshTasks();
    }

    public void refreshLists()
    {
        Log.d(TAG, "refreshLists");
        Database db = Database.getInstance(this);
        ArrayList<TaskList> dbLists = db.taskLists.getList();

        if (mTaskLists == null)
            mTaskLists = new ArrayList<>();
        else
            mTaskLists.clear();

        mTaskLists.add(new TaskList(null, "All Tasks")); //null is placeholder for "all lists"
        for(TaskList list : dbLists)
            mTaskLists.add(list);
        mTaskLists.add(new TaskList(NEW_LISTID, "<Add List>"));

        if(mActionBarAdapter == null) {
            mActionBarAdapter = new ArrayAdapter<>(mActionBar.getThemedContext(), android.R.layout.simple_spinner_dropdown_item, mTaskLists);
            ActionBar.OnNavigationListener navigationListener = new ActionBar.OnNavigationListener() {
                @Override
                public boolean onNavigationItemSelected(int itemPosition, long itemId) {
                    Log.d(TAG,"position = " + itemPosition + " index = " + mActionBar.getSelectedNavigationIndex() );
                    if(itemPosition == mActionBar.getNavigationItemCount() - 1) {
                        onAddTaskList();
                        //Prefered action is to prevent current selection, not select the old one...
                        mActionBar.setSelectedNavigationItem( getListPosition(mCurrListId) );
                    }
                    else {
                        Log.d(TAG,"navigation listener, refreshing tasks");
                        mCurrListId = mTaskLists.get(itemPosition).id;
                        refreshTasks();
                    }

                    return false;
                }
            };

            mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
            mActionBar.setListNavigationCallbacks(mActionBarAdapter, navigationListener);
        }
        else
            mActionBarAdapter.notifyDataSetChanged();

        mActionBar.setSelectedNavigationItem(getListPosition(mCurrListId));
    }

    public void refreshTasks()
    {
        Log.d(TAG,"refreshTasks");
        Database db = Database.getInstance(this);
        ArrayList<Task> tasks = db.getTasks(mCurrListId);

        TaskListAdapter myAdapter = new TaskListAdapter(this,R.layout.row_list, tasks);
        setListAdapter(myAdapter);
    }

    private int getListPosition(String id)
    {
        int index = 0;
        if(id != null) {
            for (int i = 1; i < mActionBarAdapter.getCount() - 1; i++) { //Skip first and last list
                TaskList curr = mActionBarAdapter.getItem(i);
                if (curr.id.contentEquals(id))
                    index = i;
            }
        }

        return index;
    }

    //TODO, save curr/default as TaskList variables that are always valid, then we don't need these functions
    public TaskList getCurrentList()
    {
        return mActionBarAdapter.getItem(mActionBar.getSelectedNavigationIndex() );
    }

    private TaskList getDefaultList() {
        for(TaskList list : mTaskLists) {
            if(list.id == null)
                continue;
            if(list.bDefault)
                return list;
        }

        return null;
    }

}
