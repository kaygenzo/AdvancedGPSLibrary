package com.telen.library.advanced_gps.models;

/**
 * Created by karim on 25/03/2018.
 */

public class QueueModel {

    public enum CALLBACK_TYPE {
        CALLBACK_REGISTER_LOCATION_CHANGE
    }

    public enum QUEUE_ACTION {
        LAUNCH_LOCATION_LISTENER,
        STOP_LOCATION_LISTENER,
        LAST_KNOW_LOCATION
    }

    private QUEUE_ACTION action;
    private Object obj;

    public QueueModel(QUEUE_ACTION action) {
        this.action = action;
    }

    public QueueModel(QUEUE_ACTION action, Object obj) {
        this.action = action;
        this.obj = obj;
    }

    public Object getObj() {
        return obj;
    }

    public QUEUE_ACTION getAction() {
        return action;
    }
}
