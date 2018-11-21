package com.lateralimaging.stripreader;

/**
 * Created by matt on 24/02/16.
 */
public interface TaskCheckPoints {
    void OnTaskStart();
    void OnTaskEnd(Object result);
}
