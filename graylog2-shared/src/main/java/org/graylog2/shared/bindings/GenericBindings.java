/**
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.graylog2.shared.bindings;

import com.codahale.metrics.MetricRegistry;
import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.ServiceManager;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import org.graylog2.inputs.gelf.gelf.GELFChunkManager;
import org.graylog2.plugin.inputs.util.ThroughputCounter;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.shared.bindings.providers.*;
import org.graylog2.shared.buffers.ProcessBuffer;
import org.graylog2.shared.buffers.ProcessBufferWatermark;
import org.graylog2.shared.stats.ThroughputStats;
import org.jboss.netty.util.HashedWheelTimer;

import java.util.concurrent.Executors;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class GenericBindings extends AbstractModule {
    private final InstantiationService instantiationService;

    public GenericBindings(InstantiationService instantiationService) {
        this.instantiationService = instantiationService;
    }

    @Override
    protected void configure() {
        // This is holding all our metrics.
        bind(MetricRegistry.class).toInstance(new MetricRegistry());
        bind(ThroughputStats.class).toInstance(new ThroughputStats());
        bind(ProcessBufferWatermark.class).toInstance(new ProcessBufferWatermark());

        bind(InstantiationService.class).toInstance(instantiationService);

        install(new FactoryModuleBuilder().build(ProcessBuffer.Factory.class));

        bind(ProcessBuffer.class).toProvider(ProcessBufferProvider.class);
        bind(GELFChunkManager.class).toProvider(GELFChunkManagerProvider.class);
        bind(NodeId.class).toProvider(NodeIdProvider.class);

        bind(ServiceManager.class).toProvider(ServiceManagerProvider.class);

        bind(HashedWheelTimer.class).toInstance(new HashedWheelTimer());
        bind(ThroughputCounter.class);

        bind(EventBus.class).toProvider(EventBusProvider.class).in(Scopes.SINGLETON);
    }
}
