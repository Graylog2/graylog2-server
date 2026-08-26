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
package org.graylog.collectors.config.exporter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class ExporterSendingQueueTest {
    private final ObjectMapper yamlObjectMapper = new ObjectMapper(new YAMLFactory()
            .enable(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE))
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .registerModule(new Jdk8Module());

    @Test
    void defaultValues() {
        final var config = ExporterSendingQueue.createDefault();

        assertThat(config.enabled()).isTrue();
        assertThat(config.numConsumers()).isEqualTo(10);
        assertThat(config.waitForResult()).isFalse();
        assertThat(config.blockOnOverflow()).isFalse();
        assertThat(config.sizer()).isEqualTo(ExporterSendingQueue.Sizer.BYTES);
        assertThat(config.queueSize()).isEqualTo(67108864);
        assertThat(config.storage()).contains("file_storage/default");
        assertThat(config.batch()).contains(ExporterSendingQueue.Batch.createDefault());
    }

    @Test
    void batchDefaultValues() {
        final var batch = ExporterSendingQueue.Batch.createDefault();

        assertThat(batch.flushTimeout()).isEqualTo(Duration.ofMillis(200));
        assertThat(batch.minSize()).isEqualTo(1048576);
        assertThat(batch.maxSize()).isEqualTo(3145728);
        assertThat(batch.sizer()).contains(ExporterSendingQueue.Sizer.BYTES);
    }

    @Test
    void yamlSerializationOfDefaultConfig() throws Exception {
        final var yaml = yamlObjectMapper.writeValueAsString(ExporterSendingQueue.createDefault());

        // Check that the duration is correctly serialized as Go duration
        assertThat(yaml).contains("flush_timeout: \"200ms\"");
    }
}
