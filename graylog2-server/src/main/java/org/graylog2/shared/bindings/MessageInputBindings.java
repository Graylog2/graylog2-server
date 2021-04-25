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
package org.graylog2.shared.bindings;

import com.google.inject.multibindings.MapBinder;
import org.graylog.plugins.beats.BeatsInputPluginModule;
import org.graylog2.inputs.codecs.CodecsModule;
import org.graylog2.inputs.gelf.amqp.GELFAMQPInput;
import org.graylog2.inputs.gelf.http.GELFHttpInput;
import org.graylog2.inputs.gelf.kafka.GELFKafkaInput;
import org.graylog2.inputs.gelf.tcp.GELFTCPInput;
import org.graylog2.inputs.gelf.udp.GELFUDPInput;
import org.graylog2.inputs.misc.jsonpath.JsonPathInput;
import org.graylog2.inputs.random.FakeHttpMessageInput;
import org.graylog2.inputs.raw.amqp.RawAMQPInput;
import org.graylog2.inputs.raw.kafka.RawKafkaInput;
import org.graylog2.inputs.raw.tcp.RawTCPInput;
import org.graylog2.inputs.raw.udp.RawUDPInput;
import org.graylog2.inputs.syslog.amqp.SyslogAMQPInput;
import org.graylog2.inputs.syslog.kafka.SyslogKafkaInput;
import org.graylog2.inputs.syslog.tcp.SyslogTCPInput;
import org.graylog2.inputs.syslog.udp.SyslogUDPInput;
import org.graylog2.inputs.transports.TransportsModule;
import org.graylog2.plugin.inject.Graylog2Module;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.inputs.gelf.http.GELFHttpBatchInput;

public class MessageInputBindings extends Graylog2Module {
    @Override
    protected void configure() {
        install(new TransportsModule());
        install(new CodecsModule());

        final MapBinder<String, MessageInput.Factory<? extends MessageInput>> inputMapBinder = inputsMapBinder();
        // new style inputs, using transports and codecs
        installInput(inputMapBinder, RawTCPInput.class, RawTCPInput.Factory.class);
        installInput(inputMapBinder, RawUDPInput.class, RawUDPInput.Factory.class);
        installInput(inputMapBinder, RawAMQPInput.class, RawAMQPInput.Factory.class);
        installInput(inputMapBinder, RawKafkaInput.class, RawKafkaInput.Factory.class);
        installInput(inputMapBinder, SyslogTCPInput.class, SyslogTCPInput.Factory.class);
        installInput(inputMapBinder, SyslogUDPInput.class, SyslogUDPInput.Factory.class);
        installInput(inputMapBinder, SyslogAMQPInput.class, SyslogAMQPInput.Factory.class);
        installInput(inputMapBinder, SyslogKafkaInput.class, SyslogKafkaInput.Factory.class);
        installInput(inputMapBinder, FakeHttpMessageInput.class, FakeHttpMessageInput.Factory.class);
        installInput(inputMapBinder, GELFTCPInput.class, GELFTCPInput.Factory.class);
        installInput(inputMapBinder, GELFHttpInput.class, GELFHttpInput.Factory.class);
        installInput(inputMapBinder, GELFUDPInput.class, GELFUDPInput.Factory.class);
        installInput(inputMapBinder, GELFAMQPInput.class, GELFAMQPInput.Factory.class);
        installInput(inputMapBinder, GELFKafkaInput.class, GELFKafkaInput.Factory.class);
        installInput(inputMapBinder, JsonPathInput.class, JsonPathInput.Factory.class);
        installInput(inputMapBinder, GELFHttpBatchInput.class, GELFHttpBatchInput.Factory.class);

        install(new BeatsInputPluginModule());
    }
}
