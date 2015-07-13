package org.cerion.todolist;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

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
        title.setText(task.toString());

        //TODO, add viewholder pattern
        //holder.txtTitle.setText(weather.title);
        //holder.imgIcon.setImageResource(weather.icon);

        ((TextView)row.findViewById(R.id.taskid)).setText(task.id);

        if(task.updated.getTime() == 0)
            ((TextView)row.findViewById(R.id.modified)).setText("Unmodified");
        else
            ((TextView)row.findViewById(R.id.modified)).setText(task.updated.toString());

        return row;
    }
}
