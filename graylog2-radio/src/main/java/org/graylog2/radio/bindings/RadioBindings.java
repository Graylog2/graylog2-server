/*
 * Copyright 2012-2014 TORCH GmbH
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
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.graylog2.radio.bindings;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.ning.http.client.AsyncHttpClient;
import org.graylog2.radio.Configuration;
import org.graylog2.radio.bindings.providers.AsyncHttpClientProvider;
import org.graylog2.radio.bindings.providers.RadioInputRegistryProvider;
import org.graylog2.radio.buffers.processors.RadioProcessBufferProcessor;
import org.graylog2.shared.ServerStatus;
import org.graylog2.shared.inputs.InputRegistry;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class RadioBindings extends AbstractModule {
    private final Configuration configuration;
    private static final int SCHEDULED_THREADS_POOL_SIZE = 10;

    public RadioBindings(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    protected void configure() {
        bindProviders();
        bindSingletons();
        install(new FactoryModuleBuilder().build(RadioProcessBufferProcessor.Factory.class));
    }

    private void bindSingletons() {
        bind(Configuration.class).toInstance(configuration);

        ServerStatus serverStatus = new ServerStatus(configuration);
        serverStatus.addCapability(ServerStatus.Capability.RADIO);
        bind(ServerStatus.class).toInstance(serverStatus);
        bind(InputRegistry.class).toProvider(RadioInputRegistryProvider.class);
    }

    private void bindProviders() {
        bind(AsyncHttpClient.class).toProvider(AsyncHttpClientProvider.class);
    }
}
