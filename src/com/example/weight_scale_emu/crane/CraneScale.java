package com.example.weight_scale_emu.crane;

import android.content.Context;
import com.example.weight_scale_emu.Versions;

/**
 * Created by Kostya on 17.06.2015.
 */
public abstract class CraneScale extends Versions {
    final static String TAG = "CraneScale";

    protected CraneScale(Context context) {
        super(context);
    }
}
