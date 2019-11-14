package com.example.korneltomczak.bt2;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Set;

public class BlueActivity extends AppCompatActivity {

    private Button button_turn;
    private Button button_show;
    private ListView listView;
    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter bluetoothAdapter;
    private String mac;
    public static String EXTRA_MAC="MAC_ADDRESS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blue);

        button_turn = (Button) findViewById(R.id.button_turn);
        button_show=(Button)findViewById(R.id.button_show);
        listView=(ListView) findViewById(R.id.listView);

        button_turn.setWidth(listView.getWidth()/2-10);
        button_show.setWidth(listView.getWidth()/2-10);

    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
    }

    public void message(String s){
        Toast.makeText(getApplicationContext(),s,Toast.LENGTH_LONG).show();
    }

    public void on_show(View view) {
        Set<BluetoothDevice>pairedDevices=bluetoothAdapter.getBondedDevices();
        ArrayList list=new ArrayList();
        if(pairedDevices.size()>0){
            for (BluetoothDevice device:pairedDevices){
                list.add(device.getName()+"\n"+device.getAddress() );
            }
        }else{
            message("No paired devices ");
        }
        final ArrayAdapter adapter=new ArrayAdapter(this,android.R.layout.simple_list_item_1,list);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(myListClickListener);
    }

    public void on_turn_on(View view){
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            message("enable bluetooth");
        }
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BT);
            message("bluetooth enabled");
        }
    }

    private AdapterView.OnItemClickListener myListClickListener=new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            String info=((TextView)view).getText().toString();
            mac=info.substring((info.length()-17));

            Intent intent=new Intent(BlueActivity.this,ControlActivity.class);
            intent.putExtra(EXTRA_MAC,mac);
            startActivity(intent);
        }
    };

}