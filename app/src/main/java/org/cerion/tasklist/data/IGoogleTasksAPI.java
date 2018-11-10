package org.cerion.tasklist.data;


import java.util.Date;
import java.util.List;

import androidx.annotation.Nullable;

public interface IGoogleTasksAPI {

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
