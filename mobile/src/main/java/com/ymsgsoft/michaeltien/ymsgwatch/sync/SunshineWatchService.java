package com.ymsgsoft.michaeltien.ymsgwatch.sync;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import com.ymsgsoft.michaeltien.ymsgwatch.Utility;
import com.ymsgsoft.michaeltien.ymsgwatch.data.WeatherContract;

import java.util.concurrent.TimeUnit;

/**
 * Created by Michael Tien on 2015/10/20.
 */
public class SunshineWatchService extends WearableListenerService {
    private static final String WEATHER_PATH = "/weather";
    private static final String WEATHER_KEY = "weather";

    static final String TAG = SunshineWatchService.class.getSimpleName();
    final String REQUEST_WEATHER_PATH = "/request_weather";
    private static final String[] DETAIL_COLUMNS = {
            WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATE,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
    };
    public static final int COL_WEATHER_ID = 0;
    public static final int COL_WEATHER_DATE = 1;
    public static final int COL_WEATHER_MAX_TEMP = 2;
    public static final int COL_WEATHER_MIN_TEMP = 3;
    public static final int COL_WEATHER_CONDITION_ID = 4;
    static private boolean mSyncFlag = false;
    static Cursor getTodayWeatherCursor(Context context)
    {
        String locationSetting = Utility.getPreferredLocation(context);
        Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(locationSetting, System.currentTimeMillis());
        Cursor cursor = context.getContentResolver().query(weatherForLocationUri,
                DETAIL_COLUMNS,
                null,
                null,
                null);
        return cursor;
    }
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(TAG, "onMessageReceived");
        if (REQUEST_WEATHER_PATH.equals(messageEvent.getPath()) ) {
            Context context = getApplicationContext();
            syncWearWeatherData(context);
        }
    }

    @Override
    public void onPeerConnected(Node peer) {
        super.onPeerConnected(peer);
        Log.d(TAG, "onPeerConnected, mSyncFlag: " + mSyncFlag);
        if ( !mSyncFlag)
           syncWearWeatherData(getApplicationContext());
    }

    static int data_serial_no = 0;
    static public void syncWearWeatherData(Context context){
        Log.e(TAG, "syncWearWeatherData");
        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(context)
                .addApi(Wearable.API)
                .build();
        ConnectionResult connectionResult =
                googleApiClient.blockingConnect(30, TimeUnit.SECONDS);
        if (!connectionResult.isSuccess()) {
            Log.e(TAG, "Failed to connect to GoogleApiClient.");
            mSyncFlag = false;
            return;
        }
        Cursor data = getTodayWeatherCursor(context);
        if ( data != null ) {
            if ( data.moveToFirst()) {
                boolean isMetric = Utility.isMetric(context);
                int weatherId = data.getInt(COL_WEATHER_CONDITION_ID);

                double high = data.getDouble(COL_WEATHER_MAX_TEMP);
                double low = data.getDouble(COL_WEATHER_MIN_TEMP);
                int high_int, low_int;
                if (isMetric) {
                    high_int = (int) (high + 0.5);
                    low_int = (int) (low + 0.5);
                } else {
                    high_int = (int) (high * 9 / 5 + 32.5);
                    low_int = (int) (low * 9 / 5 + 32.5);
                }
                updateWeatherData(googleApiClient, weatherId, high_int, low_int, isMetric ? 0 : 1);
            }
            data.close();
            mSyncFlag = true;
        }
    }
    static void updateWeatherData(GoogleApiClient mGoogleApiClient, int weather_id, int high, int low, int unit) {
        byte[] weather_data = new byte[6];
        weather_data[0] = (byte) (weather_id & 0x0000ff);
        weather_data[1] = (byte) (weather_id >> 8 & 0xff);
        weather_data[2] = (byte) high;
        weather_data[3] = (byte) low;
        weather_data[4] = (byte) unit; // metric or imperial
        weather_data[5] = (byte) data_serial_no++; // data_serial_no number, prevent same data not triggering onDataChanged
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(WEATHER_PATH);
        putDataMapRequest.getDataMap().putByteArray(WEATHER_KEY, weather_data);
        PutDataRequest request = putDataMapRequest.asPutDataRequest();

        Wearable.DataApi.putDataItem(mGoogleApiClient, request)
                .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                    @Override
                    public void onResult(DataApi.DataItemResult dataItemResult) {
                        if (!dataItemResult.getStatus().isSuccess()) {
                            Log.e(TAG, "ERROR: failed to putDataItem, status code: "
                                    + dataItemResult.getStatus().getStatusCode());
                        }
                    }
                });

    }

}
