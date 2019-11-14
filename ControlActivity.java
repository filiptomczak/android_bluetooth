package com.example.korneltomczak.bt2;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Scanner;
import java.util.UUID;



public class ControlActivity extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = "MY_APP_DEBUG_TAG";

    private BluetoothAdapter btAdapter;
    private BluetoothSocket btSocket;
    private BluetoothDevice btDevice;


    private UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private String mac;
    private Button button_up;
    private Button button_right;

    private TextView textView2;
    private TextView textView_current;
    private TextView textView_min;
    private TextView textView_max;
    private TextView textView_minDate;
    private TextView textView_maxDate;

    private ProgressBar progressBar;

    private SensorManager sensorManager;
    private Sensor sensorGrav;

    float[] grav = new float[3];
    String[] a = new String[4];
    String[] g = new String[4];
    boolean connected=false;

    float accZprev=0.00f;

    float min = 100;
    float max = -100;
    float prevCurrent=20;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        mac = intent.getStringExtra(BlueActivity.EXTRA_MAC);

        setContentView(R.layout.activity_control);

        Toast.makeText(getApplicationContext(), mac, Toast.LENGTH_LONG).show();
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        btDevice = btAdapter.getRemoteDevice(mac);

        button_up = findViewById(R.id.button_on);
        button_right = findViewById(R.id.button_off);
        textView2 = findViewById(R.id.textView2);
        textView_current = findViewById(R.id.textView_current);
        textView_min = findViewById(R.id.textView_minTemp);
        textView_max = findViewById(R.id.textView_maxTemp);
        textView_minDate = findViewById(R.id.textView_min);
        textView_maxDate = findViewById(R.id.textView_max);
        progressBar = findViewById(R.id.progressBar);

        ConnectThread connectThread = new ConnectThread(btDevice);
        connectThread.start();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorGrav = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
    }

    private class ConnectThread extends Thread {

        public ConnectThread(BluetoothDevice device) {
            BluetoothSocket tmp = null;
            btDevice = device;
            try {
                tmp = device.createRfcommSocketToServiceRecord(uuid);
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            btSocket = tmp;
        }

        public void run() {
            btAdapter.cancelDiscovery();
            try {
                btSocket.connect();
                progressBar.setVisibility(View.INVISIBLE);
                textView_current.setText("waiting for incoming data...");
                connected=true;
            } catch (IOException connectException) {
                try {
                    btSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }
            manageMyConnectedSocket(btSocket);
        }

        public void cancel() {
            try {
                btSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }

    public void manageMyConnectedSocket(BluetoothSocket socket) {
        ConnectedThread connectedThread = new ConnectedThread(socket);
        connectedThread.start();
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket btSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;
        private byte[] buffer;

        private ConnectedThread(BluetoothSocket socket) {
            btSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating input stream", e);
            }
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating output stream", e);
            }
            inputStream = tmpIn;
            outputStream = tmpOut;
        }

        public void run() {
            buffer = new byte[8];
            int numBytes;

            while (true) {
                try {
                    numBytes = inputStream.read(buffer);
                    String readMessage=new String(buffer, 0, numBytes);

                    displayTemp(readMessage);
                } catch (IOException e) {
                    Log.d(TAG, "Input stream was disconnected", e);
                    break;
                }
            }
        }

        private void write(byte[] bytes) {
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when sending data", e);
            }
        }

        public void cancel() {
            try {
                btSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }

    public void displayTemp(String currentTemp) {
        textView_current.setText(currentTemp);

        try {
            float current = Float.parseFloat(currentTemp);
            /*filtering random/bad temp read*/
            if(current-prevCurrent>10 && prevCurrent-current<-10){
                // do nothing
            }
            else if(current-prevCurrent<-10 && prevCurrent-current>10){
                //do nothing
            }
            else{
                if (current < min) {
                    min = current;
                    //Date currentTime = Calendar.getInstance().getTime();
                    //textView_minDate.setText(currentTime.toString());
                    textView_min.setText(String.valueOf(min));
                }
                if (current > max) {
                    max = current;
                    //Date currentTime = Calendar.getInstance().getTime();
                    //textView_maxDate.setText(currentTime.toString());
                    textView_max.setText(String.valueOf(max));
                }
                prevCurrent=current;
            }

        }catch(NumberFormatException nfe){
            Log.e(TAG,"received data is inappropiate");
        }
    }

    public void message(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }

    public void on_disconnect(View v) {
        if (btSocket != null) {
            try {
                btSocket.close();
                message("Disconnected");
                Intent intent = new Intent(ControlActivity.this, BlueActivity.class);
                startActivity(intent);
            } catch (IOException e) {
                message("Error while disconnecting");
            }
        }
    }

    public void on_on(View v) {
        String digit = "1";
        ConnectedThread connectedThread = new ConnectedThread(btSocket);
        connectedThread.write(digit.getBytes());
    }

    public void on_off(View v) {
        String digit = "3";
        ConnectedThread connectedThread = new ConnectedThread(btSocket);
        connectedThread.write(digit.getBytes());
    }

    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
            TextView x = findViewById(R.id.X);
            TextView y = findViewById(R.id.Y);
            TextView z = findViewById(R.id.Z);

            for (int i = 0; i < 3; i++) {
                grav[i] = event.values[i];
                g[i] = String.format("%.2f", grav[i]);
            }
            double g_vec = Math.sqrt(grav[0] * grav[0] + grav[1] * grav[1] + grav[2] * grav[2]);
            g[3] = String.format("%.2f", g_vec);
            x.setText(g[0]);
            y.setText(g[1]);
            z.setText(g[2]);

            /*if(connected) {
                ConnectedThread connectedThread = new ConnectedThread(btSocket);

                DecimalFormat df=new DecimalFormat();
                df.setMaximumFractionDigits(2);
                float accZ=0.00f;
                accZ=grav[2];

                if(accZ>accZprev*1.1 || accZ<=accZprev*0.9) {
                    connectedThread.write(("<" + accZ + ">").getBytes());
                    accZprev=accZ;
                }
            }*/

        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, sensorGrav, SensorManager.SENSOR_DELAY_NORMAL);

        /*btDevice=btAdapter.getRemoteDevice(mac);
        try{
            btSocket=btDevice.createRfcommSocketToServiceRecord(uuid);
        }catch(IOException e){
            Log.e(TAG,"error while on resume btDevice");
        }try{
            btSocket.connect();
        }catch(IOException e2){
            Log.e(TAG,"error while connect() btSocket");
        }
        manageMyConnectedSocket(btSocket);*/
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this, sensorGrav);
        /*try{
            btSocket.close();
        }catch(IOException e){
            Log.e(TAG,"on pause close() error");
        }*/
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
