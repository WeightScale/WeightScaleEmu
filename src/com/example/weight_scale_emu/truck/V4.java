package com.example.weight_scale_emu.truck;

import android.content.Context;
import com.example.weight_scale_emu.Preferences;
import com.example.weight_scale_emu.Scale;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Kostya on 18.06.2015.
 */
public class V4 extends TruckScale {
    /**
     * Номер версии
     */
    final static String TAG = "4";

    public V4(Context context) {
        super(context);
    }

    {
        commands.put(STR_VRS, new CommandRunnable() {
            @Override
            public void run() {
                sender.send(STR_VRS + TruckScale.TAG + TAG);
            }

            @Override
            public void run(String value) {
            }
        });
        commands.put(STR_GCO, new CommandRunnable() {
            @Override
            public void run() {
                sender.send(STR_GCO + Scale.adc_offset);
            }

            @Override
            public void run(String value) {
            }
        });
        commands.put(STR_SGD, new CommandRunnable() {
            @Override
            public void run(String value) {
                new Preferences(mContext.getSharedPreferences(Preferences.PREF_EEPROM, Context.MODE_PRIVATE)).write(STR_SGD, value);
                sender.send(STR_SGD);
            }

            @Override
            public void run() {
                sender.send(STR_SGD + new Preferences(mContext.getSharedPreferences(Preferences.PREF_EEPROM, Context.MODE_PRIVATE)).read(STR_SGD, ""));
            }
        });
        commands.put(STR_UGD, new CommandRunnable() {
            @Override
            public void run(String value) {
                new Preferences(mContext.getSharedPreferences(Preferences.PREF_EEPROM, Context.MODE_PRIVATE)).write(STR_UGD, value);
                sender.send(STR_UGD);
            }

            @Override
            public void run() {
                sender.send(STR_UGD + new Preferences(mContext.getSharedPreferences(Preferences.PREF_EEPROM, Context.MODE_PRIVATE)).read(STR_UGD, ""));
            }
        });
        commands.put(STR_PGD, new CommandRunnable() {
            @Override
            public void run(String value) {
                new Preferences(mContext.getSharedPreferences(Preferences.PREF_EEPROM, Context.MODE_PRIVATE)).write(STR_PGD, value);
                sender.send(STR_PGD);
            }

            @Override
            public void run() {
                sender.send(STR_PGD + new Preferences(mContext.getSharedPreferences(Preferences.PREF_EEPROM, Context.MODE_PRIVATE)).read(STR_PGD, ""));
            }
        });
        commands.put(STR_PHN, new CommandRunnable() {
            @Override
            public void run(String value) {
                new Preferences(mContext.getSharedPreferences(Preferences.PREF_EEPROM, Context.MODE_PRIVATE)).write(STR_PHN, value);
                sender.send(STR_PHN);
            }

            @Override
            public void run() {
                sender.send(STR_PHN + new Preferences(mContext.getSharedPreferences(Preferences.PREF_EEPROM, Context.MODE_PRIVATE)).read(STR_PHN, ""));
            }
        });
        commands.put(STR_SCO, new CommandRunnable() {
            @Override
            public void run(String value) {

            }

            @Override
            public void run() {
                Scale.setOffset();
                sender.send(STR_SCO);
            }
        });
        commands.put(STR_DCO, new CommandRunnable() {
            @Override
            public void run(String value) {

            }

            @Override
            public void run() {
                sender.send(STR_DCO + String.valueOf((Scale.sensor_tenzo - Scale.adc_offset)));
            }
        });
    }

    /**
     * Имя версии в текстовом виде.
     *
     * @return Версия в текстовом виде.
     */
    public String toString() {
        return TruckScale.TAG + TAG;
    }

}
