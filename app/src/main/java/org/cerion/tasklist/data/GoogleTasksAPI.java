package org.cerion.tasklist.data;


import android.support.annotation.Nullable;

import java.util.Date;
import java.util.List;

public interface GoogleTasksAPI {

    String BASE_URL = "https://www.googleapis.com/tasks/v1/";
    String API_KEY = "346378052412-b0lrj3jgnucf299u3qf23c4sh4agdgsk.apps.googleusercontent.com";
    //private static final String API_KEY = "AIzaSyAHgDGorXJ1I0WqzpWE6Pm2o894T-j4pdQ";

    interface Tasks {
        boolean delete(Task task);
        List<Task> list(String listId, @Nullable Date dtUpdatedMin) throws APIException;
        Task insert(Task task) throws APIException;
        boolean update(Task task) throws APIException;
    }

    interface Tasklists {
        TaskList get(String id) throws APIException;
        List<TaskList> list() throws APIException;
        TaskList insert(TaskList list) throws APIException;
        boolean update(TaskList list) throws APIException;
    }

    //Error from tasks API, json encoded with error code and message
    class APIException extends Exception {

        private final int mErrorCode;
        public int getErrorCode() {
            return mErrorCode;
        }

        public APIException(String message, int code) {
            super("Error " + code + ": " + message);
            mErrorCode = code;
        }
    }
}
