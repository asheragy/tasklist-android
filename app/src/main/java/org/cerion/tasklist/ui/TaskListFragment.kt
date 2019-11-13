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
import kotlinx.coroutines.*
import org.cerion.tasklist.MainActivity
import org.cerion.tasklist.R
import org.cerion.tasklist.common.OnListAnyChangeCallback
import org.cerion.tasklist.common.OnSwipeTouchListener
import org.cerion.tasklist.data.Task
import org.cerion.tasklist.data.TaskList
import org.cerion.tasklist.databinding.FragmentTasklistBinding
import org.cerion.tasklist.sync.AuthTools
import org.cerion.tasklist.sync.Sync
import org.cerion.tasklist.ui.dialogs.AlertDialogFragment
import org.cerion.tasklist.ui.dialogs.TaskListDialogFragment
import org.cerion.tasklist.ui.dialogs.TaskListsChangedListener
import kotlin.coroutines.CoroutineContext


class TaskListFragment : Fragment(), TaskListsChangedListener, CoroutineScope  {

    private val TAG = MainActivity::class.java.simpleName
    private lateinit var mSwipeRefresh: SwipeRefreshLayout
    private lateinit var mTaskListAdapter: TaskListAdapter

    private var syncJob: Job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = syncJob + Dispatchers.Main

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
                    override fun toggleDeleted(task: Task) = viewModel.toggleDeleted(task)
                    override fun open(task: Task) = onOpenTask(task)
                }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        mTaskListAdapter.setEmptyView(binding.recyclerView, binding.emptyView)
        binding.recyclerView.adapter = mTaskListAdapter
        binding.recyclerView.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))

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

        // TODO use viewLifecycleOwner elsewhere so multiple observes do not occur
        viewModel.message.observe(viewLifecycleOwner, Observer<String> {
            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
        })

        viewModel.selectedList.addOnPropertyChanged {
            (requireActivity() as AppCompatActivity).supportActionBar?.title = viewModel.currList.title
            populateNavigationLists() // TODO all we really need here is set the selected list but repopulating is the most accurate way of doing it
        }

        //Toolbar
        setHasOptionsMenu(true)
        (requireActivity() as AppCompatActivity).supportActionBar?.title = viewModel.currList.title
        //val toolbar = binding.toolbar
        //(requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)
        //(requireActivity() as AppCompatActivity).supportActionBar?.setDisplayShowTitleEnabled(false) //Hide app name, task lists replace title on actionbar
        //toolbar.setViewModel(viewModel)

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

            if (viewModel.currList == list) {
                item.isChecked = true
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        syncJob.cancel()
    }

    /*
    override fun onResume() {
        //Log.d(TAG,"onResume");
        vm.updateLastSync()
        super.onResume()
    }
    */

    private fun onOpenTask(task: Task?) {
        var list = viewModel.currList
        if (list.isAllTasks)
            list = viewModel.getDefaultList()

        // TODO use safe args, the moveTask fragment does this (look at generated file)
        val bundle = TaskDetailFragment.getBundle(list.id, task?.id ?: "")
        findNavController().navigate(R.id.action_taskListFragment_to_taskDetailFragment, bundle)
    }

    override fun onTaskListsChanged(currList: TaskList) {
        viewModel.setList(currList) //This list was added or updated
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.main, menu)
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
            R.id.action_clear_completed -> viewModel.clearCompleted()
            R.id.action_rename -> {
                val dialog = TaskListDialogFragment.getRenameInstance(viewModel.currList)
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

    private val handler = CoroutineExceptionHandler { _, throwable ->
        Log.e("Exception", ":$throwable")
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
                startSync(token)
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
            }
        })
    }

    private fun startSync(token: String) {
        setInSync(true)
        syncJob = Job()

        launch(handler)  {
            val sync = Sync.getInstance(requireContext(), token)
            var success = false
            var error: String? = null

            //Use dispatcher to switch between context
            try {
                success = withContext(Dispatchers.Default) {
                    sync.run()
                }
            }
            catch (e: Exception) {
                error = e.message
            }

            setInSync(false)
            if (success) {
                viewModel.updateLastSync() //Update last sync time only if successful
                viewModel.hasLocalChanges.set(false)
            } else {
                val message = if(error.isNullOrBlank()) "Sync Failed, unknown error" else error
                val dialog = AlertDialogFragment.newInstance("Sync failed", message)
                dialog.show(requireFragmentManager(), "dialog")
            }

            viewModel.load() //refresh since data may have changed
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


            val action = TaskListFragmentDirections.actionTaskListFragmentToMoveTaskDialogFragment(task.listId, task.id)

            findNavController().navigate(action)
            //val newFragment = MoveTaskDialogFragment.newInstance(task)
            //newFragment.setTargetFragment(this, 0)
            //newFragment.show(requireFragmentManager(), "moveTask")

            return true
        }

        return super.onContextItemSelected(item)
    }
}

fun <T: Observable> T.addOnPropertyChanged(callback: (T) -> Unit) =
        addOnPropertyChangedCallback(object: Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(observable: Observable?, i: Int) {
                callback(observable as T)
            }
        })