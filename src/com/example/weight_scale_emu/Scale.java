package com.example.weight_scale_emu;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.UUID;

/**
 * Created by Kostya on 29.08.14.
 */
public class Scale {
    private final BluetoothAdapter adapter;
    private AcceptThread acceptThread;
    static final String BTM_NAME = "SCALES";
    static final String SCALE_VERSION = "WeightScales4";
    private final Context context;
    private final UshellTask ushellTask;
    int max_adc_bat;
    int min_adc_bat;
    float const_bat;
    int pwr_time;
    int adc_offset;
    float const_temp;

    int power_time;

    private int battery;

    private int sensor_battery;
    private int sensor_tenzo;
    private int sensor_temp;

    static final double MIN_BAT = 3.15;
    static final double MAX_BAT = 4.15;
    static final float CONST_BAT = (float) (((float) MAX_BAT - (float) MIN_BAT) / 100);

    Scale(Context c, BluetoothAdapter bluetoothAdapter) {
        context = c;
        adapter = bluetoothAdapter;
        ushellTask = new UshellTask(context, Scale.this);
    }

    void connect() {
        acceptThread = new AcceptThread();
        acceptThread.execute();
    }

    public void send(String str) {
        if (acceptThread != null)
            acceptThread.write(str);
    }

    void disconnect() {
        if (acceptThread != null)
            acceptThread.close();
    }

    private class AcceptThread extends AsyncTask<Void, Void, Boolean> {
        private boolean closed = true;
        BluetoothAdapter adapter;
        private BluetoothServerSocket mmServerSocket;
        BluetoothSocket socket;
        private InputStream mmInStream;
        private OutputStreamWriter mmOutputStreamWriter;
        final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            adapter = BluetoothAdapter.getDefaultAdapter();
            closed = false;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            try {
                adapter.cancelDiscovery();
                mmServerSocket = adapter.listenUsingRfcommWithServiceRecord("WeightScale", uuid);
                socket = mmServerSocket.accept();

                if (socket != null) {
                    mmServerSocket.close();
                    mmInStream = socket.getInputStream();
                    mmOutputStreamWriter = new OutputStreamWriter(socket.getOutputStream());

                    while (!isCancelled()) {
                        try {
                            byte byteRead = (byte) mmInStream.read();
                            ushellTask.buildCommand(byteRead);
                        } catch (IOException e) {
                            e.printStackTrace();
                            if (socket != null)
                                try {
                                    socket.close();
                                    socket = null;
                                } catch (IOException e1) {
                                    e1.printStackTrace();
                                }
                            break;
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();//todo
                if (isCancelled())
                    return false;
            }
            if (isCancelled())
                return false;
            closed = true;
            return true;
        }

        @Override
        protected void onPostExecute(Boolean b) {
            super.onPostExecute(b);
            if (b)
                context.sendBroadcast(new Intent("connectSocket"));
        }

        public void write(String bytes) {
            try {
                mmOutputStreamWriter.write(bytes);
                mmOutputStreamWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void close() {
            try {
                if (mmServerSocket != null)
                    mmServerSocket.close();
                if (socket != null)
                    socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    void cancelAcceptThread(boolean b) {
        if (acceptThread != null) {
            acceptThread.cancel(b);
        }
    }

    synchronized int getSensorBattery() {
        return sensor_battery;
    }

    synchronized int getBattery() {
        return battery;
    }

    synchronized int getSensorTenzo() {
        return sensor_tenzo;
    }

    synchronized int getSensorTemp() {
        return sensor_temp;
    }

    synchronized void setSensorBattery(int b) {
        sensor_battery = b;
    }

    synchronized void setBattery(int b) {
        battery = b;
    }

    synchronized void setSensorTenzo(int tenzo) {
        sensor_tenzo = tenzo;
    }

    synchronized void setSensorTemp(int temp) {
        sensor_temp = temp;
    }

    /*boolean isConnected(){
        if(acceptThread.socket!=null)
            return acceptThread.socket.isConnected();
        return false;
    }*/
}
