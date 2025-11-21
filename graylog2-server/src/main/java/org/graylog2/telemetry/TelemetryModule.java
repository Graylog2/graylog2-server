/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.telemetry;

import com.google.inject.multibindings.Multibinder;
import org.graylog2.plugin.PluginModule;
import org.graylog2.telemetry.scheduler.TelemetrySubmissionPeriodical;
import org.graylog2.telemetry.suppliers.UsersMetricsSupplier;
import org.graylog2.telemetry.suppliers.InputsMetricsSupplier;
import org.graylog2.telemetry.suppliers.OutputsMetricsSupplier;
import org.graylog2.telemetry.suppliers.MongoDBMetricsSupplier;
import org.graylog2.telemetry.suppliers.ShardsMetricsSupplier;
import org.graylog2.telemetry.suppliers.LookupTablesSupplier;
import org.graylog2.telemetry.suppliers.EventDefinitionsMetricsSupplier;
import org.graylog2.telemetry.suppliers.EventNotificationsMetricsSupplier;
import org.graylog2.telemetry.suppliers.DashboardsMetricsSupplier;
import org.graylog2.telemetry.suppliers.StreamsMetricsSupplier;
import org.graylog2.telemetry.suppliers.SidecarsVersionSupplier;
import org.graylog2.telemetry.suppliers.NodesSystemMetricsSupplier;

public class TelemetryModule extends PluginModule {
    @Override
    protected void configure() {
        // Initializing binder so it can be injected with no actual bindings
        telemetryMetricSupplierBinder();

        addPeriodical(TelemetrySubmissionPeriodical.class);
        Multibinder.newSetBinder(binder(), TelemetryDataProvider.class);

        addTelemetryMetricProvider("Users Metrics", UsersMetricsSupplier.class);
        addTelemetryMetricProvider("Inputs Metrics", InputsMetricsSupplier.class);
        addTelemetryMetricProvider("Outputs Metrics", OutputsMetricsSupplier.class);
        addTelemetryMetricProvider("MongoDB Metrics", MongoDBMetricsSupplier.class);
        addTelemetryMetricProvider("Shards Metrics", ShardsMetricsSupplier.class);
        addTelemetryMetricProvider("Lookup Tables Metrics", LookupTablesSupplier.class);
        addTelemetryMetricProvider("Event Definitions Metrics", EventDefinitionsMetricsSupplier.class);
        addTelemetryMetricProvider("Event Notifications Metrics", EventNotificationsMetricsSupplier.class);
        addTelemetryMetricProvider("Dashboards Metrics", DashboardsMetricsSupplier.class);
        addTelemetryMetricProvider("Streams Metrics", StreamsMetricsSupplier.class);
        addTelemetryMetricProvider("Sidecars Version", SidecarsVersionSupplier.class);
        addTelemetryMetricProvider("Nodes System Metrics", NodesSystemMetricsSupplier.class);
    }
}
