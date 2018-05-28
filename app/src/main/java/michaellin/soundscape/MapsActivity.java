package michaellin.soundscape;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap soundmap;
    private static final String TAG = "MapsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onBackPressed() {

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        soundmap = googleMap;
        enableLocation();
        Location current_location = getCurrentLocation();
        if(current_location != null) {
            Log.i(TAG, "current_location successful");
            LatLng current_coords = new LatLng(current_location.getLatitude(), current_location.getLongitude());
            MarkerOptions marker_data = new MarkerOptions();
            marker_data.position(current_coords).title("Current Location");
            soundmap.moveCamera(CameraUpdateFactory.newLatLng(current_coords));
            soundmap.moveCamera(CameraUpdateFactory.newLatLngZoom(current_coords, 16));
            soundmap.addMarker(marker_data);
        }
    }

    private void enableLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            soundmap.setMyLocationEnabled(true);
        else
            ActivityCompat.requestPermissions(MapsActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
    }

    private Location getCurrentLocation() {
        Location current_location;
        LocationManager location_manager;

        location_manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
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
