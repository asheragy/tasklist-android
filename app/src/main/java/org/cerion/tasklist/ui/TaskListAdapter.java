package org.cerion.tasklist.ui;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Paint;
import android.support.v4.content.ContextCompat;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.cerion.tasklist.R;
import org.cerion.tasklist.data.Task;

import java.util.List;

class TaskListAdapter extends ArrayAdapter<Task> {
    private static final int RESOURCE_ID = R.layout.task_list_item;
    private final Context mContext;
    private final List<Task> mTasks;
    private final int mPrimaryColor;
    private final int mSecondaryColor;

    public TaskListAdapter(Context context, List<Task> objects) {
        super(context, RESOURCE_ID, objects);
        mContext = context;
        mTasks = objects;

        //Programmatically set color based on completion, need to know current theme
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
        mPrimaryColor = ContextCompat.getColor(context, typedValue.resourceId);
        theme.resolveAttribute(android.R.attr.textColorSecondary, typedValue, true);
        mSecondaryColor = ContextCompat.getColor(context, typedValue.resourceId);
    }

    public void refresh(List<Task> tasks) {
        mTasks.clear();
        mTasks.addAll(tasks);
        //mTasks = tasks;
        notifyDataSetChanged();
    }

    private static class ViewHolder {
        TextView title;
        TextView due;
        TextView notes;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Task task = mTasks.get(position);
        ViewHolder viewHolder;

        if(convertView == null) {
            LayoutInflater inflater = ((Activity)mContext).getLayoutInflater();
            convertView = inflater.inflate(RESOURCE_ID, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.title = (TextView)convertView.findViewById(R.id.title);
            viewHolder.due = (TextView)convertView.findViewById(R.id.dueDate);
            viewHolder.notes = (TextView)convertView.findViewById(R.id.notes);
            convertView.setTag(viewHolder);
        }
        else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        String sTitle = task.title.length() > 0 ? task.title : "<Blank>";
        if(task.deleted)
            sTitle = "*DELETED* " + sTitle;
        viewHolder.title.setText(sTitle);
        viewHolder.notes.setText(task.notes);

        if(task.completed) {
            viewHolder.title.setTextColor(mSecondaryColor);
            viewHolder.title.setPaintFlags(viewHolder.title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        }
        else {
            viewHolder.title.setTextColor(mPrimaryColor);
            viewHolder.title.setPaintFlags(viewHolder.title.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
        }

        if(task.due != null && task.due.getTime() != 0)
            viewHolder.due.setText(task.getDue());
        else
            viewHolder.due.setText("");

        return convertView;
    }
}
