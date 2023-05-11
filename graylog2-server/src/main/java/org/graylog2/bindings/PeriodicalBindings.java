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
package org.graylog2.bindings;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import org.graylog.scheduler.periodicals.ScheduleTriggerCleanUp;
import org.graylog2.events.ClusterEventCleanupPeriodical;
import org.graylog2.events.ClusterEventPeriodical;
import org.graylog2.indexer.fieldtypes.IndexFieldTypePollerPeriodical;
import org.graylog2.periodical.BatchedElasticSearchOutputFlushThread;
import org.graylog2.periodical.ClusterHealthCheckThread;
import org.graylog2.periodical.ContentPackLoaderPeriodical;
import org.graylog2.periodical.ESVersionCheckPeriodical;
import org.graylog2.periodical.IndexBlockCheck;
import org.graylog2.periodical.IndexRangesCleanupPeriodical;
import org.graylog2.periodical.IndexRetentionThread;
import org.graylog2.periodical.IndexRotationThread;
import org.graylog2.periodical.IndexerClusterCheckerThread;
import org.graylog2.periodical.NodePingThread;
import org.graylog2.periodical.ThrottleStateUpdaterThread;
import org.graylog2.periodical.TrafficCounterCalculator;
import org.graylog2.periodical.UserSessionTerminationPeriodical;
import org.graylog2.periodical.VersionCheckThread;
import org.graylog2.plugin.periodical.Periodical;
import org.graylog2.telemetry.cluster.TelemetryClusterInfoPeriodical;

public class PeriodicalBindings extends AbstractModule {
    @Override
    protected void configure() {
        Multibinder<Periodical> periodicalBinder = Multibinder.newSetBinder(binder(), Periodical.class);
        periodicalBinder.addBinding().to(BatchedElasticSearchOutputFlushThread.class);
        periodicalBinder.addBinding().to(ClusterHealthCheckThread.class);
        periodicalBinder.addBinding().to(ContentPackLoaderPeriodical.class);
        periodicalBinder.addBinding().to(IndexerClusterCheckerThread.class);
        periodicalBinder.addBinding().to(IndexBlockCheck.class);
        periodicalBinder.addBinding().to(IndexRetentionThread.class);
        periodicalBinder.addBinding().to(IndexRotationThread.class);
        periodicalBinder.addBinding().to(NodePingThread.class);
        periodicalBinder.addBinding().to(VersionCheckThread.class);
        periodicalBinder.addBinding().to(ThrottleStateUpdaterThread.class);
        periodicalBinder.addBinding().to(ClusterEventPeriodical.class);
        periodicalBinder.addBinding().to(ClusterEventCleanupPeriodical.class);
        periodicalBinder.addBinding().to(IndexRangesCleanupPeriodical.class);
        periodicalBinder.addBinding().to(TrafficCounterCalculator.class);
        periodicalBinder.addBinding().to(IndexFieldTypePollerPeriodical.class);
        periodicalBinder.addBinding().to(ScheduleTriggerCleanUp.class);
        periodicalBinder.addBinding().to(ESVersionCheckPeriodical.class);
        periodicalBinder.addBinding().to(UserSessionTerminationPeriodical.class);
        periodicalBinder.addBinding().to(TelemetryClusterInfoPeriodical.class);
    }
}
