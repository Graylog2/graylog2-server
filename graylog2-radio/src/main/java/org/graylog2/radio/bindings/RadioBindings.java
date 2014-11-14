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
package org.graylog2.radio.bindings;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import com.google.inject.util.Providers;
import com.ning.http.client.AsyncHttpClient;
import org.graylog2.inputs.BasicCache;
import org.graylog2.inputs.InputCache;
import org.graylog2.jersey.container.netty.SecurityContextFactory;
import org.graylog2.radio.Configuration;
import org.graylog2.radio.bindings.providers.AsyncHttpClientProvider;
import org.graylog2.radio.bindings.providers.RadioInputRegistryProvider;
import org.graylog2.radio.bindings.providers.RadioTransportProvider;
import org.graylog2.radio.buffers.processors.RadioProcessBufferProcessor;
import org.graylog2.radio.system.activities.NullActivityWriter;
import org.graylog2.radio.transports.RadioTransport;
import org.graylog2.radio.transports.amqp.AMQPProducer;
import org.graylog2.radio.transports.kafka.KafkaProducer;
import org.graylog2.plugin.BaseConfiguration;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.shared.inputs.InputRegistry;
import org.graylog2.shared.system.activities.ActivityWriter;

import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.ext.ExceptionMapper;
import java.net.URI;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class RadioBindings extends AbstractModule {
    private final Configuration configuration;

    public RadioBindings(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    protected void configure() {
        bindProviders();
        bindSingletons();
        bindTransport();
        install(new FactoryModuleBuilder().build(RadioProcessBufferProcessor.Factory.class));
        SecurityContextFactory instance = null;
        bind(SecurityContextFactory.class).toProvider(Providers.of(instance));
        bindDynamicFeatures();
        bindContainerResponseFilters();
        bindExceptionMappers();
        bind(ActivityWriter.class).to(NullActivityWriter.class);
    }

    private void bindSingletons() {
        bind(Configuration.class).toInstance(configuration);
        bind(BaseConfiguration.class).toInstance(configuration);

        Multibinder<ServerStatus.Capability> capabilityBinder =
                Multibinder.newSetBinder(binder(), ServerStatus.Capability.class);
        capabilityBinder.addBinding().toInstance(ServerStatus.Capability.RADIO);

        bind(ServerStatus.class).in(Scopes.SINGLETON);
        bind(InputRegistry.class).toProvider(RadioInputRegistryProvider.class).asEagerSingleton();

        bind(URI.class).annotatedWith(Names.named("ServerUri")).toInstance(configuration.getGraylog2ServerUri());
        bind(URI.class).annotatedWith(Names.named("OurRadioUri")).toInstance(configuration.getRestTransportUri());

        bind(InputCache.class).to(BasicCache.class).in(Scopes.SINGLETON);
    }

    private void bindProviders() {
        bind(AsyncHttpClient.class).toProvider(AsyncHttpClientProvider.class);
        bind(ObjectMapper.class).toProvider(ObjectMapperProvider.class);
    }

    private void bindTransport() {
        bind(RadioTransport.class).toProvider(RadioTransportProvider.class);
    }

    private void bindDynamicFeatures() {
        TypeLiteral<Class<? extends DynamicFeature>> type = new TypeLiteral<Class<? extends DynamicFeature>>(){};
        Multibinder<Class<? extends DynamicFeature>> setBinder = Multibinder.newSetBinder(binder(), type);
    }

    private void bindContainerResponseFilters() {
        TypeLiteral<Class<? extends ContainerResponseFilter>> type = new TypeLiteral<Class<? extends ContainerResponseFilter>>(){};
        Multibinder<Class<? extends ContainerResponseFilter>> setBinder = Multibinder.newSetBinder(binder(), type);
    }

    private void bindExceptionMappers() {
        TypeLiteral<Class<? extends ExceptionMapper>> type = new TypeLiteral<Class<? extends ExceptionMapper>>(){};
        Multibinder<Class<? extends ExceptionMapper>> setBinder = Multibinder.newSetBinder(binder(), type);
    }
}
