package org.cerion.tasklist.ui

import android.app.Application
import androidx.annotation.NonNull
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.cerion.tasklist.common.ResourceProvider
import org.cerion.tasklist.common.ResourceProvider_Impl
import org.cerion.tasklist.database.AppDatabase
import org.cerion.tasklist.database.Prefs
import org.cerion.tasklist.database.TaskRepository

class ViewModelFactory(private val app: Application) : ViewModelProvider.Factory {

    private val db: AppDatabase
        get() = AppDatabase.getInstance(app)!!

    private val resources: ResourceProvider
        get() = ResourceProvider_Impl(app)

    private val prefs: Prefs
        get() = Prefs.getInstance(app)

    @NonNull
    override fun <T : ViewModel> create(@NonNull modelClass: Class<T>): T {

        if (modelClass.isAssignableFrom(TaskDetailViewModel::class.java))
            return TaskDetailViewModel(ResourceProvider_Impl(app), db.taskDao()) as T

        if (modelClass.isAssignableFrom(TasksViewModel::class.java))
            return TasksViewModel(resources, prefs, db, TaskRepository(db.taskDao()), db.taskListDao(),  app) as T

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
