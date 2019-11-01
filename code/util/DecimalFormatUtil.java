package utils;

import java.math.BigDecimal;

public class DecimalFormatUtil {

    public static void main(String[] args) {
        double pi = 3.1416;

        System.out.println(setScaleDown(pi, 1)); //3.1
        System.out.println(setScaleDown(pi, 3)); //3.141
        System.out.println(setScaleDown(pi, 9)); //3.141600000
        System.out.println(setScaleDown(3, 9)); //3.000000000

        System.out.println();

        System.out.println(setScaleHalfUp(pi, 1)); //3.1
        System.out.println(setScaleHalfUp(pi, 3)); //3.142
        System.out.println(setScaleHalfUp(pi, 9)); //3.141600000

        System.out.println();

        System.out.println(setScaleDownStrip(pi, 1)); //3.1
        System.out.println(setScaleDownStrip(pi, 3)); //3.141
        System.out.println(setScaleDownStrip(pi, 9)); //3.1416

        System.out.println();

        System.out.println(setScaleHalfUpStrip(pi, 1)); //3.1
        System.out.println(setScaleHalfUpStrip(pi, 3)); //3.142
        System.out.println(setScaleHalfUpStrip(pi, 9)); //3.1416

        System.out.println();

        System.out.println(stripTrailingZeros(1.121000100000)); //1.1210001
        System.out.println(stripTrailingZeros(1.121000100000100)); //1.1210001000001
    }

    public static String setScale(String value, int scale, int model) {
        return new BigDecimal(value).setScale(scale, model).toPlainString();
    }

    public static String setScale(double value, int scale, int model) {
        return setScale(String.valueOf(value), scale, model);
    }

    //指定小数位的位数，小数位数不足则补零，不四舍五入
    public static String setScaleDown(double value, int scale) {
        return setScale(String.valueOf(value), scale, BigDecimal.ROUND_DOWN);
    }

    //指定小数位的位数，小数位数不足则补零，四舍五入
    public static String setScaleHalfUp(double value, int scale) {
        return setScale(String.valueOf(value), scale, BigDecimal.ROUND_HALF_UP);
    }

    //指定小数位的最大位数，不四舍五入
    public static String setScaleDownStrip(double value, int scale) {
        return stripTrailingZeros(setScale(String.valueOf(value), scale, BigDecimal.ROUND_DOWN));
    }

    //指定小数位的最大位数，四舍五入
    public static String setScaleHalfUpStrip(double value, int scale) {
        return stripTrailingZeros(setScale(String.valueOf(value), scale, BigDecimal.ROUND_HALF_UP));
    }

    //去除尾数0
    public static String stripTrailingZeros(double value) {
        return stripTrailingZeros(String.valueOf(value));
    }

    //去除尾数0
    public static String stripTrailingZeros(String value) {
        return new BigDecimal(value).stripTrailingZeros().toPlainString();
    }

}
