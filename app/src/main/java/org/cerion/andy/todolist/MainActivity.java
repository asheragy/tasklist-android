package org.cerion.andy.todolist;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;


public class MainActivity extends ListActivity implements Sync.SyncCallback
{
    public static final String TAG = MainActivity.class.getSimpleName();

    public TextView mStatus;
    public Button mAuth;
    public Button mVerify;
    public Button mDownload;
    GestureDetector mGestureDetector;
    ActionBar mActionBar;

    public static ArrayList<TaskList> mTaskLists; //TODO, make accessable to TaskActivity without using public static, better object oriented way?
    public static ArrayAdapter<TaskList> mAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        //setTheme(Global.THEME);

        setContentView(R.layout.activity_main);

        mStatus = (TextView)findViewById(R.id.status);
        mAuth   = (Button)findViewById(R.id.auth);
        mVerify = (Button)findViewById(R.id.verify);
        mDownload=(Button)findViewById(R.id.download);

        registerForContextMenu(getListView());

/*
        mAuth.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                setAuthToken();
            }
        });
        */


        mVerify.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                VerifyAuthTask task = new VerifyAuthTask();
                task.execute();
            }
        });


        mDownload.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Sync.syncTaskLists(v.getContext(),MainActivity.this);
            }
        });

        //mToken = Prefs.getPref(this,Prefs.PREF_AUTHTOKEN);

        if(mTaskLists == null)
        {
            mTaskLists = new ArrayList<>();
            mTaskLists.add( new TaskList("","Default") );
        }

        mActionBar = getActionBar();


        mAdapter = new ArrayAdapter<TaskList>(mActionBar.getThemedContext(), android.R.layout.simple_spinner_dropdown_item, mTaskLists);
        ActionBar.OnNavigationListener navigationListener = new ActionBar.OnNavigationListener()
        {
            @Override
            public boolean onNavigationItemSelected(int itemPosition, long itemId)
            {
                TaskList list = mTaskLists.get(itemPosition);
                Log.d(TAG,list.title);

                loadTasks(list);
                return false;
            }
        };


        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        mActionBar.setListNavigationCallbacks(mAdapter, navigationListener);
        mActionBar.setSelectedNavigationItem(0);
        mActionBar.setDisplayShowTitleEnabled(false); //TODO set in styles otherwise title shows until code runs this line

        LinearLayout layout = (LinearLayout)findViewById(R.id.root);

        mGestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener()
        {
            public static final int MAJOR_MOVE = 50; //TODO change at runtime using DisplayMetrics() class

            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
            {
                //Log.d(TAG,"onFling");
                int dx = (int) (e2.getX() - e1.getX());
                if (Math.abs(dx) > MAJOR_MOVE && Math.abs(velocityX) > Math.abs(velocityY))
                {
                    int index = mActionBar.getSelectedNavigationIndex();

                    if (velocityX > 0)
                    {
                        Log.d(TAG,"onPrevious");
                        index--;
                    }
                    else
                    {
                        Log.d(TAG,"onNext");
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

        getListView().setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                Task task = (Task)getListView().getItemAtPosition(position);
                onOpenTask(task);
            }
        });


        loadTaskLists();
    }

    public void onOpenTask(Task task)
    {
        TaskActivity.mTask = task;
        Intent intent = new Intent(this, TaskActivity.class);
        intent.putExtra(TaskActivity.EXTRA_TASK_ID,task.id);
        startActivity(intent);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        boolean bResult = super.onTouchEvent(event);
        if (!bResult && mGestureDetector != null)
            bResult = mGestureDetector.onTouchEvent(event);
        return bResult;
    }

    @Override
    public void onSyncComplete()
    {
        refreshList();
    }

    private class VerifyAuthTask extends AsyncTask<Void, Void, Void>
    {
        private boolean mValid = false;
        @Override
        protected Void doInBackground(Void... params)
        {
            //mValid = TasksAPI.verifyAuth();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid)
        {
            if(mValid)
                mStatus.setText("Auth Success");
            else
                mStatus.setText("Auth Failed");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings)
        {
            Intent i = new Intent(this, SettingsActivity.class);
            startActivityForResult(i, RESULT_OK);
            return true;
        }
        else if(id == R.id.action_add)
        {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            final EditText edittext= new EditText(this);
            final Context context = this;

            alert.setTitle("New Task List");
            alert.setMessage("Enter name");
            alert.setView(edittext);

            alert.setPositiveButton("Save", new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int whichButton)
                {
                    String name = edittext.getText().toString();

                    Database db = Database.getInstance(context);
                    TaskList update = new TaskList(TaskList.generateId(),name);
                    db.addTaskList(update);

                    loadTaskLists(); //reload from database
                }
            });

            alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int whichButton)
                {
                    Log.d(TAG,"Cancel Add");
                }
            });

            alert.show();
        }
        else if(id == R.id.action_rename)
        {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            final EditText edittext= new EditText(this);
            final TaskList taskList = getCurrentList();
            final Context context = this;

            alert.setTitle(taskList.title);
            alert.setMessage("Enter new name");
            alert.setView(edittext);

            alert.setPositiveButton("Save", new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int whichButton)
                {
                    //What ever you want to do with the value
                    String newName = edittext.getText().toString();
                    Log.d(TAG,"Rename " + taskList.title + " to " + newName);

                    //TODO, move to database?
                    TaskList update = new TaskList(taskList.id,newName);
                    update.setRenamed(true);
                    Database db = Database.getInstance(context);
                    db.updateTaskList(update);

                    loadTaskLists(); //reload from database
                }
            });

            alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int whichButton)
                {
                    Log.d(TAG,"Cancel Rename");
                }
            });

            alert.show();
        }

        return super.onOptionsItemSelected(item);
    }

    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo)
    {
        if (v.getId()==android.R.id.list)
        {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.list_context_menu, menu);
        }
    }

    public boolean onContextItemSelected(MenuItem item)
    {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        Task task = (Task)getListAdapter().getItem(info.position);
        Database db = null;

        ArrayAdapter<Task> adapter = (ArrayAdapter)getListAdapter();

        switch (item.getItemId())
        {
            case R.id.delete:
                Log.d(TAG,"onDelete: " + task.title);
                db = Database.getInstance(this);
                task.setDeleted(true);
                db.updateTask(task);
                refreshList();
                return true;
            case R.id.undelete:
                Log.d(TAG,"onUnDelete: " + task.title);
                db = Database.getInstance(this);
                task.setDeleted(false);
                db.updateTask(task);
                refreshList();
                return true;

            case R.id.modify:
                Log.d(TAG,"onModify: " + task.title);
                db = Database.getInstance(this);
                task.title += "x";
                task.setModified();
                db.updateTask(task);
                refreshList();
                return true;

            default:
                return super.onContextItemSelected(item);
        }
    }

    public TaskList getCurrentList()
    {
        return mAdapter.getItem(mActionBar.getSelectedNavigationIndex() );
    }

    public void loadTaskLists()
    {
        Database db = Database.getInstance(this);
        ArrayList<TaskList> dbLists = db.getTaskLists();

        mTaskLists.clear();
        mTaskLists.add( new TaskList(null,"All Tasks")); //null is placeholder for "all lists"
        for(TaskList list : dbLists)
            mTaskLists.add(list);

        mAdapter.notifyDataSetChanged();

        //TODO save current list ID so it will be selected on reload
    }

    public void refreshList()
    {
        int index = mActionBar.getSelectedNavigationIndex();
        TaskList list = mTaskLists.get(index);
        loadTasks(list);
    }

    public void loadTasks(TaskList list)
    {
        Database db = Database.getInstance(this);
        ArrayList<Task> tasks = db.getTasks(list.id);

        TaskListAdapter myAdapter = new TaskListAdapter(this,R.layout.list_row, tasks);
        ///ArrayAdapter<Task> myAdapter = new ArrayAdapter <>(this, R.layout.list_row, R.id.textView, tasks);
        setListAdapter(myAdapter);
    }
}
