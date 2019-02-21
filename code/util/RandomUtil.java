package com.company.utils;

import java.util.Random;

public class RandomUtil {

    public static void main(String[] args) {
       
    }

    //返回 min 到 max 之间的 int，包括 min 和 max
    private static int fun1(int min, int max) {
        if (min >= max) {
            throw new IllegalArgumentException();
        }
        return min + new Random().nextInt(max - min + 1);
    }

    //返回 min 到 max 之间的 long，包括 min 和 max
    private static long fun2(long min, long max) {
        if (min >= max) {
            throw new IllegalArgumentException();
        }
        return min + (long) (new Random().nextDouble() * (max - min + 1));
    }

    //返回 min 到 max 之间的 double，包括 min ，不包括 max
    private static double fun3(double min, double max) {
        if (min >= max) {
            throw new IllegalArgumentException();
        }
        return min + new Random().nextDouble() * (max - min);
    }

    //返回 min 到 max 之间的 float，包括 min ，不包括 max
    private static double fun4(float min, float max) {
        if (min >= max) {
            throw new IllegalArgumentException();
        }
        return min + new Random().nextFloat() * (max - min);
    }

}