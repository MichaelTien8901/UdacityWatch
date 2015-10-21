package com.ymsgsoft.michaeltien.ymsgwatch;

import android.content.Context;
import android.os.AsyncTask;

import com.ymsgsoft.michaeltien.ymsgwatch.sync.SunshineWatchService;

/**
 * Created by Michael Tien on 2015/10/20.
 */
public class AsyncTaskSendWearData extends AsyncTask<Context, Void, Void> {
    protected Void doInBackground(Context... params) {
        Context context = params[0];
        SunshineWatchService.syncWearWeatherData(context);
        return null;
    }
}