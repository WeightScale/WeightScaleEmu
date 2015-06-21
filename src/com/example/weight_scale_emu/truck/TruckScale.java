package com.example.weight_scale_emu.truck;

import android.content.Context;
import com.example.weight_scale_emu.Scale;
import com.example.weight_scale_emu.Versions;

import com.example.weight_scale_emu.Preferences;

/**
 * Created by Kostya on 17.06.2015.
 */
public abstract class TruckScale extends Versions {
    /**
     * Имя версии.
     */
    final static String TAG = "WeightScales";

    {
        commands.put(STR_FAD, new CommandRunnable() {
            @Override
            public void run() {
                sender.send(STR_FAD + String.valueOf(new Preferences(mContext.getSharedPreferences(Preferences.PREF_EEPROM, Context.MODE_PRIVATE)).read(STR_FAD, 15)));
            }

            @Override
            public void run(String value) {
                try {
                    int filter = Integer.parseInt(value);
                    if (filter > 15)
                        filter = 15;
                    new Preferences(mContext.getSharedPreferences(Preferences.PREF_EEPROM, Context.MODE_PRIVATE)).write(STR_FAD, filter);
                    sender.send(STR_FAD);
                }catch (Exception e){}
            }
        });
        commands.put(STR_TOF, new CommandRunnable() {
            @Override
            public void run() {
                sender.send(STR_TOF + String.valueOf(new Preferences(mContext.getSharedPreferences(Preferences.PREF_EEPROM, Context.MODE_PRIVATE)).read(STR_TOF, 600) / 60));
            }

            @Override
            public void run(String value) {
                try {
                    int time = Integer.parseInt(value) * 60;
                    new Preferences(mContext.getSharedPreferences(Preferences.PREF_EEPROM, Context.MODE_PRIVATE)).write(STR_TOF, time);
                    Scale.pwr_time = time;
                    sender.send(STR_TOF);
                }catch (Exception e){}
            }
        });
        commands.put(STR_DAT, new CommandRunnable() {
            @Override
            public void run(String value) {
                new Preferences(mContext.getSharedPreferences(Preferences.PREF_EEPROM, Context.MODE_PRIVATE)).write(STR_DAT, value);
                sender.send(STR_DAT);
            }

            @Override
            public void run() {
                sender.send(STR_DAT + new Preferences(mContext.getSharedPreferences(Preferences.PREF_EEPROM, Context.MODE_PRIVATE)).read(STR_DAT, ""));
            }
        });
        commands.put(STR_GBT, new CommandRunnable() {
            @Override
            public void run(String value) { }

            @Override
            public void run() {
                int adc = Scale.sensor_battery;
                Scale.autoCalibrationBattery(adc);
                //(unsigned char)((float)(adc - battery->min_adc_bat)/battery->const_batery);
                String str = String.valueOf(Scale.batteryMatch(adc));
                sender.send(STR_GBT + str);
            }
        });
        commands.put(STR_DTM, new CommandRunnable() {
            @Override
            public void run(String value) {

            }

            @Override
            public void run() {
                sender.send(STR_DTM + String.valueOf(Scale.sensor_temp));
            }
        });
        commands.put(STR_CTM, new CommandRunnable() {
            @Override
            public void run(String value) {
                Scale.const_temp = Float.parseFloat(value);
                new Preferences(mContext.getSharedPreferences(Preferences.PREF_EEPROM, Context.MODE_PRIVATE)).write(STR_CTM, Scale.const_temp);
                sender.send(STR_CTM);
            }

            @Override
            public void run() {
                sender.send(String.valueOf(Scale.const_temp));
            }
        });
    }

    TruckScale(Context context) { super(context); }

}
