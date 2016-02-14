package es.ric.bluetooth.gps;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import es.ric.bluetoothgps.BTConnectThread;
import es.ric.bluetoothgps.BTGPSListener;
import es.ric.bluetoothgps.MyBluetoohDevice;
import es.ric.bluetoothgps.nmea.BTGPSPosition;


public class MainActivity extends FragmentActivity implements BTGPSListener,OnMapReadyCallback {

    ListView lv_lista_bluetooth;
    Button bt_stop,bt_test;
    BTConnectThread hilo_bluetooh;

    List<BTGPSPosition> list_locations;

    GoogleMap mMap;
    Marker marker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        list_locations = new ArrayList<BTGPSPosition>();

        lv_lista_bluetooth = (ListView) findViewById(R.id.lv_lista_bluetooth);
        bt_test = (Button) findViewById(R.id.bt_test);
        bt_test.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                animateMarker(marker,new LatLng(40.434278, -3.712051),false);
//                marker.setPosition();
            }
        });
        bt_stop = (Button) findViewById(R.id.bt_stop);
        bt_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    hilo_bluetooh.stopConnection();

                    File sdCard = Environment.getExternalStorageDirectory();
                    File dir = new File(sdCard.getAbsolutePath() + "/gps_logs");
                    dir.mkdirs();
                    Date now = new Date();
                    File file = new File(dir, now.getTime() + ".txt");
                    FileOutputStream f = new FileOutputStream(file);

                    for (BTGPSPosition position : list_locations) {
                        f.write(position.toString().getBytes());
                    }

                    f.close();

                    list_locations.clear();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        List<MyBluetoohDevice> lista_dispositivos = new ArrayList<MyBluetoohDevice>();

        final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
        }
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        // If there are paired devices
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                // Add the name and address to an array adapter to show in a ListView
                lista_dispositivos.add(new MyBluetoohDevice(device));
            }
        }

        ArrayAdapter<MyBluetoohDevice> adapter = new ArrayAdapter<MyBluetoohDevice>(this, android.R.layout.simple_list_item_single_choice, lista_dispositivos);
        lv_lista_bluetooth.setAdapter(adapter);

        lv_lista_bluetooth.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                MyBluetoohDevice device = (MyBluetoohDevice) parent.getItemAtPosition(position);
                Toast.makeText(MainActivity.this, device.toString(), Toast.LENGTH_SHORT).show();
                try {
                    hilo_bluetooh = new BTConnectThread(device.getDevice(), mBluetoothAdapter, MainActivity.this, 0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                hilo_bluetooh.start();
            }
        });


    }

    @Override
    public void update(final BTGPSPosition position, String nmea_message) {
        this.runOnUiThread(new Runnable() {
           @Override
           public void run() {
               LatLng nueva_posicion = new LatLng(position.lat, position.lon);
               marker.setPosition(nueva_posicion);
               CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(nueva_posicion, 17);
               mMap.moveCamera(cameraUpdate);

               list_locations.add(position);
               if (list_locations.size() > 1) {



                   int n = list_locations.size();
                   BTGPSPosition last_position = list_locations.get(n - 1);

                   float meters = last_position.getLocation().distanceTo(position.getLocation());
//                   tv_output.setText(position.toString() + " dif:" + meters + "m");
               }


           }
        });

        Log.d("GPS", nmea_message);
        Log.d("GPS", position.toString());

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        LatLng mi_casa = new LatLng(40.4342821,-3.7152397);
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(mi_casa, 17);
        mMap.animateCamera(cameraUpdate);

        MarkerOptions a = new MarkerOptions().position(mi_casa);
        marker = mMap.addMarker(a);
        marker.setPosition(mi_casa);
    }

    public void animateMarker(final Marker marker, final LatLng toPosition,
                              final boolean hideMarker) {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        Projection proj = mMap.getProjection();
        Point startPoint = proj.toScreenLocation(marker.getPosition());
        final LatLng startLatLng = proj.fromScreenLocation(startPoint);
        final long duration = 500;

        final Interpolator interpolator = new LinearInterpolator();

        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed
                        / duration);
                double lng = t * toPosition.longitude + (1 - t)
                        * startLatLng.longitude;
                double lat = t * toPosition.latitude + (1 - t)
                        * startLatLng.latitude;
                marker.setPosition(new LatLng(lat, lng));

                if (t < 1.0) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16);
                } else {
                    if (hideMarker) {
                        marker.setVisible(false);
                    } else {
                        marker.setVisible(true);
                    }
                }
            }
        });
    }
}

