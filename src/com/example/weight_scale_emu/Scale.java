package com.example.weight_scale_emu;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import org.apache.http.util.ByteArrayBuffer;

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
    private static Context context;
    //private final UshellTask ushellTask;
    private final Versions version;
    private final ByteArrayBuffer command = new ByteArrayBuffer(0);
    static int max_adc_bat;
    static int min_adc_bat;
    static float const_bat;
    public static int pwr_time;
    public static int adc_offset;
    public static float const_temp;

    int power_time;

    public static int battery;

    public static int sensor_battery;
    public static int sensor_tenzo;
    public static int sensor_temp;

    public static final int BAUD_NUM = 6;
    private final String CR_LF = "\r\n";
    private final int CR = 0x0D;
    private final int LF = 0x0A;

    static final double MIN_BAT = 3.15;
    static final double MAX_BAT = 4.15;
    static final float CONST_BAT = (float) (((float) MAX_BAT - (float) MIN_BAT) / 100);

    Scale(Context c, BluetoothAdapter bluetoothAdapter, Versions versions) {
        context = c;
        adapter = bluetoothAdapter;
        version = versions;
        version.setInterfaceSender(interfaceSender);
        //ushellTask = new UshellTask(context, this, interfaceScale);
    }

    public interface InterfaceSender {
        void send(String cmd);
    }

    protected void connect() {
        acceptThread = new AcceptThread();
        acceptThread.execute();
    }

    public void sendCommand(String cmd) {
        send(cmd + CR_LF);
    }

    public void send(String str) {
        if (acceptThread != null)
            acceptThread.write(str);
    }

    protected void disconnect() {
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
                            buildCommand(byteRead);
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
                context.sendBroadcast(new Intent(MainActivity.CONNECT_SOCKET));
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

    /**
     * Построитель команды
     *
     * @param b байт команды
     */
    private synchronized void buildCommand(byte b) {

        switch (b) {
            case CR:
                //command.append((byte)0);//Add NULL char
                break;
            case LF:
                String str = new String(command.toByteArray());
                parseCommand(str);
                command.clear();
                break;
            default:
                command.append(b);
                break;
        }
    }

    protected void cancelAcceptThread(boolean b) {
        if (acceptThread != null) {
            acceptThread.cancel(b);
        }
    }

    protected static synchronized void setSensorBattery(int b) {
        sensor_battery = b;
    }

    protected static synchronized void setSensorTenzo(int tenzo) {
        sensor_tenzo = tenzo;
    }

    protected static synchronized void setSensorTemp(int temp) {
        sensor_temp = temp;
    }

    private void parseCommand(String cmd) {
        version.execute(cmd);
    }

    /**
     * Интерфейс для отправки комманд
     */
    private InterfaceSender interfaceSender = new InterfaceSender() {
        @Override
        public void send(String cmd) {
            sendCommand(cmd);
        }
    };

    /**
     * Автоматическая каллибровка батареи
     *
     * @param adc Значение АЦП батареи
     */
    public static void autoCalibrationBattery(int adc) {
        //float f;
        if (adc > max_adc_bat) {
            max_adc_bat = adc;
            const_bat = (float) MAX_BAT / (float) max_adc_bat;
            min_adc_bat = (int) (MIN_BAT / const_bat);
            const_bat = ((float) max_adc_bat - (float) min_adc_bat) / 100;

            new Preferences(context.getSharedPreferences(Preferences.PREF_EEPROM, Context.MODE_PRIVATE)).write(Preferences.KEY_MIN_ADC_BAT, min_adc_bat);
            new Preferences(context.getSharedPreferences(Preferences.PREF_EEPROM, Context.MODE_PRIVATE)).write(Preferences.KEY_CONST_BAT, const_bat);
        }
    }

    /**
     * Расчет заряда батареи
     *
     * @param adc Значение АЦП батареи.
     * @return Значение в процентах заряд батареи.
     */
    public static int batteryMatch(int adc) {
        if (adc < min_adc_bat)
            adc = min_adc_bat;
        else if (adc > max_adc_bat) {
            autoCalibrationBattery(adc);
        }
        return (int) ((float) (adc - min_adc_bat) / const_bat);
    }

    /**
     * Установить офсет датчика веса.
     */
    public static void setOffset() {
        int i;
        int adc = 0;

        for (i = 0; i < 2; i++) {
            adc += sensor_tenzo;
        }
        adc_offset = (adc / i);
    }

}
