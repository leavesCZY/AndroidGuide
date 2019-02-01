package com.company.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class BigDecimalUtil {

    public static void main(String[] args) {
        System.out.println(compare(2.2222, 3.333));
        System.out.println(add(11.22, 22, 334, 44.44, 32.11212));
        System.out.println(subtract(11.22, 22, 334, 44.44, 32.11212));
        System.out.println(multiply(11.221, 22.222));
        System.out.println(divide(11.22, 22.22222));
        System.out.println(abs(22.22121141141241));
        System.out.println(max(22.22, 22.2222));
        System.out.println(min(22.22, 22.233));
    }

    public static int compare(double value1, double value2) {
        return compare(String.valueOf(value1), String.valueOf(value2));
    }

    public static int compare(String value1, String value2) {
        BigDecimal bigDecimal = new BigDecimal(value1);
        BigDecimal bigDecimal1 = new BigDecimal(value2);
        return bigDecimal.compareTo(bigDecimal1);
    }

    public static double add(String... values) {
        BigDecimal total = BigDecimal.ZERO;
        for (String value : values) {
            total = total.add(new BigDecimal(value));
        }
        return total.doubleValue();
    }

    public static double add(double... values) {
        String[] strValues = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            strValues[i] = String.valueOf(values[i]);
        }
        return add(strValues);
    }

    public static double subtract(String... values) {
        if (values.length > 0) {
            BigDecimal total = new BigDecimal(values[0]);
            for (int i = 1; i < values.length; i++) {
                total = total.subtract(new BigDecimal(values[i]));
            }
            return total.doubleValue();
        }
        return 0;
    }

    public static double subtract(double... values) {
        String[] strValues = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            strValues[i] = String.valueOf(values[i]);
        }
        return subtract(strValues);
    }

    public static double multiply(String... values) {
        if (values.length > 0) {
            BigDecimal total = new BigDecimal(values[0]);
            for (int i = 1; i < values.length; i++) {
                total = total.multiply(new BigDecimal(values[i]));
            }
            return total.doubleValue();
        }
        return 0;
    }

    public static double multiply(double... values) {
        String[] strValues = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            strValues[i] = String.valueOf(values[i]);
        }
        return multiply(strValues);
    }

    public static double divide(String value1, String value2, int scale, int model) {
        return new BigDecimal(value1).divide(new BigDecimal(value2), scale, model).doubleValue();
    }

    public static double divide(double value1, double value2, int scale, int model) {
        return divide(String.valueOf(value1), String.valueOf(value2), scale, model);
    }

    public static double divide(String value1, String value2) {
        return new BigDecimal(value1).divide(new BigDecimal(value2), 3, RoundingMode.HALF_UP).doubleValue();
    }

    public static double divide(double value1, double value2) {
        return divide(String.valueOf(value1), String.valueOf(value2));
    }

    public static double abs(double value) {
        return new BigDecimal(String.valueOf(value)).abs().doubleValue();
    }

    public static double max(String value1, String value2) {
        return new BigDecimal(value1).max(new BigDecimal(value2)).doubleValue();
    }

    public static double max(double value1, double value2) {
        return max(String.valueOf(value1), String.valueOf(value2));
    }

    public static double min(String value1, String value2) {
        return new BigDecimal(value1).min(new BigDecimal(value2)).doubleValue();
    }

    public static double min(double value1, double value2) {
        return min(String.valueOf(value1), String.valueOf(value2));
    }

}