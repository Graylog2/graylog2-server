package org.graylog.plugins.views.search.searchtypes.pivot.buckets;

public class DateInterval {
    private final Number quantity;
    private final String unit;

    public DateInterval(Number quantity, String unit) {
        this.quantity = quantity;
        this.unit = unit;
    }

    public static DateInterval seconds(int sec) {
        return new DateInterval(sec, "s");
    }

    public static DateInterval minutes(int min) {
        return new DateInterval(min, "m");
    }

    public static DateInterval hours(int hours) {
        return new DateInterval(hours, "h");
    }

    public static DateInterval days(int days) {
        return new DateInterval(days, "d");
    }

    public static DateInterval weeks(int weeks) {
        return new DateInterval(weeks, "w");
    }

    public static DateInterval months(int months) {
        return new DateInterval(months, "M");
    }

    public Number getQuantity() {
        return quantity;
    }

    public String getUnit() {
        return unit;
    }

    @Override
    public String toString() {
        return quantity + unit;
    }
}
