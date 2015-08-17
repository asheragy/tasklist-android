package org.cerion.todolist.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.cerion.todolist.R;
import org.cerion.todolist.data.Task;

import java.util.List;

class TaskListAdapter extends ArrayAdapter<Task> {
    private static final int RESOURCE_ID = R.layout.row_list;
    private Context mContext;
    private List<Task> mTasks;

    public TaskListAdapter(Context context, List<Task> objects) {
        super(context, RESOURCE_ID, objects);
        mContext = context;
        mTasks = objects;
    }

    private static class ViewHolder {
        TextView id;
        TextView title;
        TextView modified;
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
            viewHolder.id = (TextView)convertView.findViewById(R.id.taskid);
            viewHolder.modified = (TextView)convertView.findViewById(R.id.modified);
            viewHolder.notes = (TextView)convertView.findViewById(R.id.notes);
            convertView.setTag(viewHolder);
        }
        else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.id.setText(task.id);

        String sTitle = task.title.length() > 0 ? task.title : "<Blank>";
        if(task.deleted)
            sTitle = "*DELETED* " + sTitle;
        viewHolder.title.setText(sTitle);
        viewHolder.notes.setText(task.notes);

        if(task.completed)
            viewHolder.title.setPaintFlags(viewHolder.title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        else
            viewHolder.title.setPaintFlags(viewHolder.title.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);

        if(task.updated.getTime() == 0)
            viewHolder.modified.setText("Unmodified");
        else
            viewHolder.modified.setText(task.updated.toString());

        if(task.due != null && task.due.getTime() != 0)
            viewHolder.due.setText(task.getDue());
        else
            viewHolder.due.setText("");

        viewHolder.modified.setVisibility(View.GONE);
        viewHolder.id.setVisibility(View.GONE);

        return convertView;
    }
}
