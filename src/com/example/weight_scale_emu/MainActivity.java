package com.example.weight_scale_emu;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;
import android.widget.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {
    private BluetoothAdapter bluetooth; //блютуз адаптер
    private BroadcastReceiver broadcastReceiver; //приёмник намерений
    private Scale scale;
    private ImageButton buttonOn;
    private ImageButton imageButtonLed;
    private LinearLayout linearLayoutScales;
    private TextView textViewLog;
    private Spinner spinnerVersions;
    private Vibrator vibrator; //вибратор
    private Versions version;

    /** Дельта -40С и 100С */
    private int DELTA_SENSOR_TEMPERATURE = 812965;
    /** Показания датчика температуры -40С */
    private int MIN_SENSOR_TEMPERATURE = 9741613;
    private boolean flag_connect = false;
    private boolean flag_run = false;
    /** Контейнер версий весов */
    Map<String,Versions> mapVersions = new HashMap<>();
    {
        mapVersions.put("WeightScale1",new com.example.weight_scale_emu.truck.V1(this));
        mapVersions.put("WeightScale4",new com.example.weight_scale_emu.truck.V4(this));
        mapVersions.put("CraneScale1",new com.example.weight_scale_emu.crane.V1(this));
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        linearLayoutScales = (LinearLayout) findViewById(R.id.linearLayoutScales);
        linearLayoutScales.setVisibility(LinearLayout.GONE);

        spinnerVersions = (Spinner)findViewById(R.id.spinnerVersions);

        buttonOn = (ImageButton) findViewById(R.id.button_on);
        imageButtonLed = (ImageButton) findViewById(R.id.imageButton_led);
        imageButtonLed.setBackgroundDrawable(getResources().getDrawable(R.mipmap.circle_green));
        imageButtonLed.setVisibility(ImageButton.INVISIBLE);
        buttonOn.setOnTouchListener(OnTouchListenerOn);

        setupSpinner();
    }

    private final View.OnTouchListener OnTouchListenerOn = new View.OnTouchListener() {
        final TimerScalesOn timerScalesOn = new TimerScalesOn(3000, 100);

        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        @Override
        public boolean onTouch(View v, MotionEvent event) {

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    //imageButtonLed.setBackgroundColor(getResources().getColor(R.color.color_scales));
                    imageButtonLed.setBackgroundDrawable(getResources().getDrawable(R.mipmap.circle_green));
                    imageButtonLed.setVisibility(ImageButton.VISIBLE);
                    if(flag_run)
                        timerScalesOn.onStart();
                    break;
                case MotionEvent.ACTION_UP:
                    timerScalesOn.cancel();
                    //if (!timerScalesOn.isFinish()) {
                        buttonOn.setOnTouchListener(OnTouchListenerOff);
                        runScales();
                    //}

                    break;
                default:
            }
            return true;
        }
    };

    public class TimerScalesOn extends CountDownTimer {
        private boolean finish = true;

        public TimerScalesOn(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        public void onStart() {
            finish = false;
            start();
        }

        @Override
        public void onFinish() {
            //imageButtonLed.setBackgroundColor(getResources().getColor(R.color.color_bootloader));
            imageButtonLed.setBackgroundDrawable(getResources().getDrawable(R.mipmap.circle_blue));
            finish = true;
        }

        public boolean isFinish() {
            return finish;
        }

        public void onTick(long millisUntilFinished) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    toggle(imageButtonLed);
                }
            });
        }
    }

    private final View.OnTouchListener OnTouchListenerOff = new View.OnTouchListener() {
        final TimerScalesOff timerScalesOff = new TimerScalesOff(6000, 150);

        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        @Override
        public boolean onTouch(View v, MotionEvent event) {

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    timerScalesOff.onStart();
                    break;
                case MotionEvent.ACTION_UP:
                    if (timerScalesOff.isFinish()) {
                        offScales();
                    } else {
                        timerScalesOff.cancel();
                        imageButtonLed.setVisibility(ImageButton.VISIBLE);
                    }
                    break;
                default:
            }
            return true;
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        android.util.Log.e("TrackingFlow", "Creating thread to start listening...");
        //new Thread(reader).start();
    }

    void log(int resource) { //для ресурсов
        textViewLog.setText(getString(resource) + "\n" + textViewLog.getText());
    }

    void log(String string) { //для текста
        textViewLog.setText(string + "\n" + textViewLog.getText());
    }

    void log(String string, boolean toast) { //для текста
        textViewLog.setText(string + "\n" + textViewLog.getText());
        if (toast)
            Toast.makeText(this, string, Toast.LENGTH_SHORT).show();
    }

    void log(int resource, boolean toast) { //для текста
        textViewLog.setText(getString(resource) + "\n" + textViewLog.getText());
        if (toast)
            Toast.makeText(this, resource, Toast.LENGTH_SHORT).show();
    }

    void log(int resource, String str) { //для ресурсов с текстовым дополнением
        textViewLog.setText(getString(resource) + " " + str + "\n" + textViewLog.getText());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (broadcastReceiver != null)
            try {
                unregisterReceiver(broadcastReceiver);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }

        if (bluetooth != null) {
            if (bluetooth.isDiscovering())
                bluetooth.cancelDiscovery();
            bluetooth.disable();
        }

        if (scale != null) {
            scale.cancelAcceptThread(false);
            scale.disconnect();
        }
    }

    public class TimerScalesOff extends CountDownTimer {
        private boolean start = false;
        private boolean finish = true;

        public TimerScalesOff(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        public void onStart() {
            start = true;
            finish = false;
            start();
        }

        @Override
        public void onFinish() {
            start = false;
            finish = true;
            imageButtonLed.setVisibility(ImageButton.INVISIBLE);
        }

        public boolean isFinish() {
            return finish;
        }

        public void onTick(long millisUntilFinished) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    toggle(imageButtonLed);
                }
            });
        }

        boolean isStart() {
            return start;
        }

    }

    void toggle(ImageButton imageButton) {
        if (imageButton.isShown())
            imageButton.setVisibility(ImageButton.INVISIBLE);
        else
            imageButton.setVisibility(ImageButton.VISIBLE);
    }

    void runScales() {
        flag_run = true;
        imageButtonLed.setBackgroundDrawable(getResources().getDrawable(R.mipmap.circle_green));
        //imageButtonLed.setVisibility(ImageButton.VISIBLE);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        textViewLog = (TextView) findViewById(R.id.textLog);

        broadcastReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) { //обработчик Bluetooth'а
                String action = intent.getAction();
                if (action != null) {
                    if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                        if (bluetooth.getState() == BluetoothAdapter.STATE_OFF) {
                            log(R.string.bluetooth_off);
                            bluetooth.enable();
                        } else if (bluetooth.getState() == BluetoothAdapter.STATE_TURNING_ON) {
                            log(R.string.bluetooth_turning_on, true);
                        } else if (bluetooth.getState() == BluetoothAdapter.STATE_TURNING_OFF) {
                            bluetooth.enable();
                        } else if (bluetooth.getState() == BluetoothAdapter.STATE_ON) {
                            log(R.string.bluetooth_on, true);
                            //scale.connect();
                        }
                    } else if (action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) { //устройство отсоеденено
                        vibrator.vibrate(200);
                        flag_connect = false;
                        scale.disconnect();
                        //scale.connect();
                        log(R.string.bluetooth_disconnected);
                    } else if (action.equals(BluetoothDevice.ACTION_ACL_CONNECTED)) { //найдено соеденено
                        vibrator.vibrate(200);
                        flag_connect = true;
                        log(R.string.bluetooth_connected);
                    } else if (action.equals("connectSocket")) {
                        scale.connect();
                    }
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        intentFilter.addAction("android.intent.extra.MEDIA_BUTTON");
        intentFilter.addAction("connectSocket");
        registerReceiver(broadcastReceiver, intentFilter);

        bluetooth = BluetoothAdapter.getDefaultAdapter();
        if (bluetooth != null)
            if (!bluetooth.isEnabled()) {
                log(R.string.bluetooth_off, true);
                bluetooth.enable();
            } else
                log(R.string.bluetooth_on, true);
        else {
            Toast.makeText(this, R.string.bluetooth_no, Toast.LENGTH_SHORT).show();
            finish();
        }


        Button buttonDiscoverable = (Button) findViewById(R.id.button_discoverable);
        buttonDiscoverable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bluetooth.isDiscovering()) {
                    bluetooth.cancelDiscovery();
                }
                if (flag_connect) {
                    scale.disconnect();
                }

                Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                startActivity(discoverableIntent);
            }
        });
        SeekBar seekBarBattery = (SeekBar) findViewById(R.id.seekBar_battery);
        Scale.sensor_battery = new Preferences(getSharedPreferences(Preferences.PREF_EEPROM, Context.MODE_PRIVATE)).read(Preferences.KEY_SENSOR_BAT, 0);
        seekBarBattery.setProgress(Scale.sensor_battery);
        seekBarBattery.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Scale.setSensorBattery(progress);
                new Preferences(getSharedPreferences(Preferences.PREF_EEPROM, Context.MODE_PRIVATE)).write(Preferences.KEY_SENSOR_BAT, progress);
                //int max = new Preferences(getSharedPreferences(Preferences.PREF_EEPROM, Context.MODE_PRIVATE)).read(Preferences.KEY_MAX_ADC_BAT, Scale.max_adc_bat);
                //int min = new Preferences(getSharedPreferences(Preferences.PREF_EEPROM, Context.MODE_PRIVATE)).read(Preferences.KEY_MIN_ADC_BAT, Scale.min_adc_bat);
                //float step = ((float) max - (float) min) / 1024;
                //int value = (int) ((float) progress * step);
                //Scale.setBattery(value + min);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }

        });

        SeekBar seekBarSensor = (SeekBar) findViewById(R.id.seekBar_sensor);
        Scale.sensor_tenzo = new Preferences(getSharedPreferences(Preferences.PREF_EEPROM, Context.MODE_PRIVATE)).read(Preferences.KEY_SENSOR_TENZO, 0);
        seekBarSensor.setProgress(Scale.sensor_tenzo);
        seekBarSensor.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Scale.setSensorTenzo(progress);
                new Preferences(getSharedPreferences(Preferences.PREF_EEPROM, Context.MODE_PRIVATE)).write(Preferences.KEY_SENSOR_TENZO, progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        SeekBar seekBarTemp = (SeekBar) findViewById(R.id.seekBar_temp);
        seekBarTemp.setMax(DELTA_SENSOR_TEMPERATURE);
        Scale.setSensorTemp(MIN_SENSOR_TEMPERATURE + new Preferences(getSharedPreferences(Preferences.PREF_EEPROM, Context.MODE_PRIVATE)).read(Preferences.KEY_SENSOR_TEMP, 0));
        seekBarTemp.setProgress(new Preferences(getSharedPreferences(Preferences.PREF_EEPROM, Context.MODE_PRIVATE)).read(Preferences.KEY_SENSOR_TEMP, 0));
        seekBarTemp.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Scale.setSensorTemp(MIN_SENSOR_TEMPERATURE + progress);
                new Preferences(getSharedPreferences(Preferences.PREF_EEPROM, Context.MODE_PRIVATE)).write(Preferences.KEY_SENSOR_TEMP, progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        while (!bluetooth.isEnabled()) ;
        bluetooth.cancelDiscovery();

        linearLayoutScales.setVisibility(LinearLayout.VISIBLE);

        scale = new Scale(getApplicationContext(), bluetooth, version);
        scale.connect();
    }

    void offScales() {
        linearLayoutScales.setVisibility(ImageButton.GONE);
        imageButtonLed.setVisibility(ImageButton.INVISIBLE);
        buttonOn.setOnTouchListener(OnTouchListenerOn);

        if (bluetooth.isDiscovering())
            bluetooth.cancelDiscovery();
        unregisterReceiver(broadcastReceiver);
        //mainThread.cancel(true);
        //while (!mainThread.closed);
        //test.interrupt();
        scale.cancelAcceptThread(false);
        scale.disconnect();
        bluetooth.disable();
        flag_run = false;
    }

    void setupSpinner(){
        Collection<Versions> collection = mapVersions.values();
        Versions[] array = collection.toArray(new Versions[collection.size()]);
        final ArrayAdapter<Versions> dataAdapter = new ArrayAdapter<Versions>(this, R.layout.type_spinner, array);
        dataAdapter.setDropDownViewResource(R.layout.type_spinner_dropdown_item);
        spinnerVersions.setAdapter(dataAdapter);
        spinnerVersions.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                version = dataAdapter.getItem(i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }

        });
    }
}
