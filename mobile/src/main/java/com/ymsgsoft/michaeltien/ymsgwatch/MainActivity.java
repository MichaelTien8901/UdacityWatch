package com.ymsgsoft.michaeltien.ymsgwatch;

import android.content.IntentSender;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

public class MainActivity extends AppCompatActivity implements DataApi.DataListener,
        NodeApi.NodeListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener
{
    /**
        * Request code for launching the Intent to resolve Google Play services errors.
        */
    final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_RESOLVE_ERROR = 1000;
    private static final String WEATHER_PATH = "/weather";
    private static final String WEATHER_KEY = "weather";

    private GoogleApiClient mGoogleApiClient;
    private boolean mResolvingError = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                testWeatherData();
            }
        });
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnected(Bundle bundle) {
        Wearable.DataApi.addListener(mGoogleApiClient, this);
//        Wearable.MessageApi.addListener(mGoogleApiClient, this);
        Wearable.NodeApi.addListener(mGoogleApiClient, this);

    }

    @Override
    public void onPeerDisconnected(Node node) {

    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {

    }

    @Override
    public void onPeerConnected(Node node) {

    }
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        if (mResolvingError) {
            // Already attempting to resolve an error.
            return;
        } else if (result.hasResolution()) {
            try {
                mResolvingError = true;
                result.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                mGoogleApiClient.connect();
            }
        } else {
//            Log.e(TAG, "Connection to Google API client has failed");
            mResolvingError = false;
//            mStartActivityBtn.setEnabled(false);
//            mSendPhotoBtn.setEnabled(false);
            Wearable.DataApi.removeListener(mGoogleApiClient, this);
//            Wearable.MessageApi.removeListener(mGoogleApiClient, this);
            Wearable.NodeApi.removeListener(mGoogleApiClient, this);
        }
    }
    private int highTemp = 32;
    private void testWeatherData()
    {
        Log.d(TAG, "testWeatherData");
        updateWeatherData(511, highTemp++, 28, 1);
    }
    private void updateWeatherData(int weather_id, int high, int low, int unit) {
        byte[] weather_data = new byte[5];
        weather_data[0] = (byte) (weather_id & 0x0000ff);
        weather_data[1] = (byte) (weather_id >> 8 & 0xff);
        weather_data[2] = (byte) high;
        weather_data[3] = (byte) low;
        weather_data[4] = (byte) unit; // metric or uk
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(WEATHER_PATH);
        putDataMapRequest.getDataMap().putByteArray(WEATHER_KEY, weather_data);
        PutDataRequest request = putDataMapRequest.asPutDataRequest();

//        LOGD(TAG, "Generating DataItem: " + request);
        if (!mGoogleApiClient.isConnected()) {
            return;
        }
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
