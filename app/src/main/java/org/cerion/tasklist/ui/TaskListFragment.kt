package org.cerion.tasklist.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.Observable
import androidx.databinding.ObservableList
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import org.cerion.tasklist.R
import org.cerion.tasklist.common.OnListAnyChangeCallback
import org.cerion.tasklist.common.OnSwipeTouchListener
import org.cerion.tasklist.common.TAG
import org.cerion.tasklist.database.Task
import org.cerion.tasklist.database.TaskList
import org.cerion.tasklist.databinding.FragmentTasklistBinding
import org.cerion.tasklist.sync.AuthTools
import org.cerion.tasklist.ui.dialogs.AlertDialogFragment
import org.cerion.tasklist.ui.dialogs.TaskListDialogFragment
import org.cerion.tasklist.ui.dialogs.TaskListsChangedListener


class TaskListFragment : Fragment(), TaskListsChangedListener  {

    private lateinit var mSwipeRefresh: SwipeRefreshLayout
    private lateinit var mTaskListAdapter: TaskListAdapter

    private val viewModel: TasksViewModel by lazy {
        val factory = ViewModelFactory(requireActivity().application)
        ViewModelProviders.of(requireActivity(), factory).get(TasksViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_tasklist_single, container, false)
        val content = view.findViewById<FrameLayout>(R.id.listContent)
        val binding = FragmentTasklistBinding.inflate(inflater, container, false)
        content.removeAllViews()
        content.addView(binding.root, 0)

        mTaskListAdapter = TaskListAdapter(
                viewModel.tasks,
                object : TaskListener {
                    override fun toggleComplete(task: Task) = viewModel.toggleCompleted(task)
                    override fun undelete(task: Task) = viewModel.undoDelete(task)
                    override fun open(task: Task) = onOpenTask(task)
                }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        mTaskListAdapter.setEmptyView(binding.recyclerView, binding.emptyView)
        binding.recyclerView.adapter = mTaskListAdapter
        binding.recyclerView.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))

        binding.layoutDebug.visibility = View.GONE
        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        val defaultTextColor = binding.status.textColors.defaultColor

        viewModel.isOutOfSync.observe(viewLifecycleOwner, Observer { outOfSync ->
            if (outOfSync) {
                binding.status.setTextColor(Color.RED)
                onSync()
            } else
                binding.status.setTextColor(defaultTextColor)
        })

        viewModel.hasLocalChanges.addOnPropertyChangedCallback(object : Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                if(viewModel.hasLocalChanges.get() == true) {
                    viewModel.hasLocalChanges.set(false)
                    viewModel.refreshTasks()
                    onSync()
                }
            }
        })

        viewModel.lists.addOnListChangedCallback(object : OnListAnyChangeCallback<ObservableList<TaskList>>() {
            override fun onAnyChange(sender: ObservableList<*>?) {
                populateNavigationLists()
            }
        })

        viewModel.message.observe(viewLifecycleOwner, Observer<String> {
            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
        })

        viewModel.selectedList.observe(viewLifecycleOwner, Observer { list ->
            populateNavigationLists() // TODO all we really need here is set the selected list but repopulating is the most accurate way of doing it
            (requireActivity() as AppCompatActivity).supportActionBar?.title = list.title
        })

        viewModel.syncing.observe(viewLifecycleOwner, Observer { syncing ->
            // TODO see if 'isRefreshing = syncing' will work
            if (!syncing && mSwipeRefresh.isRefreshing)
                mSwipeRefresh.isRefreshing = false
            else if (syncing && !mSwipeRefresh.isRefreshing)
                mSwipeRefresh.isRefreshing = true
        })

        viewModel.deletedTask.observe(viewLifecycleOwner, Observer { task ->
            if(task != null) {
                Snackbar.make(requireActivity().findViewById(android.R.id.content), "Task deleted", Snackbar.LENGTH_SHORT)
                        .setAction("UNDO") { viewModel.undoDelete(task) }
                        .show()

                viewModel.deleteConfirmed()
            }
        })

        setHasOptionsMenu(true)
        populateNavigationLists()

        val touchListener = object : OnSwipeTouchListener(requireContext()) {
            override fun onSwipeLeft() {
                viewModel.moveLeft()
            }

            override fun onSwipeRight() {
                viewModel.moveRight()
            }
        }

        binding.fab.setOnClickListener { onOpenTask(null) }

        binding.emptyView.setOnTouchListener(touchListener)
        binding.recyclerView.setOnTouchListener(touchListener)
        mSwipeRefresh = binding.swipeRefresh
        mSwipeRefresh.setOnRefreshListener(this::onSync)

        return view
    }

    private fun populateNavigationLists() {
        // TODO this may all be better done using adapter and list view
        val drawer = requireActivity().findViewById<DrawerLayout>(R.id.drawerLayout)
        val navView = requireActivity().findViewById<NavigationView>(R.id.navView)

        navView.menu.clear()
        navView.inflateMenu(R.menu.nav_drawer)
        val menu = navView.menu.getItem(0).subMenu
        menu.clear() // clears placeholder items

        for (list in viewModel.lists) {
            val item = menu.add(list.title)
            item.setOnMenuItemClickListener {
                viewModel.setList(list)
                drawer.closeDrawers()
                true
            }

            if (viewModel.selectedList.value == list) {
                item.isChecked = true
            }
        }
    }

    private fun onOpenTask(task: Task?) {
        // TODO fragment has viewmodel so can probably just pass listId or empty here
        val action =
                if (task != null)
                    TaskListFragmentDirections.actionTaskListFragmentToTaskDetailFragment(task.listId, task.id)
                else {
                    val list = viewModel.selectedList.value!!
                    val id = if(list.isAllTasks) viewModel.defaultList.id else list.id
                    TaskListFragmentDirections.actionTaskListFragmentToTaskDetailFragment(id, "")
                }

        findNavController().navigate(action)
    }

    override fun onTaskListsChanged(currList: TaskList) {
        viewModel.setList(currList) //This list was added or updated
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.main, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.action_rename).isVisible = !viewModel.selectedList.value!!.isAllTasks
        menu.findItem(R.id.action_delete).isVisible = viewModel.tasks.size == 0
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_add -> onAddTaskList()
            R.id.action_clear_completed -> viewModel.clearCompleted()
            R.id.action_rename -> {
                val dialog = TaskListDialogFragment.getRenameInstance(viewModel.selectedList.value!!)
                dialog.setTargetFragment(this, 0)
                dialog.show(requireFragmentManager(), "dialog")
            }
            R.id.action_delete -> viewModel.deleteCurrentList()
        }

        return super.onOptionsItemSelected(item)
    }

    private fun onAddTaskList() {
        val dialog = TaskListDialogFragment.getAddInstance()
        dialog.setTargetFragment(this, 0)
        dialog.show(requireFragmentManager(), "dialog")
    }

    private fun onSync() {
        val cm = requireActivity().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = cm.activeNetworkInfo
        val isConnected = networkInfo != null && networkInfo.isConnected

        if (!isConnected) {
            Toast.makeText(requireContext(), "Internet not available", Toast.LENGTH_SHORT).show()
            if (mSwipeRefresh.isRefreshing)
                mSwipeRefresh.isRefreshing = false

            return
        }

        AuthTools.getAuthToken(requireContext(), requireActivity(), object : AuthTools.AuthTokenCallback {
            override fun onSuccess(token: String) {
                viewModel.sync(token)
            }

            override fun onError(e: Exception?) {
                if (e == null) {
                    //TODO do automatically
                    Toast.makeText(requireContext(), "Open settings and select account", Toast.LENGTH_LONG).show()
                }
                else {
                    val dialog = AlertDialogFragment.newInstance("Auth Error", e.message!!)
                    dialog.show(requireFragmentManager(), "dialog")
                }

                mSwipeRefresh.isRefreshing = false
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d(TAG, "onActivityResult: $resultCode")

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

        when (id) {
            R.id.complete -> viewModel.toggleCompleted(task)
            R.id.delete -> {
                if (task.deleted)
                    viewModel.undoDelete(task)
                else
                    viewModel.delete(task)
            }
            R.id.move -> {
                val action = TaskListFragmentDirections.actionTaskListFragmentToMoveTaskDialogFragment(task.listId, task.id)
                findNavController().navigate(action)
            }
            else -> return super.onContextItemSelected(item)
        }

        return true
    }
}