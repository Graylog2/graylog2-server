package org.graylog2.dashboards.widgets;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Maps;
import org.graylog2.plugin.dashboards.widgets.ComputationResult;
import org.graylog2.plugin.dashboards.widgets.WidgetStrategy;

import javax.inject.Inject;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class WidgetResultCache {
    private final Map<DashboardWidget, Supplier<ComputationResult>> cache;
    private final MetricRegistry metricRegistry;
    private final WidgetStrategyFactory widgetStrategyFactory;

    @Inject
    public WidgetResultCache(MetricRegistry metricRegistry, WidgetStrategyFactory widgetStrategyFactory) {
        this.metricRegistry = metricRegistry;
        this.widgetStrategyFactory = widgetStrategyFactory;
        this.cache = Maps.newHashMap();
    }

    public ComputationResult getComputationResultForDashboardWidget(final DashboardWidget dashboardWidget) throws InvalidWidgetConfigurationException {
        if (!this.cache.containsKey(dashboardWidget)) {
            final WidgetStrategy widgetStrategy = this.widgetStrategyFactory.getWidgetForType(dashboardWidget.getType().toString(),
                    dashboardWidget.getConfig(), dashboardWidget.getTimeRange(), dashboardWidget.getId());
            this.cache.put(dashboardWidget, Suppliers.memoizeWithExpiration(
                    new ComputationResultSupplier(metricRegistry, dashboardWidget, widgetStrategy),
                    dashboardWidget.getCacheTime(),
                    TimeUnit.SECONDS
            ));
        }
        return this.cache.get(dashboardWidget).get();
    }

    private class ComputationResultSupplier implements Supplier<ComputationResult> {
        private final MetricRegistry metricRegistry;
        private final DashboardWidget dashboardWidget;
        private final WidgetStrategy widgetStrategy;

        public ComputationResultSupplier(MetricRegistry metricRegistry,
                                         DashboardWidget dashboardWidget,
                                         WidgetStrategy widgetStrategy) {
            this.metricRegistry = metricRegistry;
            this.dashboardWidget = dashboardWidget;
            this.widgetStrategy = widgetStrategy;
        }

        @Override
        public ComputationResult get() {
            try (Timer.Context timer = getCalculationTimer().time()) {
                return this.widgetStrategy.compute();
            } finally {
                getCalculationMeter().mark();
            }
        }

        private Timer getCalculationTimer() {
            return metricRegistry.timer(MetricRegistry.name(this.getClass(), this.dashboardWidget.getId(), "calculationTime"));
        }

        private Meter getCalculationMeter() {
            return metricRegistry.meter(MetricRegistry.name(this.getClass(), this.dashboardWidget.getId(), "calculations"));
        }
    }
}
