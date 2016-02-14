package com.example.weight_scale_emu;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Kostya on 17.06.2015.
 */
public abstract class Versions implements InterfaceScale {
    public Context mContext;
    public Scale.InterfaceSender sender;
    /**
     * Контейнер команд.
     */
    public Map<String, CommandRunnable> commands = new HashMap<>();

    {
        commands.put(STR_DCH, new CommandRunnable() {
            @Override
            public void run(String value) {
            }

            @Override
            public void run() {
                String str = String.valueOf(Scale.sensor_tenzo);
                sender.send(STR_DCH + str);
            }
        });
        commands.put(STR_BST, new CommandRunnable() {
            @Override
            public void run(String value) {
                int b = Integer.parseInt(value);
                if (b < Scale.BAUD_NUM) {
                    new Preferences(mContext.getSharedPreferences(Preferences.PREF_EEPROM, Context.MODE_PRIVATE)).write(STR_BST, b);
                    sender.send(STR_BST);
                }
            }

            @Override
            public void run() {
                sender.send(STR_BST + String.valueOf(new Preferences(mContext.getSharedPreferences(Preferences.PREF_EEPROM, Context.MODE_PRIVATE)).read(STR_BST, 0)));
            }
        });
        commands.put(STR_CBT, new CommandRunnable() {
            @Override
            public void run(String value) {
                int adc = 0;
                double f;
                int cbt = Integer.parseInt(value);
                if (cbt > 100)
                    cbt = 100;
                int i;
                for (i = 0; i < 8; i++)
                    adc += Scale.sensor_battery;
                adc /= i;
                Scale.max_adc_bat = adc;
                f = Scale.MAX_BAT / (((Scale.CONST_BAT * cbt) + Scale.MIN_BAT) / Scale.max_adc_bat);
                if (f > 1024)
                    f = 1024;
                Scale.max_adc_bat = (int) f;

                Scale.const_bat = (float) Scale.MAX_BAT / (float) Scale.max_adc_bat;
                Scale.min_adc_bat = (int) (Scale.MIN_BAT / Scale.const_bat);
                Scale.const_bat = ((float) Scale.max_adc_bat - (float) Scale.min_adc_bat) / 100;

                new Preferences(mContext.getSharedPreferences(Preferences.PREF_EEPROM, Context.MODE_PRIVATE)).write(Preferences.KEY_MAX_ADC_BAT, Scale.max_adc_bat);
                new Preferences(mContext.getSharedPreferences(Preferences.PREF_EEPROM, Context.MODE_PRIVATE)).write(Preferences.KEY_MIN_ADC_BAT, Scale.min_adc_bat);
                new Preferences(mContext.getSharedPreferences(Preferences.PREF_EEPROM, Context.MODE_PRIVATE)).write(Preferences.KEY_CONST_BAT, Scale.const_bat);

                //Scale.write(STR_CBT+CR+LF);
                sender.send(STR_CBT);
            }

            @Override
            public void run() {

            }
        });
    }

    protected Versions(Context context) {
        mContext = context;
    }

    /**
     * Устававливает интерфейс отправителя команд.
     *
     * @param i Интерфейс.
     */
    public void setInterfaceSender(Scale.InterfaceSender i) {
        sender = i;
    }

    /**
     * Выполнить команду.
     *
     * @param cmd Команда.
     */
    public void execute(String cmd) {
        StringBuilder str = new StringBuilder(cmd);
        if (commands.containsKey(str.substring(0, 3))) {
            CommandRunnable command = commands.get(str.substring(0, 3));
            String value = str.replace(0, 3, "").toString();
            if (value.isEmpty())
                command.run();
            else
                command.run(value);
        }
        /*for (Map.Entry<String, CommandRunnable> command : commands.entrySet()){
            if (cmd.contains(command.getKey())){
                String value = str.replace(0, command.getKey().length(),"").toString();
                if(value.isEmpty())
                    command.getValue().run();
                else
                    command.getValue().run(value);
                break;
            }
        }*/
    }

    /**
     * Абстрактный клас обработчика команд.
     */
    public abstract class CommandRunnable implements Runnable {

        public abstract void run(String value);
    }

}
