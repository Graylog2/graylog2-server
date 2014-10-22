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
package org.graylog2.inputs.transports;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Names;
import org.graylog2.plugin.ConfigClass;
import org.graylog2.plugin.FactoryClass;
import org.graylog2.plugin.inputs.transports.Transport;

import java.lang.annotation.Annotation;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class TransportsModule extends AbstractModule {
    protected void configure() {
        // lol generics
        final MapBinder<String, Transport.Factory<? extends Transport>> mapBinder =
                MapBinder.newMapBinder(binder(),
                                       TypeLiteral.get(String.class),
                                       new TypeLiteral<Transport.Factory<? extends Transport>>() {
                                       });

        installTransport(mapBinder, "udp", UdpTransport.class);
        installTransport(mapBinder, "tcp", TcpTransport.class);
        installTransport(mapBinder, "http", HttpTransport.class);
        installTransport(mapBinder, "randomhttp", RandomMessageTransport.class);
        installTransport(mapBinder, "kafka", KafkaTransport.class);
        installTransport(mapBinder, "radiokafka", RadioKafkaTransport.class);
        installTransport(mapBinder, "amqp", AmqpTransport.class);
        installTransport(mapBinder, "radioamqp", RadioAmqpTransport.class);
        installTransport(mapBinder, "httppoll", HttpPollTransport.class);
        installTransport(mapBinder, "localmetrics", LocalMetricsTransport.class);

        bind(Executor.class)
                .annotatedWith(Names.named("bossPool"))
                .toInstance(Executors.newCachedThreadPool(new ThreadFactoryBuilder()
                                                                  .setNameFormat("transport-boss-%d")
                                                                  .build()));

        bind(Executor.class)
                .annotatedWith(Names.named("cached"))
                .toProvider(new Provider<Executor>() {
                    @Override
                    public Executor get() {
                        return Executors.newCachedThreadPool();
                    }
                });
    }

    // TODO fix duplication with CodecsModule
    private void installTransport(
            MapBinder<String, Transport.Factory<? extends Transport>> mapBinder,
            String name,
            Class<? extends Transport> transportClass) {

        final Class<? extends Transport.Config> configClass =
                (Class<? extends Transport.Config>)
                        findInnerClassAnnotatedWith(ConfigClass.class, transportClass, Transport.Config.class);

        final Class<? extends Transport.Factory<? extends Transport>> factoryClass =
                (Class<? extends Transport.Factory<? extends Transport>>)
                        findInnerClassAnnotatedWith(FactoryClass.class, transportClass, Transport.Factory.class);

        if (configClass == null || factoryClass == null) {
            throw new IllegalStateException("Missing annotations on transport class " + transportClass);
        }
        installTransport(mapBinder, name, transportClass, configClass, factoryClass);
    }

    private void installTransport(
            MapBinder<String, Transport.Factory<? extends Transport>> mapBinder,
            String name,
            Class<? extends Transport> transportClass,
            Class<? extends Transport.Config> configClass,
            Class<? extends Transport.Factory<? extends Transport>> factoryClass) {
        final Key<? extends Transport.Factory<? extends Transport>> factoryKey = Key.get(factoryClass);
        install(new FactoryModuleBuilder()
                        .implement(Transport.class, transportClass)
                        .implement(Transport.Config.class, configClass)
                        .build(factoryClass));

        mapBinder.addBinding(name).to(factoryKey);
    }

    private Class<?> findInnerClassAnnotatedWith(Class<? extends Annotation> annotationClass,
                                                     Class<? extends Transport> transportClass,
                                                     Class<?> targetClass) {
        final Class<?>[] declaredClasses = transportClass.getDeclaredClasses();
        Class<?> annotatedClass = null;
        for (final Class<?> declaredClass : declaredClasses) {
            if (!declaredClass.isAnnotationPresent(annotationClass)) {
                continue;
            }
            // must be subclass of Transport.Config
            if (targetClass.isAssignableFrom(declaredClass)) {
                // TODO log error if configClass is already assigned
                annotatedClass = declaredClass;
            } else {
                // TODO log error and skip transport
            }
        }
        return annotatedClass;
    }
}
