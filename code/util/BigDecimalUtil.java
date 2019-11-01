package utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class BigDecimalUtil {

    public static void main(String[] args) {
        System.out.println("比较大小");
        System.out.println(compare(2.2222, 3.333)); //-1
        System.out.println(compare(4.2222, 3.333)); //1
        System.out.println(compare(2.2222, 2.2222)); //0

        System.out.println("加");
        System.out.println(add(11.22, 22, 334, 44.44, 32.112121)); //443.772121
        System.out.println(add("11.22", "22", "334", "44.44", "32.112121")); //443.772121

        System.out.println("减");
        System.out.println(subtract(10, 0.001, 0.0001)); //9.9989
        System.out.println(subtract(11.22, 22, 334, 44.44, 32.11212)); //-421.33212
        System.out.println(subtract("11.22", "22", "334", "44.44", "32.11212")); //-421.33212

        System.out.println("乘");
        System.out.println(multiply(11.221, 22.22222222)); //249.35555553062

        System.out.println("除");
        System.out.println(divideDown(12.1299, 2, 6)); //6.06495
        System.out.println(divideDown(12.1299, 2, 5)); //6.06495
        System.out.println(divideDown(12.1299, 2, 4)); //6.064

        System.out.println(divideHalfUp(12.1299, 2, 3)); //6.065
        System.out.println(divideHalfUp(12.1299, 2, 2)); //6.06
        System.out.println(divideHalfUp(12.1299, 2, 1)); //6.1

        System.out.println("绝对值");
        System.out.println(abs(-22.22121141141241)); //22.22121141141241

        System.out.println("最大值");
        System.out.println(max(22.22, 22.2222));

        System.out.println("最小值");
        System.out.println(min(22.22, 22.233));
    }

    public static int compare(int value1, int value2) {
        return compare(String.valueOf(value1), String.valueOf(value2));
    }

    public static int compare(double value1, double value2) {
        return compare(String.valueOf(value1), String.valueOf(value2));
    }

    public static int compare(String value1, String value2) {
        BigDecimal bigDecimal = new BigDecimal(value1);
        BigDecimal bigDecimal1 = new BigDecimal(value2);
        return bigDecimal.compareTo(bigDecimal1);
    }

    public static double add(int... values) {
        if (values == null || values.length == 0) {
            return 0;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (double value : values) {
            total = total.add(new BigDecimal(String.valueOf(value)));
        }
        return total.doubleValue();
    }

    public static double add(double... values) {
        if (values == null || values.length == 0) {
            return 0;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (double value : values) {
            total = total.add(new BigDecimal(String.valueOf(value)));
        }
        return total.doubleValue();
    }

    public static double add(String... values) {
        if (values == null || values.length == 0) {
            return 0;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (String value : values) {
            total = total.add(new BigDecimal(value));
        }
        return total.doubleValue();
    }

    public static double subtract(int... values) {
        if (values == null || values.length == 0) {
            return 0;
        }
        BigDecimal total = new BigDecimal(String.valueOf(values[0]));
        for (int i = 1; i < values.length; i++) {
            total = total.subtract(new BigDecimal(String.valueOf(values[i])));
        }
        return total.doubleValue();
    }

    public static double subtract(double... values) {
        if (values == null || values.length == 0) {
            return 0;
        }
        BigDecimal total = new BigDecimal(String.valueOf(values[0]));
        for (int i = 1; i < values.length; i++) {
            total = total.subtract(new BigDecimal(String.valueOf(values[i])));
        }
        return total.doubleValue();
    }

    public static double subtract(String... values) {
        if (values == null || values.length == 0) {
            return 0;
        }
        BigDecimal total = new BigDecimal(values[0]);
        for (int i = 1; i < values.length; i++) {
            total = total.subtract(new BigDecimal(values[i]));
        }
        return total.doubleValue();
    }

    public static double multiply(int... values) {
        if (values == null || values.length == 0) {
            return 0;
        }
        BigDecimal total = new BigDecimal(String.valueOf(values[0]));
        for (int i = 1; i < values.length; i++) {
            total = total.multiply(new BigDecimal(String.valueOf(values[i])));
        }
        return total.doubleValue();
    }

    public static double multiply(double... values) {
        if (values == null || values.length == 0) {
            return 0;
        }
        BigDecimal total = new BigDecimal(String.valueOf(values[0]));
        for (int i = 1; i < values.length; i++) {
            total = total.multiply(new BigDecimal(String.valueOf(values[i])));
        }
        return total.doubleValue();
    }

    public static double multiply(String... values) {
        if (values == null || values.length == 0) {
            return 0;
        }
        BigDecimal total = new BigDecimal(values[0]);
        for (int i = 1; i < values.length; i++) {
            total = total.multiply(new BigDecimal(values[i]));
        }
        return total.doubleValue();
    }

    public static double divide(String value1, String value2, int scale, int model) {
        return new BigDecimal(value1).divide(new BigDecimal(value2), scale, model).doubleValue();
    }

    public static double divide(double value1, double value2, int scale, int model) {
        return divide(String.valueOf(value1), String.valueOf(value2), scale, model);
    }

    //指定小数位的最大位数，不四舍五入
    public static double divideDown(int value1, int value2, int scale) {
        return divide(value1, value2, scale, BigDecimal.ROUND_DOWN);
    }

    public static double divideDown(double value1, double value2, int scale) {
        return divide(value1, value2, scale, BigDecimal.ROUND_DOWN);
    }

    public static double divideDown(String value1, String value2, int scale) {
        return divide(value1, value2, scale, BigDecimal.ROUND_DOWN);
    }

    //指定小数位的最大位数，四舍五入
    public static double divideHalfUp(String value1, String value2, int scale) {
        return new BigDecimal(value1).divide(new BigDecimal(value2), scale, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    public static double divideHalfUp(double value1, double value2, int scale) {
        return divideHalfUp(String.valueOf(value1), String.valueOf(value2), scale);
    }

    public static double abs(int value) {
        return abs(String.valueOf(value));
    }

    public static double abs(double value) {
        return abs(String.valueOf(value));
    }

    public static double abs(String value) {
        return new BigDecimal(value).abs().doubleValue();
    }

    public static double max(int value1, int value2) {
        return max(String.valueOf(value1), String.valueOf(value2));
    }

    public static double max(double value1, double value2) {
        return max(String.valueOf(value1), String.valueOf(value2));
    }

    public static double max(String value1, String value2) {
        return new BigDecimal(value1).max(new BigDecimal(value2)).doubleValue();
    }

    public static double min(int value1, int value2) {
        return min(String.valueOf(value1), String.valueOf(value2));
    }

    public static double min(double value1, double value2) {
        return min(String.valueOf(value1), String.valueOf(value2));
    }

    public static double min(String value1, String value2) {
        return new BigDecimal(value1).min(new BigDecimal(value2)).doubleValue();
    }

}