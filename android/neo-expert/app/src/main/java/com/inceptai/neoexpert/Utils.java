package com.inceptai.neoexpert;

/**
 * Created by arunesh on 7/13/17.
 */

public class Utils {
    public static final String TAG = "NeoExpert";

    private Utils() {}

    public static boolean notNullOrEmpty(String target) {
        return target != null && !target.isEmpty();
    }

    public static boolean nullOrEmpty(String target) {
        return target == null || target.isEmpty();
    }
}
