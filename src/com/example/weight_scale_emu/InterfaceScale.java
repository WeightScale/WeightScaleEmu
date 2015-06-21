package com.example.weight_scale_emu;

/**
 * Created by Kostya on 20.06.2015.
 */
public interface InterfaceScale {

    /**Установить фильтер ацп*/
    String STR_FAD = "FAD";
    /**Время отключения питания*/
    String STR_TOF = "TOF";
    /**калибровка акумулятора весов в процетах*/
    String STR_CBT = "CBT";
    /**получить заряд аккумулятора весов*/
    String STR_GBT = "GBT";
    /**Имя Bluetooth*/
    String STR_SNA = "SNA";
    /**Скорость Baud rate*/
    String STR_BST = "BST";
    /**Версия весов*/
    String STR_VRS = "VRS";
    /**получить АЦП канал*/
    String STR_DCH = "DCH";
    /**получить АЦП канал минус offset*/
    String STR_DCO = "DCO";
    /**установить  offset*/
    String STR_SCO = "SCO";
    /**получить АЦП температуры*/
    String STR_DTM = "DTM";
    /**получить АЦП температуры*/
    String STR_CTM = "CTM";
    /**получить/записать данные о калибровке и т.п.*/
    String STR_DAT = "DAT";
    /**получить/записать данные о таблице google disc.*/
    String STR_SGD = "SGD";
    /**получить/записать данные о user google disc.*/
    String STR_UGD = "UGD";
    /**получить/записать данные о password google disc.*/
    String STR_PGD = "PGD";
    /**считать/записать phone for sms boss*/
    String STR_PHN = "PHN";
    /**получить offset*/
    String STR_GCO = "GCO";
}
