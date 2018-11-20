package org.cerion.tasklist.data;

public class GoogleApi {

    static final String API_KEY = "346378052412-b0lrj3jgnucf299u3qf23c4sh4agdgsk.apps.googleusercontent.com";
    //private static final String API_KEY = "AIzaSyAHgDGorXJ1I0WqzpWE6Pm2o894T-j4pdQ";
    static final boolean mLogFullResults = true;

    public static final String TASKS_BASE_URL = "https://www.googleapis.com/tasks/v1/";

    private final String mAuthKey;

    public GoogleApi(String authKey) {
        mAuthKey = authKey;
    }

    public GoogleTasklistsApi getTaskListsApi() {
        return new GoogleTasklistsApi_Impl(mAuthKey);
    }

    public GoogleTasksApi getTasksApi() {
        return new GoogleTasksApi_Impl(mAuthKey);
    }
}
