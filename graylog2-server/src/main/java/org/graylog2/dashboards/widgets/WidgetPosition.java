package org.graylog2.dashboards.widgets;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class WidgetPosition {

    public abstract String id();
    public abstract Integer width();
    public abstract Integer height();
    public abstract Integer col();
    public abstract Integer row();

    public static WidgetPosition create(String id, Integer width, Integer height, Integer col, Integer row) {
        return new AutoValue_WidgetPosition(id, width, height, col, row);
    }
}
