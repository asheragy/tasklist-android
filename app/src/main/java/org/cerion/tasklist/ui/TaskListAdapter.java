package org.cerion.tasklist.ui;

import android.content.res.Resources;
import android.graphics.Paint;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.cerion.tasklist.R;
import org.cerion.tasklist.data.Task;

import java.util.ArrayList;
import java.util.List;

class TaskListAdapter extends RecyclerView.Adapter<TaskListAdapter.ViewHolder> {

    private final List<Task> mTasks = new ArrayList<>();
    private int mPrimaryColor;
    private int mSecondaryColor;
    private final MainActivity mActivity;

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

    public void refresh(List<Task> tasks) {
        mTasks.clear();
        mTasks.addAll(tasks);
        //mTasks = tasks;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.task_list_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Task task = mTasks.get(position);

        String sTitle = task.title.length() > 0 ? task.title : "<Blank>";
        if(task.deleted)
            sTitle = "*DELETED* " + sTitle;
        holder.title.setText(sTitle);
        holder.notes.setText(task.notes);

        if(task.completed) {
            holder.title.setTextColor(mSecondaryColor);
            holder.title.setPaintFlags(holder.title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        }
        else {
            holder.title.setTextColor(mPrimaryColor);
            holder.title.setPaintFlags(holder.title.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
        }

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


    protected class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnCreateContextMenuListener {
        TextView title;
        TextView due;
        TextView notes;

        public ViewHolder(View view) {
            super(view);
            title = (TextView)view.findViewById(R.id.title);
            due = (TextView)view.findViewById(R.id.dueDate);
            notes = (TextView)view.findViewById(R.id.notes);

            view.setOnClickListener(this);
            view.setOnCreateContextMenuListener(this);

        }

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


    }

}
