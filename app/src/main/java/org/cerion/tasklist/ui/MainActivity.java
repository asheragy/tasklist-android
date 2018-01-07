package org.cerion.tasklist.ui;

import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.databinding.Observable;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import org.cerion.tasklist.R;
import org.cerion.tasklist.data.Database;
import org.cerion.tasklist.data.Prefs;
import org.cerion.tasklist.data.Task;
import org.cerion.tasklist.data.TaskList;
import org.cerion.tasklist.databinding.ActivityMainBinding;
import org.cerion.tasklist.dialogs.AlertDialogFragment;
import org.cerion.tasklist.dialogs.MoveTaskDialogFragment;
import org.cerion.tasklist.dialogs.TaskListDialogFragment;
import org.cerion.tasklist.dialogs.TaskListsChangedListener;
import org.cerion.tasklist.sync.OnSyncCompleteListener;
import org.cerion.tasklist.sync.Sync;

import java.util.List;

//TODO verify network is available and toast message

public class MainActivity extends Activity implements TaskListsChangedListener, TaskListsToolbar.TaskListsChangeListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int EDIT_TASK_REQUEST = 0;

    private SwipeRefreshLayout mSwipeRefresh;
    private Prefs mPrefs;
    private TaskListsToolbar mTaskListsToolbar;
    private final TaskListAdapter mTaskListAdapter = new TaskListAdapter(this);
    private TasksViewModel vm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate " + (savedInstanceState == null ? "null" : "saveState"));
        mPrefs = Prefs.getInstance(this);
        if (mPrefs.isDarkTheme())
            setTheme(R.style.AppTheme_Dark);

        super.onCreate(savedInstanceState);
        vm = new TasksViewModel(this);

        final ActivityMainBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        binding.layoutDebug.setVisibility(View.GONE);

        binding.setViewModel(vm);

        final int defaultTextColor = binding.status.getTextColors().getDefaultColor();
        vm.isOutOfSync.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable observable, int i) {
                if(vm.isOutOfSync.get())
                    binding.status.setTextColor(Color.RED);
                else
                    binding.status.setTextColor(defaultTextColor);
            }
        });

        //Toolbar
        mTaskListsToolbar = findViewById(R.id.toolbar);
        setActionBar(mTaskListsToolbar);
        if(getActionBar() != null)
            getActionBar().setDisplayShowTitleEnabled(false); //Hide app name, task lists replace title on actionbar
        mTaskListsToolbar.setViewModel(vm);

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(mTaskListAdapter);
        mTaskListAdapter.setEmptyView(binding.recyclerView, findViewById(R.id.empty_view));

        binding.recyclerView.setOnTouchListener(new OnSwipeTouchListener(this) {
            @Override
            public void onSwipeLeft() {
                mTaskListsToolbar.moveLeft();
            }

            @Override
            public void onSwipeRight() {
                mTaskListsToolbar.moveRight();
            }
        });

        mSwipeRefresh = findViewById(R.id.swipeRefresh);
        mSwipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                onSync();
            }
        });

        FloatingActionButton fab = findViewById(R.id.fab);
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

        vm.updateLastSync();
        refreshLists();
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    protected void onResume() {
        //Log.d(TAG,"onResume");
        vm.updateLastSync();
        super.onResume();
    }

    @Override
    protected void onPause() {
        //Log.d(TAG,"onPause");
        mPrefs.setString(Prefs.KEY_LAST_SELECTED_LIST_ID, vm.currList.get().id);
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
                        //TODO do automatically
                        Toast.makeText(MainActivity.this, "Open settings and select account", Toast.LENGTH_LONG).show();
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
                        vm.updateLastSync(); //Update last sync time only if successful
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
            /*
            else if (requestCode == PICK_ACCOUNT_REQUEST) {
                String currentAccount = mPrefs.getString(Prefs.KEY_ACCOUNT_NAME);
                String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);

                //If current account is set and different than selected account, logout first
                if (currentAccount.length() > 0 && !currentAccount.contentEquals(accountName))
                    AuthTools.logout(this);

                mPrefs.setString(Prefs.KEY_ACCOUNT_NAME, accountName);
            }
            */
        }
    }

    public void onOpenTask(Task task) {
        Intent intent = new Intent(this, TaskActivity.class);

        //Send with task or tasklist parameter if new
        if (task != null)
            intent.putExtra(TaskActivity.EXTRA_TASK, task);
        else {
            TaskList list = vm.currList.get();
            if(list.isAllTasks())
                list = mTaskListsToolbar.getDefaultList();
            intent.putExtra(TaskActivity.EXTRA_TASKLIST, list);
        }

        startActivityForResult(intent, EDIT_TASK_REQUEST);
    }

    @Override
    public void onTaskListsChanged(TaskList current) {
        vm.currList.set(current); //This list was added or updated
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
        menu.findItem(R.id.action_rename).setVisible(!vm.currList.get().isAllTasks()); //Hide rename if "All Tasks" list
        menu.findItem(R.id.action_delete).setVisible(mTaskListAdapter.getItemCount() == 0);

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
            case R.id.action_settings: {
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                break;
            }
            case R.id.action_clear_completed: onClearCompleted(); break;
            case R.id.action_rename:
                TaskListDialogFragment dialog = TaskListDialogFragment.newInstance(TaskListDialogFragment.TYPE_RENAME, vm.currList.get());
                dialog.show(getFragmentManager(), "dialog");
                break;
            case R.id.action_delete:
                Toast.makeText(this, "not implemented", Toast.LENGTH_SHORT).show();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void onClearCompleted() {
        Log.d(TAG,"onClearCompleted");
        Database db = Database.getInstance(this);
        db.tasks.clearCompleted(vm.currList.get());

        refreshTasks();
    }

    private void onAddTaskList() {
        TaskListDialogFragment dialog = TaskListDialogFragment.newInstance(TaskListDialogFragment.TYPE_ADD, null);
        dialog.show(getFragmentManager(), "dialog");
    }

    public boolean onContextItemSelected(MenuItem item) {
        int id = item.getItemId();
        Task task = mTaskListAdapter.getItem(mTaskListAdapter.getPosition());
        Database db = Database.getInstance(this);

        if (id == R.id.complete || id == R.id.delete) {
            if (id == R.id.complete)
                task.setCompleted(!task.completed);
            else
                task.setDeleted(!task.deleted);

            db.tasks.update(task);
            refreshTasks();
            return true;
        }

        if(id == R.id.move) {
            Log.d(TAG,"onMove");

            DialogFragment newFragment = MoveTaskDialogFragment.newInstance(task);
            newFragment.show(getFragmentManager(), "moveTask");

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
        vm.updateLastSync(); //Relative time so update it as much as possible
        Database db = Database.getInstance(this);
        List<TaskList> dbLists = db.taskLists.getList();
        if (dbLists.size() == 0) {
            Log.d(TAG, "No lists, adding default");
            TaskList defaultList = new TaskList("Default");
            defaultList.bDefault = true;
            db.taskLists.add(defaultList);
            dbLists = db.taskLists.getList(); //re-get list
        }

        //If the current list is not set, try to restore last saved
        if (vm.currList.get() == null)
            vm.currList.set( TaskList.get(dbLists, mPrefs.getString(Prefs.KEY_LAST_SELECTED_LIST_ID)) );

        //If nothing valid is saved default to "all tasks" list
        if (vm.currList.get() == null)
            vm.currList.set( TaskList.ALL_TASKS );

        mTaskListsToolbar.refresh(dbLists);
    }

    private void refreshTasks() {
        Log.d(TAG, "refreshTasks");
        vm.updateLastSync(); //Relative time so update it as much as possible
        mTaskListAdapter.refresh(vm.currList.get());
    }

    @Override
    public void onListChanged() {
        refreshTasks();
    }
}
