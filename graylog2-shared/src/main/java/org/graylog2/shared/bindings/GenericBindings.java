/*
 * Copyright 2013-2014 TORCH GmbH
 *
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
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.graylog2.shared.bindings;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import org.graylog2.shared.buffers.ProcessBuffer;
import org.graylog2.shared.buffers.processors.ProcessBufferProcessor;
import org.graylog2.shared.filters.FilterRegistry;
import org.graylog2.shared.periodical.ThroughputCounterManagerThread;
import org.graylog2.shared.stats.ThroughputStats;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class GenericBindings extends AbstractModule {
    @Override
    protected void configure() {
        // This is holding all our metrics.
        bind(MetricRegistry.class).toInstance(new MetricRegistry());
        bind(FilterRegistry.class).toInstance(new FilterRegistry());
        bind(ThroughputStats.class).toInstance(new ThroughputStats());

        install(new FactoryModuleBuilder().build(ProcessBuffer.Factory.class));
        install(new FactoryModuleBuilder().build(ProcessBufferProcessor.Factory.class));
        install(new FactoryModuleBuilder().build(ThroughputCounterManagerThread.Factory.class));
    }
}
