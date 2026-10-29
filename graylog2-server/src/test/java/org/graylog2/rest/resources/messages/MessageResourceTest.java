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
package org.graylog2.rest.resources.messages;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import org.graylog.plugins.netflow.codecs.NetFlowCodec;
import org.graylog2.indexer.indexset.registry.IndexSetRegistry;
import org.graylog2.indexer.messages.Messages;
import org.graylog2.indexer.results.ResultMessageFactory;
import org.graylog2.inputs.codecs.CodecFactory;
import org.graylog2.plugin.inputs.codecs.Codec;
import org.graylog2.rest.models.messages.requests.MessageParseRequest;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.UncheckedIOException;
import java.lang.reflect.Proxy;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MessageResourceTest {
    @Test
    void rejectsMultiMessageCodecWithoutCreatingIt() {
        final NetFlowCodec.Factory netFlowFactory = (NetFlowCodec.Factory) Proxy.newProxyInstance(
                NetFlowCodec.Factory.class.getClassLoader(), new Class<?>[]{NetFlowCodec.Factory.class},
                (proxy, method, args) -> {
                    throw new AssertionError("codec must not be created");
                });
        final MessageResource resource = resource(Map.of("netflow", netFlowFactory));

        assertThatThrownBy(() -> resource.parse(request("netflow", Map.of("netflow9_definitions_Path", "/etc"))))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void doesNotLeakCodecCreationErrors() {
        @SuppressWarnings("unchecked")
        final Codec.Factory<Codec> factory = mock(Codec.Factory.class);
        when(factory.create(any())).thenThrow(new UncheckedIOException(new FileNotFoundException("/etc (Is a directory)")));
        final MessageResource resource = resource(Map.of("custom", factory));

        assertThatThrownBy(() -> resource.parse(request("custom", Map.of("path", "/etc"))))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Could not decode message");
    }

    @Test
    void unknownCodecIsNotFound() {
        final MessageResource resource = resource(Map.of());

        assertThatThrownBy(() -> resource.parse(request("unknown", Map.of())))
                .isInstanceOf(NotFoundException.class);
    }

    private static MessageResource resource(Map<String, Codec.Factory<? extends Codec>> factories) {
        return new MessageResource(mock(Messages.class), new CodecFactory(factories), mock(IndexSetRegistry.class),
                mock(ResultMessageFactory.class));
    }

    private static MessageParseRequest request(String codec, Map<String, Object> configuration) {
        return MessageParseRequest.create("x", codec, "127.0.0.1", configuration);
    }
}
