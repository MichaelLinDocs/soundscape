package michaellin.soundscape;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import java.util.ArrayList;

public class NodePlaybackService extends Service {

    private final String TAG = "NodePlaybackService";
    private final IBinder binder = new LocalBinder();
    private ArrayList<SoundNode> nodes = new ArrayList<>();
    private boolean playback_enabled = false;

    public class LocalBinder extends Binder {
        public NodePlaybackService getService() {
            return NodePlaybackService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void recvNodes(ArrayList<SoundNode> nodes)
    {
        this.nodes = new ArrayList<>();
        this.nodes.addAll(nodes);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int StartID)
    {
        playback_enabled = true;

        new Thread(new Runnable() {
            @Override
            public void run() {
                startPlayback();
            }
        }).start();

        return START_STICKY;
    }

    private void startPlayback()
    {
        while(playback_enabled) {
            try {
                Thread.sleep(1000);
                if(playback_enabled) {
                    Location location_buffer = getCurrentLocation();
                    if(location_buffer == null)
                        Log.i(TAG, "Location is null.");
                    else {
                        for(SoundNode node : nodes) {
                            double dist_lat = location_buffer.getLatitude() - node.getLatitude();
                            double dist_lng = location_buffer.getLongitude() - node.getLongitude();
                            double dist = Math.sqrt(Math.pow(dist_lat, 2) + Math.pow(dist_lng, 2)) - node.getRadius();
                            if(dist <= 0)
                                Log.i(TAG, node.getUriLink());
                            else
                                Log.i(TAG, "Out of range.");
                        }
                    }
                }
            }
            catch(InterruptedException e) {
                Log.i(TAG, "Thread Interrupted.");
            }
        }
    }

    private void stopPlayback() {
        playback_enabled = false;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        stopPlayback();
        Log.i(TAG, "Service Destroyed.");
    }

    private Location getCurrentLocation() {
        Location current_location;
        LocationManager location_manager;

        location_manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            assert location_manager != null;
            if (location_manager.isProviderEnabled(LocationManager.GPS_PROVIDER))
                current_location = location_manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            else if (location_manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
                current_location = location_manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            else if (location_manager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER))
                current_location = location_manager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
            else
                return null;
        }
        else
            return null;

        return current_location;
    }
}
