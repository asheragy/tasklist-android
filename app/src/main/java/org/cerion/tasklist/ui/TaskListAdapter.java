package org.cerion.tasklist.ui;

import android.content.res.Resources;
import android.graphics.Paint;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.TextView;

import org.cerion.tasklist.R;
import org.cerion.tasklist.data.Database;
import org.cerion.tasklist.data.Task;
import org.cerion.tasklist.data.TaskList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

class TaskListAdapter extends RecyclerView.Adapter<TaskListAdapter.ViewHolder> {

    private static final String TAG = TaskListAdapter.class.getSimpleName();

    private final List<Task> mTasks = new ArrayList<>();
    private int mPrimaryColor;
    private int mSecondaryColor;
    private final MainActivity mActivity;
    private TaskList mCurrList;

    public TaskListAdapter(MainActivity activity) {
        mActivity = activity;

    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);

        //Programmatically set color based on completion, need to know current theme
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = mActivity.getTheme();
        theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
        mPrimaryColor = ContextCompat.getColor(mActivity, typedValue.resourceId);
        theme.resolveAttribute(android.R.attr.textColorSecondary, typedValue, true);
        mSecondaryColor = ContextCompat.getColor(mActivity, typedValue.resourceId);
    }

    public void refresh(TaskList list) {
        mCurrList = list;
        Database db = Database.getInstance(mActivity);
        List<Task> tasks = db.tasks.getList(list.id, false); //Get list with blank records excluded

        mTasks.clear();
        mTasks.addAll(tasks);

        Collections.sort(mTasks, new Comparator<Task>() {
            @Override
            public int compare(Task task, Task t1) {
                if(task.deleted != t1.deleted)
                    return task.deleted ? 1 : -1;
                if(task.completed != t1.completed)
                    return task.completed ? 1 : -1;

                return task.title.compareToIgnoreCase(t1.title);
            }
        });

        notifyDataSetChanged();
    }

    private void refresh() {
        refresh(mCurrList);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_task, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Task task = mTasks.get(position);

        String sTitle = task.title.length() > 0 ? task.title : "<Blank>";
        holder.title.setText(sTitle);
        holder.notes.setText(task.notes);
        holder.completed.setChecked(task.completed);

        if(task.completed) {
            holder.title.setTextColor(mSecondaryColor);
            holder.title.setPaintFlags(holder.title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

        }
        else {
            holder.title.setTextColor(task.deleted ? mSecondaryColor : mPrimaryColor);
            holder.title.setPaintFlags(holder.title.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
        }


        //If deleted don't show completed checkbox
        holder.completed.setVisibility(task.deleted ? View.GONE : View.VISIBLE);
        holder.undelete.setVisibility(task.deleted ? View.VISIBLE : View.GONE);

        if(task.due != null && task.due.getTime() != 0)
            holder.due.setText(task.getDue());
        else
            holder.due.setText("");
    }

    @Override
    public int getItemCount() {
        return mTasks.size();
    }

    public Task getItem(int position) {
        return mTasks.get(position);
    }

    //Workaround for activity to get context menu position
    private int position;
    public int getPosition() {
        return position;
    }


    protected class ViewHolder extends RecyclerView.ViewHolder implements OnClickListener, View.OnCreateContextMenuListener, OnCheckedChangeListener {
        final TextView title;
        final TextView due;
        final TextView notes;
        final CheckBox completed;
        final ImageButton undelete;

        public ViewHolder(View view) {
            super(view);
            title = (TextView)view.findViewById(R.id.title);
            due = (TextView)view.findViewById(R.id.dueDate);
            notes = (TextView)view.findViewById(R.id.notes);
            completed = (CheckBox)view.findViewById(R.id.completed);
            undelete = (ImageButton)view.findViewById(R.id.undelete);

            view.setOnClickListener(this);
            view.setOnCreateContextMenuListener(this);
            completed.setOnCheckedChangeListener(this);
            undelete.setOnClickListener(mOnUnDeleteListener);
        }

        private final OnClickListener mOnUnDeleteListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                Task task = mTasks.get(getLayoutPosition());

                Log.d(TAG,"onUnDelete");
                Database db = Database.getInstance(v.getContext());
                task.setDeleted(false);
                db.tasks.update(task);
                refresh();

            }
        };

        @Override
        public void onClick(View v) {
            Task task = mTasks.get(getLayoutPosition());
            mActivity.onOpenTask(task);
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {

            MenuInflater inflater = mActivity.getMenuInflater();
            inflater.inflate(R.menu.main_context, menu);

            position = getLayoutPosition();
            Task task = mTasks.get(position);
            if (task.completed) {
                MenuItem item = menu.findItem(R.id.complete);
                item.setTitle("Un-Complete");
            }
            if (task.deleted) {
                MenuItem item = menu.findItem(R.id.delete);
                item.setTitle("Un-Delete");
            }
        }

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            Task task = mTasks.get(getLayoutPosition());
            if(task.completed != isChecked) { //checkbox was manually changed

                //Update record in database and refresh list
                Log.d(TAG,"Toggle completed checkbox");
                Database db = Database.getInstance(buttonView.getContext());
                task.setCompleted(isChecked);
                db.tasks.update(task);
                refresh();
            }
        }
    }

}
