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
import com.google.inject.Provider;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Names;
import org.graylog2.plugin.inject.Graylog2Module;
import org.graylog2.plugin.inputs.transports.Transport;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class TransportsModule extends Graylog2Module {
    protected void configure() {
        final MapBinder<String, Transport.Factory<? extends Transport>> mapBinder = transportMapBinder();

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
        installTransport(mapBinder, "syslog-tcp", SyslogTcpTransport.class);

        // TODO Add instrumentation to ExecutorService and ThreadFactory
        bind(Executor.class)
                .annotatedWith(Names.named("bossPool"))
                .toInstance(Executors.newCachedThreadPool(new ThreadFactoryBuilder()
                                                                  .setNameFormat("transport-boss-%d")
                                                                  .build()));

        // TODO Add instrumentation to ExecutorService and ThreadFactory
        bind(Executor.class)
                .annotatedWith(Names.named("cached"))
                .toProvider(new Provider<Executor>() {
                    @Override
                    public Executor get() {
                        return Executors.newCachedThreadPool();
                    }
                });
    }

}
