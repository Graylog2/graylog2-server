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
package org.graylog2.inputs.codecs;

import org.graylog.integrations.ipfix.codecs.IpfixCodec;
import org.graylog.plugins.netflow.codecs.NetFlowCodec;
import org.graylog2.plugin.inputs.codecs.Codec;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CodecFactoryTest {
    private final CodecFactory codecFactory = new CodecFactory(Map.of(
            "netflow", guiceLikeFactory(NetFlowCodec.Factory.class),
            "ipfix", guiceLikeFactory(IpfixCodec.Factory.class),
            "raw", guiceLikeFactory(RawCodec.Factory.class)
    ));

    @Test
    void detectsMultiMessageCodecs() {
        assertThat(codecFactory.isMultiMessageCodec("netflow")).isTrue();
        assertThat(codecFactory.isMultiMessageCodec("ipfix")).isTrue();
        assertThat(codecFactory.isMultiMessageCodec("raw")).isFalse();
        assertThat(codecFactory.isMultiMessageCodec("unknown")).isFalse();
    }

    @SuppressWarnings("unchecked")
    private static <F extends Codec.Factory<? extends Codec>> F guiceLikeFactory(Class<F> factoryInterface) {
        return (F) Proxy.newProxyInstance(factoryInterface.getClassLoader(), new Class<?>[]{factoryInterface},
                (proxy, method, args) -> {
                    throw new UnsupportedOperationException();
                });
    }
}
