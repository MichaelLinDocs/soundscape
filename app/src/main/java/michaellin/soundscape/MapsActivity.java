package michaellin.soundscape;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
//import android.util.Log;
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

    //GoogleMap
    private GoogleMap soundmap;

    //Android UI variables
    //private static final String TAG = "MapsActivity";
    private Button confirm_location_button;
    private Button cancel_location_button;
    private DrawerLayout drawer_layout;
    private FloatingActionButton playback_button;
    private SeekBar seekbar_radius;
    private TextView seekbar_label;
    private TextView seekbar_radius_text;
    private float stroke_width;
    private int fill_color;
    private int stroke_color;

    //Android buffer variables
    private Circle circle_buffer;
    private Marker marker_buffer;

    //Data buffer variables
    private ArrayList<SoundNode> node_array;
    private boolean playback_enabled;
    private double longitude_buffer;
    private double latitude_buffer;
    private double radius_buffer;
    private String radius_buffer_text;

    //Playback service instance & variables
    private NodePlaybackService playback_service;
    private Intent playback_intent;
    private boolean playback_service_bound;

    //Disable back button
    @Override
    public void onBackPressed() {}

    //Activity initialization
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //GoogleMap initialization
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //Android UI initialization
        confirm_location_button = findViewById(R.id.confirm_button);
        cancel_location_button = findViewById(R.id.cancel_button);
        drawer_layout = findViewById(R.id.drawer_layout);
        playback_button = findViewById(R.id.playback_button);
        seekbar_radius = findViewById(R.id.radiusSeekBar);
        seekbar_label = findViewById(R.id.seekBarLabel);
        seekbar_radius_text = findViewById(R.id.seekBarPercent);
        stroke_width = 2.0f;
        fill_color = Color.argb(125, 80, 180, 255);
        stroke_color = Color.argb(255,80,180,255);

        //Data buffer initialization
        node_array = new ArrayList<>();
        playback_enabled = false;
        radius_buffer = 0;
        radius_buffer_text = "0";
    }

    //GoogleMap object initialization
    @Override
    public void onMapReady(GoogleMap googleMap) {
        soundmap = googleMap;

        //Seekbar initialization
        seekbar_radius.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                radius_buffer_text = '%' + Integer.toString(i);
                seekbar_radius_text.setText(radius_buffer_text);
                radius_buffer = i;
                if(circle_buffer != null)
                    circle_buffer.setRadius(i);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        //Confirm & Cancel button initialization
        confirm_location_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(marker_buffer != null && marker_buffer.isVisible()) {
                    Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(intent,1);
                }
            }
        });
        cancel_location_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hidePeripherals();
            }
        });

        //Zoom into current location
        enableLocation();
        Location current_location = getCurrentLocation();
        if(current_location != null) {
            LatLng current_coords = new LatLng(current_location.getLatitude(), current_location.getLongitude());
            soundmap.moveCamera(CameraUpdateFactory.newLatLng(current_coords));
            soundmap.moveCamera(CameraUpdateFactory.newLatLngZoom(current_coords, 16));
        }
    }

    //Menu UI swipe open
    public void openDrawer(View view) {
        drawer_layout.openDrawer(Gravity.START);
    }

    //SoundNode addition
    public void addSoundNode(MenuItem menuItem) {
        soundmap.clear();
        drawer_layout.closeDrawer(Gravity.START);
        seekbar_radius_text.setText(radius_buffer_text);
        showPeripherals();
    }

    //Receive and process URI link from audio file selection, save data to node buffer
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 1 && resultCode == Activity.RESULT_OK) {
            String uri_buffer = data.getDataString();
            SoundNode node_buffer = new SoundNode(latitude_buffer, longitude_buffer, radius_buffer, uri_buffer);
            node_array.add(node_buffer);
        }
    }

    //Close and remove UI elements, disable OnMapClick
    private void hidePeripherals() {
        if(marker_buffer != null)
            marker_buffer.remove();

        if(circle_buffer != null)
            circle_buffer.remove();

        confirm_location_button.setVisibility(View.GONE);
        cancel_location_button.setVisibility(View.GONE);
        seekbar_label.setVisibility(View.GONE);
        seekbar_radius_text.setVisibility(View.GONE);
        seekbar_radius.setVisibility(View.GONE);
        playback_button.setVisibility(View.VISIBLE);
        soundmap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {@Override public void onMapClick(LatLng latLng) {}});
    }

    //Open and view UI elements, enable OnMapClick
    private void showPeripherals() {
        confirm_location_button.setVisibility(View.VISIBLE);
        cancel_location_button.setVisibility(View.VISIBLE);
        seekbar_radius.setVisibility(View.VISIBLE);
        seekbar_radius_text.setVisibility(View.VISIBLE);
        seekbar_label.setVisibility(View.VISIBLE);
        playback_button.setVisibility(View.GONE);
        soundmap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                if(circle_buffer != null)
                    circle_buffer.remove();

                if(marker_buffer != null) {
                    marker_buffer.remove();
                    marker_buffer.setVisible(false);
                }

                longitude_buffer = latLng.longitude;
                latitude_buffer = latLng.latitude;

                marker_buffer = soundmap.addMarker(new MarkerOptions().position(latLng));
                marker_buffer.setVisible(true);

                circle_buffer = soundmap.addCircle(new CircleOptions().center(latLng));
                circle_buffer.setRadius(radius_buffer);
                circle_buffer.setStrokeColor(stroke_color);
                circle_buffer.setStrokeWidth(stroke_width);
                circle_buffer.setFillColor(fill_color);
            }
        });
    }

    //Display all nodes, hide UI elements
    public void viewSoundscape(MenuItem menuItem) {
        hidePeripherals();
        drawer_layout.closeDrawer(Gravity.START);
        LatLng position_buffer;
        for(SoundNode node : node_array) {
            position_buffer = new LatLng(node.getLatitude(), node.getLongitude());
            soundmap.addMarker(new MarkerOptions()
                    .position(position_buffer));
            soundmap.addCircle(new CircleOptions()
                    .center(position_buffer)
                    .radius(node.getRadius())
                    .strokeWidth(stroke_width)
                    .strokeColor(stroke_color)
                    .fillColor(fill_color));
        }
    }

    //Hide all nodes and UI elements
    public void clearSoundscape(MenuItem menuItem) {
        hidePeripherals();
        drawer_layout.closeDrawer(Gravity.START);
        soundmap.clear();
    }

    public void accessSettings(MenuItem menuItem) {
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

    public void switchPlayback(View view) {
        if(!playback_enabled) {
            playback_button.setImageResource(R.drawable.pause_icon);
            playback_enabled = true;
            beginPlayback();
        }
        else {
            playback_button.setImageResource(R.drawable.play_icon);
            playback_enabled = false;
            stopPlayback();
        }
    }

    private ServiceConnection playback_connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            NodePlaybackService.LocalBinder binder = (NodePlaybackService.LocalBinder) iBinder;
            playback_service = binder.getService();
            playback_service_bound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            playback_service_bound = false;
        }
    };

    private void beginPlayback() {
        playback_intent = new Intent(this, NodePlaybackService.class);
        bindService(playback_intent, playback_connection, Context.BIND_AUTO_CREATE);
        if(playback_service_bound)
        {
            playback_service.recvNodes(node_array);
            startService(playback_intent);
        }
    }

    private void stopPlayback() {
        stopService(playback_intent);
        unbindService(playback_connection);
        playback_service_bound = false;
    }

    //Debug functions
    /*

    private void setPlayableMarkers() {
        soundmap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                String getUri = null;
                LatLng position_buffer;
                for(SoundNode node : node_array) {
                    position_buffer = new LatLng(node.getLatitude(), node.getLongitude());
                    if(marker.getPosition() != null && marker.getPosition().equals(position_buffer)) {
                        getUri = node.getUriLink();
                        break;
                    }
                }

                if(getUri != null) {
                    MediaPlayer mediaPlayer = MediaPlayer.create(getApplicationContext(), Uri.parse(getUri));
                    mediaPlayer.start();
                }
                return true;
            }
        });
    }

    private void nonPlayableMarkers() {
        soundmap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                return false;
            }
        });
    }

    */
}
