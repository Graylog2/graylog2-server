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

import org.graylog2.inputs.codecs.gelf.GELFBulkDroppedMsgService;
import org.graylog2.plugin.MessageFactory;
import org.graylog2.plugin.TestMessageFactory;
import org.graylog2.plugin.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class GelfCodecTest {

    @Mock
    private GelfChunkAggregator aggregator;

    @Mock
    private GELFBulkDroppedMsgService gelfBulkDroppedMsgService;

    private GelfCodec codec;

    private final MessageFactory messageFactory = new TestMessageFactory();

    @Before
    public void setUp() {
        codec = new GelfCodec(new Configuration(Collections.emptyMap()), aggregator, messageFactory, gelfBulkDroppedMsgService);
    }

    @Test
    public void getAggregatorReturnsGelfChunkAggregator() {
        assertThat(codec.getAggregator()).isSameAs(aggregator);
    }
}
