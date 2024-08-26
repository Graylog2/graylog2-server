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
package org.graylog2.inputs.transports;

import com.google.inject.multibindings.MapBinder;
import io.netty.channel.EventLoopGroup;
import org.graylog2.inputs.transports.netty.EventLoopGroupFactory;
import org.graylog2.inputs.transports.netty.EventLoopGroupProvider;
import org.graylog2.plugin.inject.Graylog2Module;
import org.graylog2.plugin.inputs.transports.Transport;

public class TransportsModule extends Graylog2Module {
    @Override
    protected void configure() {
        final MapBinder<String, Transport.Factory<? extends Transport>> mapBinder = transportMapBinder();

        installTransport(mapBinder, "udp", UdpTransport.class);
        installTransport(mapBinder, "tcp", TcpTransport.class);
        installTransport(mapBinder, "http", HttpTransport.class);
        installTransport(mapBinder, "randomhttp", RandomMessageTransport.class);
        installTransport(mapBinder, "kafka", KafkaTransport.class);
        installTransport(mapBinder, "amqp", AmqpTransport.class);
        installTransport(mapBinder, "httppoll", HttpPollTransport.class);
        installTransport(mapBinder, "syslog-tcp", SyslogTcpTransport.class);

        bind(EventLoopGroupFactory.class).asEagerSingleton();
        bind(EventLoopGroup.class).toProvider(EventLoopGroupProvider.class).asEagerSingleton();
    }
}
