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

import org.graylog.inputs.otel.bindings.OTelModule;
import org.graylog.plugins.beats.BeatsInputPluginModule;
import org.graylog2.inputs.beats.kafka.BeatsKafkaInput;
import org.graylog2.inputs.codecs.CodecsModule;
import org.graylog2.inputs.gelf.amqp.GELFAMQPInput;
import org.graylog2.inputs.gelf.http.GELFHttpInput;
import org.graylog2.inputs.gelf.kafka.GELFKafkaInput;
import org.graylog2.inputs.gelf.tcp.GELFTCPInput;
import org.graylog2.inputs.gelf.udp.GELFUDPInput;
import org.graylog2.inputs.misc.jsonpath.JsonPathInput;
import org.graylog2.inputs.random.FakeHttpMessageInput;
import org.graylog2.inputs.raw.amqp.RawAMQPInput;
import org.graylog2.inputs.raw.http.RawHttpInput;
import org.graylog2.inputs.raw.kafka.RawKafkaInput;
import org.graylog2.inputs.raw.tcp.RawTCPInput;
import org.graylog2.inputs.raw.udp.RawUDPInput;
import org.graylog2.inputs.syslog.amqp.SyslogAMQPInput;
import org.graylog2.inputs.syslog.kafka.SyslogKafkaInput;
import org.graylog2.inputs.syslog.tcp.SyslogTCPInput;
import org.graylog2.inputs.syslog.udp.SyslogUDPInput;
import org.graylog2.inputs.transports.TransportsModule;
import org.graylog2.plugin.inject.Graylog2Module;

public class MessageInputBindings extends Graylog2Module {
    @Override
    protected void configure() {
        install(new TransportsModule());
        install(new CodecsModule());

        addMessageInput(RawTCPInput.class);
        addMessageInput(RawUDPInput.class);
        addMessageInput(RawAMQPInput.class);
        addMessageInput(RawHttpInput.class);
        addMessageInput(RawKafkaInput.class);
        addMessageInput(SyslogTCPInput.class);
        addMessageInput(SyslogUDPInput.class);
        addMessageInput(SyslogAMQPInput.class);
        addMessageInput(SyslogKafkaInput.class);
        addMessageInput(FakeHttpMessageInput.class);
        addMessageInput(GELFTCPInput.class);
        addMessageInput(GELFHttpInput.class);
        addMessageInput(GELFUDPInput.class);
        addMessageInput(GELFAMQPInput.class);
        addMessageInput(GELFKafkaInput.class);
        addMessageInput(JsonPathInput.class);
        addMessageInput(BeatsKafkaInput.class);

        install(new BeatsInputPluginModule());
        install(new OTelModule());
    }
}
