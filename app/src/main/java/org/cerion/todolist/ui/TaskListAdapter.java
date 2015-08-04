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
import org.cerion.todolist.Task;

import java.util.List;

public class TaskListAdapter extends ArrayAdapter<Task>
{
    Context mContext;
    int mResourceId;
    List<Task> mTasks;

    public TaskListAdapter(Context context, int resource, List<Task> objects)
    {
        super(context, resource, objects);
        mContext = context;
        mResourceId = resource;
        mTasks = objects;

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
        if(task.completed)
            title.setPaintFlags(title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        else
            title.setPaintFlags(title.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);


        //TODO, add viewholder pattern
        //holder.txtTitle.setText(weather.title);
        //holder.imgIcon.setImageResource(weather.icon);

        ((TextView)row.findViewById(R.id.taskid)).setText(task.id);

        if(task.updated.getTime() == 0)
            ((TextView)row.findViewById(R.id.modified)).setText("Unmodified");
        else
            ((TextView)row.findViewById(R.id.modified)).setText(task.updated.toString());


        if(task.due != null && task.due.getTime() != 0) {
            ((TextView) row.findViewById(R.id.dueDate)).setText(task.getDue());
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
