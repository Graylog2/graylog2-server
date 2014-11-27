/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.shared.bindings;

import com.codahale.metrics.MetricRegistry;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.inputs.util.ThroughputCounter;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.shared.bindings.providers.EventBusProvider;
import org.graylog2.shared.bindings.providers.MetricRegistryProvider;
import org.graylog2.shared.bindings.providers.NodeIdProvider;
import org.graylog2.shared.bindings.providers.ServiceManagerProvider;
import org.graylog2.shared.buffers.ProcessBuffer;
import org.graylog2.shared.stats.ThroughputStats;
import org.jboss.netty.util.HashedWheelTimer;

public class GenericBindings extends AbstractModule {
    private final InstantiationService instantiationService;

    public GenericBindings(InstantiationService instantiationService) {
        this.instantiationService = instantiationService;
    }

    @Override
    protected void configure() {
        // This is holding all our metrics.
        bind(MetricRegistry.class).toProvider(MetricRegistryProvider.class).asEagerSingleton();
        bind(LocalMetricRegistry.class).in(Scopes.NO_SCOPE); // must not be a singleton!
        bind(ThroughputStats.class).toInstance(new ThroughputStats());

        bind(InstantiationService.class).toInstance(instantiationService);

        bind(ProcessBuffer.class).asEagerSingleton();
        bind(NodeId.class).toProvider(NodeIdProvider.class);

        bind(ServiceManager.class).toProvider(ServiceManagerProvider.class).asEagerSingleton();

        bind(HashedWheelTimer.class).toInstance(new HashedWheelTimer());
        bind(ThroughputCounter.class);

        bind(EventBus.class).toProvider(EventBusProvider.class).in(Scopes.SINGLETON);
    }
}
