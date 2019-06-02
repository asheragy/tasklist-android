package org.cerion.tasklist.ui

import android.accounts.OperationCanceledException
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.databinding.Observable
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import org.cerion.tasklist.R
import org.cerion.tasklist.data.Task
import org.cerion.tasklist.data.TaskList
import org.cerion.tasklist.databinding.FragmentTasklistBinding
import org.cerion.tasklist.dialogs.AlertDialogFragment
import org.cerion.tasklist.dialogs.MoveTaskDialogFragment
import org.cerion.tasklist.dialogs.TaskListDialogFragment
import org.cerion.tasklist.dialogs.TaskListsChangedListener
import org.cerion.tasklist.sync.OnSyncCompleteListener
import org.cerion.tasklist.sync.SyncTask

//TODO verify network is available and toast message

class TaskListFragment : Fragment(), TaskListsChangedListener {

    private val TAG = MainActivity::class.java.simpleName
    private val EDIT_TASK_REQUEST = 0

    private lateinit var mSwipeRefresh: SwipeRefreshLayout
    private lateinit var mTaskListAdapter: TaskListAdapter

    private val viewModel: TasksViewModel by lazy {
        val factory = ViewModelFactory(requireActivity().application)
        ViewModelProviders.of(this, factory).get(TasksViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentTasklistBinding.inflate(inflater, container, false)

        mTaskListAdapter = TaskListAdapter(
                viewModel.tasks,
                object : TaskListener {
                    override fun toggleComplete(task: Task) = viewModel.toggleCompleted(task)
                    override fun toggleDeleted(task: Task) = viewModel.toggleDeleted(task)
                    override fun open(task: Task) = onOpenTask(task)
                }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = mTaskListAdapter
        binding.recyclerView.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        mTaskListAdapter.setEmptyView(binding.recyclerView, binding.emptyView)

        binding.layoutDebug.visibility = View.GONE
        binding.viewModel = viewModel

        val defaultTextColor = binding.status.textColors.defaultColor
        viewModel.isOutOfSync.addOnPropertyChangedCallback(object : Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(observable: Observable, i: Int) {
                if (viewModel.isOutOfSync.get()!!) {
                    binding.status.setTextColor(Color.RED)
                    onSync()
                } else
                    binding.status.setTextColor(defaultTextColor)
            }
        })

        //Toolbar
        setHasOptionsMenu(true)
        val toolbar = binding.toolbar
        //requireActivity().setActionBar(toolbar)
        //().actionBar?.setDisplayShowTitleEnabled(false) //Hide app name, task lists replace title on actionbar
        toolbar.setViewModel(viewModel)

        val touchListener = object : OnSwipeTouchListener(requireContext()) {
            override fun onSwipeLeft() {
                toolbar.moveLeft()
            }

            override fun onSwipeRight() {
                toolbar.moveRight()
            }
        }

        binding.fab.setOnClickListener { onOpenTask(null) }

        binding.emptyView.setOnTouchListener(touchListener)
        binding.recyclerView.setOnTouchListener(touchListener)
        mSwipeRefresh = binding.swipeRefresh
        mSwipeRefresh.setOnRefreshListener(this::onSync)

        initViewModel()
        return binding.root
    }

    /*
    override fun onResume() {
        //Log.d(TAG,"onResume");
        vm.updateLastSync()
        super.onResume()
    }
    */

    fun onOpenTask(task: Task?) {
        var list = viewModel.currList
        if (list.isAllTasks)
            list = viewModel.getDefaultList()

        //val intent = TaskDetailActivity.getIntent(requireContext(), task?.listId ?: list.id, task)
        //startActivityForResult(intent, EDIT_TASK_REQUEST)

        val fragment = TaskDetailFragment.getInstance(list.id, task?.id ?: "")

        activity!!.supportFragmentManager.beginTransaction()
                .replace(R.id.fragment, fragment)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .addToBackStack(null)
                .commit()

    }

    override fun onTaskListsChanged(current: TaskList) {
        viewModel.setList(current) //This list was added or updated
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.main, menu)
    }

    /* TODO need to convert
    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.action_rename).isVisible = !vm.getCurrList().isAllTasks() //Hide rename if "All Tasks" list
        menu.findItem(R.id.action_delete).isVisible = mTaskListAdapter.itemCount === 0

        return super.onPrepareOptionsMenu(menu)
    }
    */

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId

        when (id) {
            R.id.action_add -> onAddTaskList()
            R.id.action_settings -> {
                val intent = Intent(requireActivity(), SettingsActivity::class.java)
                startActivity(intent)
            }
            R.id.action_clear_completed -> viewModel.clearCompleted()
            R.id.action_rename -> {
                val dialog = TaskListDialogFragment.newInstance(TaskListDialogFragment.TYPE_RENAME, viewModel.currList)
                dialog.show(fragmentManager, "dialog")
            }
            R.id.action_delete -> viewModel.deleteCurrentList()
        }

        return super.onOptionsItemSelected(item)
    }

    private fun onAddTaskList() {
        val dialog = TaskListDialogFragment.newInstance(TaskListDialogFragment.TYPE_ADD, null)
        dialog.show(fragmentManager, "dialog")
    }

    private fun initViewModel() {
        //viewModel.message.observe(this, { s -> Toast.makeText(this@MainActivity, s, Toast.LENGTH_SHORT).show() })

        viewModel.load()
    }

    private fun onSync() {
        val cm = requireActivity().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = cm.activeNetworkInfo
        val isConnected = networkInfo != null && networkInfo.isConnected

        if (isConnected) {
            setInSync(true)

            SyncTask.run(requireContext(), requireActivity(), object : OnSyncCompleteListener {
                override fun onAuthError(e: Exception?) {
                    setInSync(false)

                    if (e == null) {
                        //TODO do automatically
                        Toast.makeText(requireContext(), "Open settings and select account", Toast.LENGTH_LONG).show()
                    } else if (e is OperationCanceledException) {
                        Log.d(TAG, "User denied auth prompt")
                        //For some reason showing an AlertDialog here causes a crash
                    } else {
                        val dialog = AlertDialogFragment.newInstance("Auth Error", e.message)
                        dialog.show(fragmentManager, "dialog")
                    }
                }

                override fun onSyncFinish(bSuccess: Boolean, e: Exception?) {
                    setInSync(false)

                    if (bSuccess) {
                        viewModel.updateLastSync() //Update last sync time only if successful
                    } else {
                        var message = "Sync Failed, unknown error"
                        if (e != null)
                            message = e.message!!

                        val dialog = AlertDialogFragment.newInstance("Sync failed", message)
                        dialog.show(fragmentManager, "dialog")
                    }

                    viewModel.load() //refresh since data may have changed
                }

            })
        } else {
            val dialog = AlertDialogFragment.newInstance("Error", "Internet not available")
            dialog.show(fragmentManager, "dialog")
            if (mSwipeRefresh.isRefreshing)
                mSwipeRefresh.isRefreshing = false
        }
    }

    private fun setInSync(bSyncing: Boolean) {
        if (!bSyncing && mSwipeRefresh.isRefreshing)
            mSwipeRefresh.isRefreshing = false
        else if (bSyncing && !mSwipeRefresh.isRefreshing)
            mSwipeRefresh.isRefreshing = true
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d(TAG, "onActivityResult: $resultCode")

        if (requestCode == EDIT_TASK_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                viewModel.refreshTasks()
                onSync()
            }
        }

        /*
        else if (requestCode == PICK_ACCOUNT_REQUEST) {
            check resultCode too
            String currentAccount = mPrefs.getString(Prefs.KEY_ACCOUNT_NAME);
            String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);

            //If current account is set and different than selected account, logout first
            if (currentAccount.length() > 0 && !currentAccount.contentEquals(accountName))
                AuthTools.logout(this);

            mPrefs.setString(Prefs.KEY_ACCOUNT_NAME, accountName);
        }
        */
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        val task = mTaskListAdapter.getItem(mTaskListAdapter.itemPosition)

        if (id == R.id.complete || id == R.id.delete) {
            if (id == R.id.complete)
                viewModel.toggleCompleted(task)
            else
                viewModel.toggleDeleted(task)

            return true
        }

        if (id == R.id.move) {
            Log.d(TAG, "onMove")

            val newFragment = MoveTaskDialogFragment.newInstance(task)
            newFragment.show(fragmentManager, "moveTask")

            return true
        }

        return super.onContextItemSelected(item)
    }
}