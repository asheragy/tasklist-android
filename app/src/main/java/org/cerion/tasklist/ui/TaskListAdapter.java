package org.cerion.tasklist.ui;

import android.content.res.Resources;
import android.databinding.ObservableList;
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
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.TextView;

import org.cerion.tasklist.R;
import org.cerion.tasklist.common.OnListAnyChangeCallback;
import org.cerion.tasklist.data.Task;

class TaskListAdapter extends RecyclerView.Adapter<TaskListAdapter.ViewHolder> {

    private static final String TAG = TaskListAdapter.class.getSimpleName();

    private final TasksViewModel vm;
    private int mPrimaryColor;
    private int mSecondaryColor;
    private final MainActivity mActivity;
    //private TaskList mCurrList;
    private RecyclerView parent;
    private View emptyView;

    public TaskListAdapter(MainActivity activity, TasksViewModel viewModel) {
        mActivity = activity;
        vm = viewModel;
    }

    public void setEmptyView(RecyclerView recyclerView, View emptyView) {
        this.parent = recyclerView;
        this.emptyView = emptyView;
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

        vm.getTasks().addOnListChangedCallback(new OnListAnyChangeCallback<ObservableList<Task>>() {
            @Override
            public void onAnyChange(ObservableList sender) {
                if (vm.getTasks().size() > 0) {
                    parent.setVisibility(View.VISIBLE);
                    emptyView.setVisibility(View.GONE);
                } else {
                    parent.setVisibility(View.GONE);
                    emptyView.setVisibility(View.VISIBLE);
                }

                notifyDataSetChanged();
            }
        });
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_task, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Task task = vm.getTasks().get(position);

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
        return vm.getTasks().size();
    }

    public Task getItem(int position) {
        return vm.getTasks().get(position);
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

        ViewHolder(View view) {
            super(view);

            title = view.findViewById(R.id.title);
            due = view.findViewById(R.id.dueDate);
            notes = view.findViewById(R.id.notes);
            completed = view.findViewById(R.id.completed);
            undelete = view.findViewById(R.id.undelete);

            view.setOnClickListener(this);
            view.setOnCreateContextMenuListener(this);
            completed.setOnCheckedChangeListener(this);
            undelete.setOnClickListener(mOnUnDeleteListener);
        }

        private final OnClickListener mOnUnDeleteListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                Task task = vm.getTasks().get(getLayoutPosition());
                vm.toggleDeleted(task);
            }
        };

        @Override
        public void onClick(View v) {
            Task task = vm.getTasks().get(getLayoutPosition());
            mActivity.onOpenTask(task);
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {

            MenuInflater inflater = mActivity.getMenuInflater();
            inflater.inflate(R.menu.main_context, menu);

            position = getLayoutPosition();
            Task task = vm.getTasks().get(position);
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
        public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
            final Task task = vm.getTasks().get(getLayoutPosition());
            if(task.completed != isChecked) { //checkbox was manually changed

                AlphaAnimation anim = new AlphaAnimation(1.0f, 0.0f);
                anim.setDuration(500);
                anim.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        //Update record in database and refresh list
                        Log.d(TAG,"Toggle completed checkbox");
                        vm.toggleCompleted(task);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });

                this.itemView.startAnimation(anim);
            }
        }
    }

}
