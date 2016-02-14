package com.example.weight_scale_emu.crane;

import android.content.Context;

/**
 * Created by Kostya on 18.06.2015.
 */
public class V1 extends CraneScale {
    /**
     * Имя версии
     */
    final static String TAG = "1";

    public V1(Context context) {
        super(context);
    }

    public String toString() {
        return CraneScale.TAG + TAG;
    }

    @Override
    public void execute(String cmd) {
    }
}
