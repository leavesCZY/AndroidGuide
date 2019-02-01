package com.company.utils;

import java.text.MessageFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Calendar;

public class LocalDateUtil {

    public static void main(String[] args) {
        fun1();
        fun2();
        fun3();
        fun4();
        fun5();
        fun6();
        fun7();
        fun8();
        fun9();
        fun10();
        fun11();
        fun12();
        fun13();
    }

    //计算某个月份的天数
    private static void fun13() {
        int year = 2017;
        int month = 2;

        YearMonth yearMonth = YearMonth.of(year, month);
        //28
        System.out.println(yearMonth.lengthOfMonth());

        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month - 1);
        //28
        System.out.println(calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
    }

    //计算某一天是该年或该月的第几个星期
    private static void fun12() {
        int year = 2018;
        int month = 11;
        int day = 11;

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month - 1);
        calendar.set(Calendar.DAY_OF_MONTH, day);

        int weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR);
        //46
        System.out.println(weekOfYear);

        int weekOfMonth = calendar.get(Calendar.WEEK_OF_MONTH);
        //3
        System.out.println(weekOfMonth);
    }

    private static void fun11() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        LocalDateTime now = LocalDateTime.now();
        //2018-12-25 11:02:19
        System.out.println(now.format(formatter));

        LocalDateTime nowPlusDay = now.plusDays(1);
        //2018-12-26 11:02:19
        System.out.println(nowPlusDay.format(formatter));

        LocalDateTime nowMinusHours = now.minusHours(5);
        //2018-12-25 06:02:19
        System.out.println(nowMinusHours.format(formatter));

        String datetimeText = "2018-12-31 23:59:59";
        LocalDateTime datetime = LocalDateTime.parse(datetimeText, formatter);
        //2018-12-31T23:59:59
        System.out.println(datetime);
    }

    private static void fun10() {
        LocalDate startDate = LocalDate.of(2018, 11, 19);
        LocalDate endDate = LocalDate.of(2018, 11, 29);
        //10
        System.out.println(ChronoUnit.DAYS.between(startDate, endDate));

        endDate = LocalDate.of(2019, 1, 3);
        //45
        System.out.println(ChronoUnit.DAYS.between(startDate, endDate));
        //6
        System.out.println(ChronoUnit.WEEKS.between(startDate, endDate));

        LocalDateTime startDateTime = LocalDateTime.of(2018, 12, 12, 14, 10);
        LocalDateTime endDateTime = LocalDateTime.of(2018, 12, 12, 17, 12);
        //0
        System.out.println(ChronoUnit.DAYS.between(startDateTime, endDateTime));
        //3
        System.out.println(ChronoUnit.HOURS.between(startDateTime, endDateTime));
        //182
        System.out.println(ChronoUnit.MINUTES.between(startDateTime, endDateTime));
    }

    private static void fun9() {
        Instant start = Instant.now();
        Instant end = start.plusSeconds(100);
        Duration timeElapsed = Duration.between(start, end);
        //PT1M40S
        System.out.println(timeElapsed);
        //0
        System.out.println(timeElapsed.toHours());
        //1
        System.out.println(timeElapsed.toMinutes());
        //100000
        System.out.println(timeElapsed.toMillis());
    }

    private static void fun8() {
        LocalDate today = LocalDate.now();

        LocalDate firstDayOfThisMonth = today.with(TemporalAdjusters.firstDayOfMonth());
        //取本月第1天 2018-12-01
        System.out.println(firstDayOfThisMonth);

        LocalDate secondDayOfThisMonth = today.withDayOfMonth(2);
        //取本月第2天 2018-12-02
        System.out.println(secondDayOfThisMonth);

        LocalDate lastDayOfThisMonth = today.with(TemporalAdjusters.lastDayOfMonth());
        //取本月最后一天 2018-12-31
        System.out.println(lastDayOfThisMonth);

        LocalDate firstDayOf2015 = lastDayOfThisMonth.plusDays(1);
        //取下一天 2019-01-01
        System.out.println(firstDayOf2015);

        LocalDate firstMondayOfYear = LocalDate.parse("2019-02-01").with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY));
        //取当年2月第一个周一 2019-02-04
        System.out.println(firstMondayOfYear);
    }

    private static void fun7() {
        LocalDate localDate = LocalDate.now();

        LocalDate localDate1 = LocalDate.of(2018, 11, 1);
        Period period = Period.between(localDate, localDate1);
        //2018-12-25和2018-11-01相差了0年-1月-24日
        System.out.println(MessageFormat.format("{0}和{1}相差了{2}年{3}月{4}日", localDate, localDate1, period.getYears(), period.getMonths(), period.getDays()));

        period = Period.between(localDate1, localDate);
        //2018-11-01和2018-12-25相差了0年1月24日
        System.out.println(MessageFormat.format("{0}和{1}相差了{2}年{3}月{4}日", localDate1, localDate, period.getYears(), period.getMonths(), period.getDays()));

        LocalDate localDate2 = LocalDate.of(2018, 12, 22);
        period = Period.between(localDate, localDate2);
        //2018-12-25和2018-12-22相差了0年0月-3日
        System.out.println(MessageFormat.format("{0}和{1}相差了{2}年{3}月{4}日", localDate, localDate2, period.getYears(), period.getMonths(), period.getDays()));

        LocalDate localDate3 = LocalDate.of(2018, 12, 1);
        LocalDate localDate4 = LocalDate.of(2018, 11, 30);
        period = Period.between(localDate3, localDate4);
        //2018-12-01和2018-11-30相差了0年0月-1日
        System.out.println(MessageFormat.format("{0}和{1}相差了{2}年{3}月{4}日", localDate3, localDate4, period.getYears(), period.getMonths(), period.getDays()));
    }

    private static void fun6() {
        YearMonth yearMonth = YearMonth.now();
        //2,018年的12月有31天，全年有365天，是否闰年：false
        System.out.println(MessageFormat.format("{0}年的{1}月有{2}天，全年有{3}天，是否闰年：{4}",
                yearMonth.getYear(), yearMonth.getMonthValue(), yearMonth.lengthOfMonth(), yearMonth.lengthOfYear(), yearMonth.isLeapYear()));
        //2019-02
        System.out.println(yearMonth.plusMonths(2));
    }

    private static void fun5() {
        LocalDateTime localDateTime = LocalDateTime.now();
        //2018-12-25T09:46:31.384
        System.out.println(localDateTime);

        LocalDateTime localDateTime1 = localDateTime.plusHours(2);
        //2018-12-25T11:46:31.384
        System.out.println(localDateTime1);

        LocalDateTime localDateTime2 = localDateTime.minusDays(1);
        //2018-12-24T09:46:31.384
        System.out.println(localDateTime2);

        LocalDateTime localDateTime3 = localDateTime.minusDays(-1);
        //2018-12-26T09:46:31.384
        System.out.println(localDateTime3);

        LocalDateTime localDateTime4 = localDateTime.plusWeeks(1);
        //2019-01-01T09:46:31.384
        System.out.println(localDateTime4);

        //true
        System.out.println(localDateTime.isBefore(localDateTime1));
        //true
        System.out.println(localDateTime.isAfter(localDateTime2));
        //false
        System.out.println(localDateTime.isEqual(localDateTime3));

    }

    private static void fun4() {
        LocalDateTime localDateTime = LocalDateTime.now();
        //2018-12-25T09:41:11.787
        System.out.println(localDateTime);
        //12
        System.out.println(localDateTime.getMonthValue());
        //25
        System.out.println(localDateTime.getDayOfMonth());
        //9
        System.out.println(localDateTime.getHour());
        //41
        System.out.println(localDateTime.getMinute());
        //1
        System.out.println(localDateTime.getSecond());
        //787000000
        System.out.println(localDateTime.getNano());
    }

    private static void fun3() {
        LocalDate today = LocalDate.now();
        LocalDate dayOfBirth = LocalDate.of(2000, 12, 2);
        MonthDay birth = MonthDay.of(dayOfBirth.getMonth(), dayOfBirth.getDayOfMonth());
        MonthDay toadyMonthDay = MonthDay.from(today);
        //--12-02
        System.out.println(birth);
        //--12-25
        System.out.println(toadyMonthDay);
        //DECEMBER
        System.out.println(birth.getMonth());
        //12
        System.out.println(birth.getMonthValue());
    }

    private static void fun2() {
        LocalDate localDate = LocalDate.of(1995, 12, 25);
        //2018-12-12
        System.out.println(localDate);
    }

    private static void fun1() {
        LocalDate today = LocalDate.now();
        //2018-12-25
        System.out.println(today);
        int year = today.getYear();
        int month = today.getMonthValue();
        int day = today.getDayOfMonth();
        //2018-12-25
        System.out.println(year + "-" + month + "-" + day);
    }


}