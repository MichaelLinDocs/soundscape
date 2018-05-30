package michaellin.soundscape;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MapsActivity";
    private GoogleMap soundmap;
    private DrawerLayout drawer_layout;
    private Button confirm_location_button;
    private Button cancel_location_button;
    private ArrayList<Marker> marker_array;
    private ArrayList<Circle> circle_array;
    private ArrayList<String> filepath_array;
    private Marker marker_buffer;
    private TextView seekbar_label;
    private TextView seekbar_percentage;
    private SeekBar seekbar_radius;
    private Circle circle_buffer;
    private String percent_tracker;
    private double percent_double;

    @Override
    public void onBackPressed() {}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        drawer_layout = findViewById(R.id.drawer_layout);
        confirm_location_button = findViewById(R.id.confirm_button);
        cancel_location_button = findViewById(R.id.cancel_button);
        seekbar_label = findViewById(R.id.seekBarLabel);
        seekbar_percentage = findViewById(R.id.seekBarPercent);
        seekbar_radius = findViewById(R.id.radiusSeekBar);
        marker_array = new ArrayList<>();
        circle_array = new ArrayList<>();
        filepath_array = new ArrayList<>();
        percent_tracker = "0";
        percent_double = 0;
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

    public void openDrawer(View view)
    {
        drawer_layout.openDrawer(Gravity.START);
    }

    public void addSong(MenuItem menuItem)
    {
        drawer_layout.closeDrawer(Gravity.START);
        seekbar_percentage.setText(percent_tracker);
        showPeripherals();

        seekbar_radius.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                String percent_buffer = '%' + Integer.toString(i);
                seekbar_percentage.setText(percent_buffer);
                percent_double = i;
                percent_tracker = Integer.toString(i);
                if(circle_buffer != null)
                    circle_buffer.setRadius(i);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        confirm_location_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(marker_buffer != null && marker_buffer.isVisible()) {
                    Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(intent,1);

                    marker_array.add(marker_buffer);
                    circle_array.add(circle_buffer);
                    marker_buffer.remove();
                    circle_buffer.remove();
                    marker_buffer.setVisible(false);
                    Log.i(TAG, marker_array.get(marker_array.size()-1).getPosition().toString());
                }
            }
        });

        cancel_location_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hidePeripherals();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        String filepath;

        if(requestCode == 1 && resultCode == Activity.RESULT_OK) {
            filepath = data.getDataString();
            Log.i(TAG, "filepath: " + filepath);
            filepath_array.add(filepath);
        }
    }

    private void setPlayableMarkers()
    {
        soundmap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                int index = 0;
                for(Marker saved_marker : marker_array)
                {
                    if(!marker.getPosition().equals(saved_marker.getPosition()))
                        index++;
                    else
                        break;
                }
                MediaPlayer mediaPlayer = MediaPlayer.create(getApplicationContext(), Uri.parse(filepath_array.get(index)));
                mediaPlayer.start();
                return false;
            }
        });
    }

    private void nonPlayableMarkers()
    {
        soundmap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                return false;
            }
        });
    }

    private void hidePeripherals()
    {
        if(marker_buffer != null)
            marker_buffer.remove();

        if(circle_buffer != null)
            circle_buffer.remove();

        confirm_location_button.setVisibility(View.GONE);
        cancel_location_button.setVisibility(View.GONE);
        seekbar_label.setVisibility(View.GONE);
        seekbar_percentage.setVisibility(View.GONE);
        seekbar_radius.setVisibility(View.GONE);
        soundmap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {@Override public void onMapClick(LatLng latLng) {}});
    }

    private void showPeripherals()
    {
        confirm_location_button.setVisibility(View.VISIBLE);
        cancel_location_button.setVisibility(View.VISIBLE);
        seekbar_radius.setVisibility(View.VISIBLE);
        seekbar_percentage.setVisibility(View.VISIBLE);
        seekbar_label.setVisibility(View.VISIBLE);
        soundmap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                if(circle_buffer != null)
                    circle_buffer.remove();

                if(marker_buffer != null) {
                    marker_buffer.remove();
                    marker_buffer.setVisible(false);
                }

                marker_buffer = soundmap.addMarker(new MarkerOptions().position(latLng));
                marker_buffer.setVisible(true);
                circle_buffer = soundmap.addCircle(new CircleOptions().center(latLng));
                circle_buffer.setRadius(percent_double);
                circle_buffer.setStrokeColor(Color.argb(255,80,180,255));
                circle_buffer.setFillColor(Color.argb(125, 80, 180, 255));
                circle_buffer.setStrokeWidth(2.0f);
            }
        });
    }

    public void viewSoundscape(MenuItem menuItem)
    {
        hidePeripherals();
        drawer_layout.closeDrawer(Gravity.START);
        for(Marker marker : marker_array) {
            soundmap.addMarker(new MarkerOptions().position(marker.getPosition()));
        }
        for(Circle circle: circle_array) {
            soundmap.addCircle(new CircleOptions()
                    .center(circle.getCenter())
                    .fillColor(circle.getFillColor())
                    .radius(circle.getRadius())
                    .strokeColor(circle.getStrokeColor())
                    .strokeWidth(circle.getStrokeWidth()));
        }
        setPlayableMarkers();
    }

    public void clearSoundscape(MenuItem menuItem)
    {
        hidePeripherals();
        drawer_layout.closeDrawer(Gravity.START);
        nonPlayableMarkers();
        soundmap.clear();
    }

    public void accessSettings(MenuItem menuItem)
    {
        hidePeripherals();
        drawer_layout.closeDrawer(Gravity.START);
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
