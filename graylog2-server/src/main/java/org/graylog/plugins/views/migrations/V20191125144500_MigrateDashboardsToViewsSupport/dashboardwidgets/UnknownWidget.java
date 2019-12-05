package org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.dashboardwidgets;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.RandomUUIDProvider;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.ViewWidget;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.Widget;
import org.graylog.plugins.views.migrations.V20191125144500_MigrateDashboardsToViewsSupport.viewwidgets.NonImplementedWidget;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

@AutoValue
@JsonAutoDetect
public abstract class UnknownWidget implements WidgetConfig {
    public abstract Map<String, Object> config();

    @Override
    public Set<ViewWidget> toViewWidgets(Widget widget, RandomUUIDProvider randomUUIDProvider) {
        return Collections.singleton(NonImplementedWidget.create(widget.id(), widget.type(), config()));
    }

    @JsonCreator
    public static UnknownWidget create(Map<String, Object> config) {
        return new AutoValue_UnknownWidget(config);
    }
}
