package com.example.weight_scale_emu;

import android.content.Context;
import org.apache.http.util.ByteArrayBuffer;

/**
 * Created by Kostya on 28.08.14.
 */
public class UshellTask {

    private final Context context;
    private final Scale scale;
    private final ByteArrayBuffer command = new ByteArrayBuffer(0);
    private enumCommand commandType = enumCommand.CMD_NONE;
    private boolean parameterFlag = false;

    private final int CR = 0x0D;
    private final int LF = 0x0A;
    private final String CR_LF = "\r\n";
    private final int CTRL_C = 0x03;
    private final int ABORT_CHAR = CTRL_C;
    /**Установить фильтер ацп*/
    static String STR_FAD = "FAD";
    /**Время отключения питания*/
    static String STR_TOF = "TOF";
    /**калибровка акумулятора весов в процетах*/
    static String STR_CBT = "CBT";
    /**получить заряд аккумулятора весов*/
    static String STR_GBT = "GBT";
    /**Имя Bluetooth*/
    static String STR_SNA = "SNA";
    /**Скорость Baud rate*/
    static String STR_BST = "BST";
    /**Версия весов*/
    static String STR_VRS = "VRS";
    /**получить АЦП канал*/
    static String STR_DCH = "DCH";
    /**получить АЦП канал минус offset*/
    static String STR_DCO = "DCO";
    /**установить  offset*/
    static String STR_SCO = "SCO";
    /**получить АЦП температуры*/
    static String STR_DTM = "DTM";
    /**получить АЦП температуры*/
    static String STR_CTM = "CTM";
    /**получить/записать данные о калибровке и т.п.*/
    static String STR_DAT = "DAT";
    /**получить/записать данные о таблице google disc.*/
    static String STR_SGD = "SGD";
    /**получить/записать данные о user google disc.*/
    static String STR_UGD = "UGD";
    /**получить/записать данные о password google disc.*/
    static String STR_PGD = "PGD";   //

    private static final int BAUD_NUM = 6;

    UshellTask(Context context, Scale s) {
        this.context = context;
        scale = s;
    }

    public enum enumCommand {
        /**Нет команды*/
        CMD_NONE,
        /**Фильтр АЦП 0-15*/
        CMD_FAD,
        /**Время отключения*/
        CMD_TOF,
        /**Калибровка батареи*/
        CMD_CBT,
        /**Заряд батареи*/
        CMD_GBT,
        /**Имя Bluetooth*/
        CMD_SNA,
        /**Скорость Baund rate*/
        CMD_BST,
        /**версия весов*/
        CMD_VRS,
        /**получить АЦП канала*/
        CMD_DCH,
        /**получить АЦП канала минус offset*/
        CMD_DCO,
        /**установить offset*/
        CMD_SCO,
        /**получить АЦП температуры*/
        CMD_DTM,
        /**калибровка температуры*/
        CMD_CTM,
        /**Данные настроек весов*/
        CMD_DAT,
        /**Таблица Spreadsheet google disk*/
        CMD_SGD,
        /**Пользователь Google*/
        CMD_UGD,
        /**Пароль Google*/
        CMD_PGD,
        TIMEOUT
    }

    void test(String command) {
        byte[] buffer = command.getBytes();

        for (byte aBuffer : buffer) {
            buildCommand(aBuffer);
        }
    }

    synchronized void buildCommand(byte b) {

        switch (b) {
            case CR:
                //command.append((byte)0);//Add NULL char
                break;
            case ABORT_CHAR:    //^c abort cmd
                command.clear();
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

    void parseCommand(String cmd) {
        //cmd = true;
        //Decode command type
        if (cmd.contains(STR_VRS))
            commandType = enumCommand.CMD_VRS;
        else if (cmd.contains(STR_FAD))    //Фильтер ацп
            commandType = enumCommand.CMD_FAD;
        else if (cmd.contains(STR_TOF))    //Время отключения питания
            commandType = enumCommand.CMD_TOF;
        else if (cmd.contains(STR_CBT))    //калибровка акумулятора
            commandType = enumCommand.CMD_CBT;
        else if (cmd.contains(STR_GBT))    //получить заряд батареи
            commandType = enumCommand.CMD_GBT;
        else if (cmd.contains(STR_SNA))    //Имя Bluetooth соединения
            commandType = enumCommand.CMD_SNA;
        else if (cmd.contains(STR_BST))    //Скорость Baund rate
            commandType = enumCommand.CMD_BST;
        else if (cmd.contains(STR_VRS))    //Имя версии весов
            commandType = enumCommand.CMD_VRS;
        else if (cmd.contains(STR_DCH))    //Данные весового датчика
            commandType = enumCommand.CMD_DCH;
        else if (cmd.contains(STR_DCO))    //Данные весового датчика минус offset
            commandType = enumCommand.CMD_DCO;
        else if (cmd.contains(STR_SCO))    //установить offset
            commandType = enumCommand.CMD_SCO;
        else if (cmd.contains(STR_DTM))    //Данные температурного датчика
            commandType = enumCommand.CMD_DTM;
        else if (cmd.contains(STR_CTM))    //калибровка температурного датчика
            commandType = enumCommand.CMD_CTM;
        else if (cmd.contains(STR_DAT))    //Данные
            commandType = enumCommand.CMD_DAT;
        else if (cmd.contains(STR_SGD))    //Таблица
            commandType = enumCommand.CMD_SGD;
        else if (cmd.contains(STR_UGD))    //Имя
            commandType = enumCommand.CMD_UGD;
        else if (cmd.contains(STR_PGD))    //Пароль
            commandType = enumCommand.CMD_PGD;
        else {
            //cmd = false;
            return;
        }

        //Get first arg (if any)
        String parameter = "";
        if (cmd.length() > 3) {
            parameterFlag = true;
            parameter = cmd.substring(3, cmd.length());
        } else
            parameterFlag = false;

        ushellTask(commandType, parameter);
    }

    synchronized void ushellTask(enumCommand type, String parameter) {
        switch (type) {
            case CMD_VRS:
                scale.send(STR_VRS);
                scale.send(Scale.SCALE_VERSION);
                scale.send(CR_LF);
                break;
            case CMD_CBT:
                int adc = 0;
                double f;
                int cbt = Integer.parseInt(parameter);
                if (cbt > 100)
                    cbt = 100;
                int i;
                for (i = 0; i < 8; i++)
                    adc += scale.getSensorBattery();
                adc /= i;
                scale.max_adc_bat = adc;
                f = Scale.MAX_BAT / (((Scale.CONST_BAT * cbt) + Scale.MIN_BAT) / scale.max_adc_bat);
                if (f > 1024)
                    f = 1024;
                scale.max_adc_bat = (int) f;

                scale.const_bat = (float) Scale.MAX_BAT / (float) scale.max_adc_bat;
                scale.min_adc_bat = (int) (Scale.MIN_BAT / scale.const_bat);
                scale.const_bat = ((float) scale.max_adc_bat - (float) scale.min_adc_bat) / 100;

                new Preferences(context.getSharedPreferences(Preferences.PREF_EEPROM, Context.MODE_PRIVATE)).write(Preferences.KEY_MAX_ADC_BAT, scale.max_adc_bat);
                new Preferences(context.getSharedPreferences(Preferences.PREF_EEPROM, Context.MODE_PRIVATE)).write(Preferences.KEY_MIN_ADC_BAT, scale.min_adc_bat);
                new Preferences(context.getSharedPreferences(Preferences.PREF_EEPROM, Context.MODE_PRIVATE)).write(Preferences.KEY_CONST_BAT, scale.const_bat);

                //Scale.write(STR_CBT+CR+LF);
                scale.send(STR_CBT);
                scale.send(CR_LF);
                break;
            case CMD_FAD:                                                        //Фильтер АЦП 0-15
                scale.send(STR_FAD);
                if (parameterFlag) {
                    int fad = Integer.parseInt(parameter);
                    if (fad > 15)
                        fad = 15;
                    //new Preferences(PreferenceManager.getDefaultSharedPreferences(context)).write(STR_FAD,15);
                    new Preferences(context.getSharedPreferences(Preferences.PREF_EEPROM, Context.MODE_PRIVATE)).write(STR_FAD, fad);
                    //ad7797.bit_reg.mode.FS = (unsigned char)i;//select_filter((unsigned char)i);
                } else {
                    scale.send(String.valueOf(new Preferences(context.getSharedPreferences(Preferences.PREF_EEPROM, Context.MODE_PRIVATE)).read(STR_FAD, 15)));
                }
                scale.send(CR_LF);
                break;
            case CMD_TOF:                                                    //Время отключения питания в режиме бездействия
                scale.send(STR_TOF);
                if (parameterFlag) {
                    int time = Integer.parseInt(parameter) * 60;
                    new Preferences(context.getSharedPreferences(Preferences.PREF_EEPROM, Context.MODE_PRIVATE)).write(STR_TOF, time);
                    scale.pwr_time = time;
                } else {
                    String str = String.valueOf(new Preferences(context.getSharedPreferences(Preferences.PREF_EEPROM, Context.MODE_PRIVATE)).read(STR_TOF, 600) / 60);
                    scale.send(str);
                }
                scale.send(CR_LF);
                break;
            case CMD_GBT: {                                                    //Получить заряд батареи в процентах

                //adc = filter((long int)adc_chanel(7),adc,20,0.1,0.1);
                //adc = Scale.battery;
                adc = scale.getBattery();
                //adc = 20;
                auto_calibration_bat(adc, 100);
                String str = String.valueOf(bat_match(adc));
                scale.send(STR_GBT);
                scale.send(str);
                scale.send(CR_LF);
                break;
            }
            case CMD_BST:                                                   //Установить скорость передачи данных
                scale.send(STR_BST);
                if (parameterFlag) {
                    int b = Integer.parseInt(parameter);
                    if (b < BAUD_NUM) {
                        new Preferences(context.getSharedPreferences(Preferences.PREF_EEPROM, Context.MODE_PRIVATE)).write(STR_BST, b);
                    } else
                        break;
                } else {
                    scale.send(String.valueOf(new Preferences(context.getSharedPreferences(Preferences.PREF_EEPROM, Context.MODE_PRIVATE)).read(STR_BST, 0)));
                }
                scale.send(CR_LF);
                break;
            case CMD_DCH:                                                    // получить данные АЦП канал
                scale.send(STR_DCH);
                String str = String.valueOf(ad7797_single_conversion());
                scale.send(str);
                scale.send(CR_LF);
                break;
            case CMD_SCO:                                                    // установить offset канала на ноль
                set_offset();
                scale.send(STR_SCO);
                scale.send(CR_LF);
                break;
            case CMD_DCO:                                                    //получить данные АЦП канал минус offset
                scale.send(STR_DCO);
                scale.send(String.valueOf(get_data_chanel_offset()));
                scale.send(CR_LF);
                break;
            case CMD_CTM:                                                    //Установить получить калибровку температуры
                scale.send(STR_CTM);
                if (parameterFlag) {
                    scale.const_temp = Float.parseFloat(parameter);
                    new Preferences(context.getSharedPreferences(Preferences.PREF_EEPROM, Context.MODE_PRIVATE)).write(STR_CTM, scale.const_temp);
                } else {
                    scale.send(String.valueOf(scale.const_temp));
                }
                scale.send(CR_LF);
                break;
            case CMD_DTM:                                                    // получить данные АЦП канал температура
                scale.send(STR_DTM);
                scale.send(String.valueOf(ad7797_get_temperature()));
                scale.send(CR_LF);
                break;
            case CMD_DAT:                                                    // получить или записать данные для калибровки весов
                scale.send(STR_DAT);
                if (parameterFlag) {
                    new Preferences(context.getSharedPreferences(Preferences.PREF_EEPROM, Context.MODE_PRIVATE)).write(STR_DAT, parameter);
                } else {
                    scale.send(new Preferences(context.getSharedPreferences(Preferences.PREF_EEPROM, Context.MODE_PRIVATE)).read(STR_DAT, ""));
                }
                scale.send(CR_LF);
                break;
            case CMD_SGD:                                                    // получить или записать имя таблицы google disk
                scale.send(STR_SGD);
                if (parameterFlag) {
                    new Preferences(context.getSharedPreferences(Preferences.PREF_EEPROM, Context.MODE_PRIVATE)).write(STR_SGD, parameter);
                } else {
                    scale.send(new Preferences(context.getSharedPreferences(Preferences.PREF_EEPROM, Context.MODE_PRIVATE)).read(STR_SGD, ""));
                }
                scale.send(CR_LF);
                break;
            case CMD_UGD:                                                    // получить или записать Имя account google disk
                scale.send(STR_UGD);
                if (parameterFlag) {
                    new Preferences(context.getSharedPreferences(Preferences.PREF_EEPROM, Context.MODE_PRIVATE)).write(STR_UGD, parameter);
                } else {
                    scale.send(new Preferences(context.getSharedPreferences(Preferences.PREF_EEPROM, Context.MODE_PRIVATE)).read(STR_UGD, ""));
                }
                scale.send(CR_LF);
                break;
            case CMD_PGD:                                                    // получить или записать password account google disk
                scale.send(STR_PGD);
                if (parameterFlag) {
                    new Preferences(context.getSharedPreferences(Preferences.PREF_EEPROM, Context.MODE_PRIVATE)).write(STR_PGD, parameter);
                } else {
                    scale.send(new Preferences(context.getSharedPreferences(Preferences.PREF_EEPROM, Context.MODE_PRIVATE)).read(STR_PGD, ""));
                }
                scale.send(CR_LF);
                break;
            default: {
            }

            parameterFlag = false;
            scale.power_time = scale.pwr_time;
        }

    }

    void auto_calibration_bat(int adc, int p) {
        //float f;
        if (adc > scale.max_adc_bat) {
            scale.max_adc_bat = adc;
            scale.const_bat = (float) scale.MAX_BAT / (float) scale.max_adc_bat;
            scale.min_adc_bat = (int) (scale.MIN_BAT / scale.const_bat);
            scale.const_bat = ((float) scale.max_adc_bat - (float) scale.min_adc_bat) / 100;

            new Preferences(context.getSharedPreferences(Preferences.PREF_EEPROM, Context.MODE_PRIVATE)).write(Preferences.KEY_MIN_ADC_BAT, scale.min_adc_bat);
            new Preferences(context.getSharedPreferences(Preferences.PREF_EEPROM, Context.MODE_PRIVATE)).write(Preferences.KEY_CONST_BAT, scale.const_bat);
        }
    }

    int bat_match(int adc) {
        if (adc < scale.min_adc_bat)
            adc = scale.min_adc_bat;
        else if (adc > scale.max_adc_bat) {
            auto_calibration_bat(adc, 100);
        }
        return (int) ((float) (adc - scale.min_adc_bat) / scale.const_bat);
    }

    int ad7797_single_conversion() {
        return scale.getSensorTenzo();//Scale.sensor_tenzo;
        //return 10;
    }

    void set_offset() {
        int i;
        int adc = 0;

        for (i = 0; i < 2; i++) {
            adc += ad7797_single_conversion();
            //adc ++;
        }
        scale.adc_offset = (adc / i);
    }

    int get_data_chanel_offset() {
        return (ad7797_single_conversion() - scale.adc_offset);
    }

    int ad7797_get_temperature() {
        return scale.getSensorTemp();//Scale.sensor_temp;
        //return 20;
    }
}
