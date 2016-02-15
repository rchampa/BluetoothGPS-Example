package es.ric.bluetooth.gps;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Location;
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
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import es.ric.bluetoothgps.BTConnectThread;
import es.ric.bluetoothgps.MyBluetoohDevice;
import es.ric.bluetoothgps.ServiceBluetoothGPS;


public class MainActivity extends FragmentActivity implements OnMapReadyCallback {

    ListView lv_lista_bluetooth;
    Button bt_stop,bt_test,bt_animation;
    BTConnectThread hilo_bluetooh;

    List<Location> list_locations;

    GoogleMap mMap;
    Marker marker;
    LatLng mi_casa = new LatLng(40.434229, -3.712912);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ServiceBluetoothGPS.ACTION_CONNECTION_SUCCESS);
        intentFilter.addAction(ServiceBluetoothGPS.ACTION_CONNECTION_CLOSED);
        intentFilter.addAction(ServiceBluetoothGPS.ACTION_CONNECTION_ERROR);
        intentFilter.addAction(ServiceBluetoothGPS.ACTION_CANT_REACH_CONNECTION);
        intentFilter.addAction(ServiceBluetoothGPS.ACTION_NEW_LOCATION);
        registerReceiver(intentReceiver, intentFilter);

        list_locations = new ArrayList<Location>();

        lv_lista_bluetooth = (ListView) findViewById(R.id.lv_lista_bluetooth);
        bt_animation = (Button) findViewById(R.id.bt_animation);
        bt_animation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                List<LatLng> trip_points = getPath();
                new MarkerAnimation().animateLine(trip_points,mMap,marker,MainActivity.this);
            }
        });

        bt_test = (Button) findViewById(R.id.bt_test);
        bt_test.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                List<LatLng> trip_points = getPath();
                animateMarkerList(marker,false,trip_points, true);

            }
        });

        bt_stop = (Button) findViewById(R.id.bt_stop);
        bt_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if(ServiceBluetoothGPS.getInstance()!=null){
                        ServiceBluetoothGPS.getInstance().stopGPS();
                    }

                    File sdCard = Environment.getExternalStorageDirectory();
                    File dir = new File(sdCard.getAbsolutePath() + "/gps_logs");
                    dir.mkdirs();
                    Date now = new Date();
                    File file = new File(dir, now.getTime() + ".txt");
                    FileOutputStream f = new FileOutputStream(file);

                    for (Location position : list_locations) {
                        f.write(position.toString().getBytes());
                        f.write("\n".getBytes());
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

                Intent msgIntent = new Intent(MainActivity.this, ServiceBluetoothGPS.class);
                msgIntent.putExtra(ServiceBluetoothGPS.DEVICE, device.getDevice());
                msgIntent.putExtra(ServiceBluetoothGPS.REFRESH_TIME, 0l);
                MainActivity.this.startService(msgIntent);
//                hilo_bluetooh = new BTConnectThread(device.getDevice(), mBluetoothAdapter, MainActivity.this, 0);

            }
        });


    }

    private BroadcastReceiver intentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context arg0, Intent intent) {

            if (intent.getAction().equals(ServiceBluetoothGPS.ACTION_NEW_LOCATION)) {
                Bundle bundle = intent.getExtras();
                Location position = bundle.getParcelable(ServiceBluetoothGPS.POSICION);
                Log.d("location",position.toString());
                LatLng nueva_posicion = new LatLng(position.getLatitude(), position.getLongitude());
                marker.setPosition(nueva_posicion);
                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(nueva_posicion, 17);
                mMap.moveCamera(cameraUpdate);

                list_locations.add(position);
                if (list_locations.size() > 1) {
                    int n = list_locations.size();
                    Location last_position = list_locations.get(n - 1);

                    float meters = last_position.distanceTo(position);
//                   tv_output.setText(position.toString() + " dif:" + meters + "m");
                }
            }
            else if(intent.getAction().equals(ServiceBluetoothGPS.ACTION_CONNECTION_SUCCESS)) {
                Toast.makeText(MainActivity.this, "Conexión establecida", Toast.LENGTH_SHORT).show();
            }
            else if(intent.getAction().equals(ServiceBluetoothGPS.ACTION_CANT_REACH_CONNECTION)) {
                Toast.makeText(MainActivity.this, "No se ha podido establecer la conexión", Toast.LENGTH_SHORT).show();
            }
            else if(intent.getAction().equals(ServiceBluetoothGPS.ACTION_CONNECTION_ERROR)) {
                Toast.makeText(MainActivity.this, "Se ha perdido la conexión", Toast.LENGTH_SHORT).show();
            }
            else if(intent.getAction().equals(ServiceBluetoothGPS.ACTION_CONNECTION_CLOSED)) {
                Toast.makeText(MainActivity.this, "Se ha cerrado la conexión", Toast.LENGTH_SHORT).show();
            }

        }
    };

    private List<LatLng> getPath(){
        double factor_lon = 0.0002d;
        double factor_lat = 0.0002d;
        List<LatLng> trip_points = new ArrayList<LatLng>();
        trip_points.add(new LatLng(mi_casa.latitude, mi_casa.longitude + factor_lon));
        trip_points.add(new LatLng(mi_casa.latitude, mi_casa.longitude+factor_lon*2));
        trip_points.add(new LatLng(mi_casa.latitude, mi_casa.longitude + factor_lon * 3));
        trip_points.add(new LatLng(mi_casa.latitude, mi_casa.longitude+factor_lon*4));
        trip_points.add(new LatLng(mi_casa.latitude, mi_casa.longitude + factor_lon * 5));
        trip_points.add(new LatLng(mi_casa.latitude, mi_casa.longitude+factor_lon*6));
        trip_points.add(new LatLng(mi_casa.latitude, mi_casa.longitude+factor_lon*7));
        trip_points.add(new LatLng(mi_casa.latitude+factor_lat, mi_casa.longitude+factor_lon*7));
        trip_points.add(new LatLng(mi_casa.latitude+factor_lat*2, mi_casa.longitude+factor_lon*7));
        trip_points.add(new LatLng(mi_casa.latitude+factor_lat*3, mi_casa.longitude+factor_lon*7));
        trip_points.add(new LatLng(mi_casa.latitude+factor_lat*4, mi_casa.longitude+factor_lon*7));
        trip_points.add(new LatLng(mi_casa.latitude + factor_lat * 5, mi_casa.longitude + factor_lon * 7));
        trip_points.add(new LatLng(mi_casa.latitude + factor_lat * 6, mi_casa.longitude + factor_lon * 7));
        return trip_points;
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(mi_casa, 17);
        mMap.animateCamera(cameraUpdate);

        MarkerOptions a = new MarkerOptions()
                .position(mi_casa)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.blue_dot));

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
        final long duration = 1000;

        final Interpolator interpolator = new LinearInterpolator();

        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed
                        / duration);
                double lng = t * toPosition.longitude + (1 - t) * startLatLng.longitude;
                double lat = t * toPosition.latitude + (1 - t) * startLatLng.latitude;
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

    public void animateMarkerList(  final Marker marker,
                                    final boolean hideMarker,
                                    final List<LatLng> lista,
                                    final boolean show_path) {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        Projection proj = mMap.getProjection();
        Point startPoint = proj.toScreenLocation(marker.getPosition());
        final LatLng startLatLng = proj.fromScreenLocation(startPoint);
        final long duration = 1000;//1 second

        final LatLng toPosition = lista.get(0);

        final Interpolator interpolator = new LinearInterpolator();

        handler.post(new Runnable() {
            @Override
            public void run() {
                final LatLng fromPosition = new LatLng(marker.getPosition().latitude, marker.getPosition().longitude);
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed
                        / duration);
                double lng = t * toPosition.longitude + (1 - t) * startLatLng.longitude;
                double lat = t * toPosition.latitude + (1 - t) * startLatLng.latitude;
                marker.setPosition(new LatLng(lat, lng));
                if(show_path) {
                    final PolylineOptions options = new PolylineOptions().width(5).color(Color.BLUE).geodesic(true);
                    options.add(fromPosition);
                    options.add(marker.getPosition());
                    Polyline line = mMap.addPolyline(options);
                }

                if (t < 1.0) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16);
                } else {

                    lista.remove(0);

                    if(lista.size()>0){
                        animateMarkerList(marker, hideMarker, lista, show_path);
                    }
                    else{
                        if (hideMarker) {
                            marker.setVisible(false);
                        } else {
                            marker.setVisible(true);
                        }
                    }

                }
            }
        });
    }
}

