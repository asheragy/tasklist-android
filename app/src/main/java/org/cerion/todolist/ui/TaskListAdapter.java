package org.cerion.todolist.ui;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.cerion.todolist.R;
import org.cerion.todolist.Task;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;

public class TaskListAdapter extends ArrayAdapter<Task>
{
    Context mContext;
    int mResourceId;
    List<Task> mTasks;
    SimpleDateFormat mDateFormat = null;

    public TaskListAdapter(Context context, int resource, List<Task> objects)
    {
        super(context, resource, objects);
        mContext = context;
        mResourceId = resource;
        mTasks = objects;

        //mDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
        mDateFormat = new SimpleDateFormat("EEE, MMM d, yyyy");
        mDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View row = convertView;

        if(row == null)
        {
            LayoutInflater inflater = ((Activity)mContext).getLayoutInflater();
            row = inflater.inflate(mResourceId, parent, false);

            //row.setTag(holder);
        }

        Task task = mTasks.get(position);

        TextView title = (TextView)row.findViewById(R.id.title);
        title.setText( task.title.length() > 0 ? task.title : "<Blank>" );

        //TODO, add viewholder pattern
        //holder.txtTitle.setText(weather.title);
        //holder.imgIcon.setImageResource(weather.icon);

        ((TextView)row.findViewById(R.id.taskid)).setText(task.id);

        if(task.updated.getTime() == 0)
            ((TextView)row.findViewById(R.id.modified)).setText("Unmodified");
        else
            ((TextView)row.findViewById(R.id.modified)).setText(task.updated.toString());


        if(task.due != null && task.due.getTime() != 0) {
            String sDue = mDateFormat.format(task.due);
            ((TextView) row.findViewById(R.id.dueDate)).setText(sDue);
        }
        else
            ((TextView)row.findViewById(R.id.dueDate)).setText("");


        if(task.notes != null && task.notes.length() > 0)
            ((TextView)row.findViewById(R.id.noteText)).setText(task.notes);
        else
            ((TextView)row.findViewById(R.id.noteText)).setText("");

        row.findViewById(R.id.modified).setVisibility(View.GONE);
        row.findViewById(R.id.taskid).setVisibility(View.GONE);
        return row;
    }
}
