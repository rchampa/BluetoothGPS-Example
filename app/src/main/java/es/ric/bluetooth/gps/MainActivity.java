package es.ric.bluetooth.gps;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import es.ric.bluetoothgps.BTConnectThread;
import es.ric.bluetoothgps.BTGPSListener;
import es.ric.bluetoothgps.MyBluetoohDevice;
import es.ric.bluetoothgps.nmea.BTGPSPosition;


public class MainActivity extends Activity implements BTGPSListener {

    ListView lv_lista_bluetooth;
    TextView tv_output;
    Button bt_stop;
    BTConnectThread hilo_bluetooh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lv_lista_bluetooth = (ListView)findViewById(R.id.lv_lista_bluetooth);
        tv_output = (TextView)findViewById(R.id.tv_output);
        bt_stop = (Button)findViewById(R.id.bt_stop);
        bt_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try{
                    hilo_bluetooh.stopConnection();
                }
                catch(Exception e){
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

        ArrayAdapter<MyBluetoohDevice> adapter = new ArrayAdapter<MyBluetoohDevice>(this,android.R.layout.simple_list_item_single_choice,lista_dispositivos);
        lv_lista_bluetooth.setAdapter(adapter);

        lv_lista_bluetooth.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                MyBluetoohDevice device = (MyBluetoohDevice)parent.getItemAtPosition(position);
                Toast.makeText(MainActivity.this,device.toString(), Toast.LENGTH_SHORT).show();
                try {
                    hilo_bluetooh = new BTConnectThread(device.getDevice(), mBluetoothAdapter, MainActivity.this, 0);
                }
                catch(Exception e){
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

                String texto_ant = tv_output.getText().toString();
                String text_actual = position.toString();
//                Spanned spanned = Html.fromHtml(text_actual);
                tv_output.setText(text_actual);
                tv_output.append("\n"+texto_ant);


            }
        });
        Log.d("GPS", nmea_message);
        Log.d("GPS", position.toString());
    }
}
