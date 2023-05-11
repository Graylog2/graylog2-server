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
package org.graylog.datanode.bindings;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import org.graylog.datanode.bootstrap.preflight.DataNodePreflightGeneratePeriodical;
import org.graylog.datanode.periodicals.ClusterManagerDiscovery;
import org.graylog.datanode.periodicals.NodePingPeriodical;
import org.graylog.datanode.periodicals.OpensearchNodeHeartbeat;
import org.graylog2.events.ClusterEventCleanupPeriodical;
import org.graylog2.events.ClusterEventPeriodical;
import org.graylog2.plugin.periodical.Periodical;

public class PeriodicalBindings extends AbstractModule {
    @Override
    protected void configure() {
        Multibinder<Periodical> periodicalBinder = Multibinder.newSetBinder(binder(), Periodical.class);
        periodicalBinder.addBinding().to(ClusterEventPeriodical.class);
        periodicalBinder.addBinding().to(ClusterEventCleanupPeriodical.class);
        periodicalBinder.addBinding().to(OpensearchNodeHeartbeat.class);
        periodicalBinder.addBinding().to(ClusterManagerDiscovery.class);
//        periodicalBinder.addBinding().to(UserSessionTerminationPeriodical.class);
        periodicalBinder.addBinding().to(NodePingPeriodical.class);
        periodicalBinder.addBinding().to(DataNodePreflightGeneratePeriodical.class);
    }
}
