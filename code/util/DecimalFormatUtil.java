package com.company.utils;

import java.text.DecimalFormat;

public class DecimalFormatUtil {

    public static void main(String[] args) {
        double pi = 3.1415;
        //3
        System.out.println(new DecimalFormat("0").format(pi));
        //3.14
        System.out.println(new DecimalFormat("0.00").format(pi));
        //3.14150
        System.out.println(new DecimalFormat("0.00000").format(pi));
        //03.142
        System.out.println(new DecimalFormat("00.000").format(pi));
        //3
        System.out.println(new DecimalFormat("#").format(pi));
        //3
        System.out.println(new DecimalFormat("##").format(pi));
        //3.1
        System.out.println(new DecimalFormat("##.#").format(pi));
        //314.16%
        System.out.println(new DecimalFormat("#.##%").format(pi));
        //圆周率为 3.14 米
        System.out.println(new DecimalFormat("圆周率为 ###.00 米").format(pi));
        //113.1
        System.out.println(new DecimalFormat("##.#").format(113.1415));
        //113.2
        System.out.println(new DecimalFormat("00.#").format(113.1615));
        //11211.210012
        System.out.println(new BigDecimal("0011211.210012000").stripTrailingZeros().toPlainString());
    }

}
