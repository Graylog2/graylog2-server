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
package org.graylog.integrations.aws;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.integrations.aws.codecs.KinesisCloudWatchFlowLogCodec;
import org.graylog.integrations.aws.codecs.KinesisRawLogCodec;
import org.graylog2.plugin.TestMessageFactory;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.codecs.Codec;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

public class AWSTestingUtils {

    public static DateTime CLOUD_WATCH_TIMESTAMP = new DateTime("2019-06-05T12:35:44.000Z", DateTimeZone.UTC);

    // Non-instantiable utils class.
    private AWSTestingUtils() {
    }

    public static Map<String, Codec.Factory<? extends Codec>> buildTestCodecs() {

        // Prepare test codecs. These have to be manually instantiated for the test context.
        Map<String, Codec.Factory<? extends Codec>> availableCodecs = new HashMap<>();
        ObjectMapper objectMapper = new ObjectMapperProvider().get();
        availableCodecs.put(KinesisRawLogCodec.NAME, new KinesisRawLogCodec.Factory() {
            @Override
            public KinesisRawLogCodec create(Configuration configuration) {
                return new KinesisRawLogCodec(configuration, objectMapper, new TestMessageFactory());
            }

            @Override
            public KinesisRawLogCodec.Config getConfig() {
                return null;
            }

            @Override
            public Codec.Descriptor getDescriptor() {
                return null;
            }
        });

        availableCodecs.put(KinesisCloudWatchFlowLogCodec.NAME, new KinesisCloudWatchFlowLogCodec.Factory() {
            @Override
            public KinesisCloudWatchFlowLogCodec create(Configuration configuration) {
                return new KinesisCloudWatchFlowLogCodec(configuration, objectMapper, new TestMessageFactory());
            }

            @Override
            public KinesisCloudWatchFlowLogCodec.Config getConfig() {
                return null;
            }

            @Override
            public Codec.Descriptor getDescriptor() {
                return null;
            }
        });

        return availableCodecs;
    }

    /**
     * Build a data payload for a Flow Log CloudWatch Kinesis subscription record.
     *
     * @see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/logs/SubscriptionFilters.html">CloudWatch Subcription Filters</a>
     */
    public static byte[] cloudWatchFlowLogPayload() throws IOException {

        final String messageData = "{\n" +
                                   "  \"messageType\": \"DATA_MESSAGE\",\n" +
                                   "  \"owner\": \"459220251735\",\n" +
                                   "  \"logGroup\": \"test-flowlogs\",\n" +
                                   "  \"logStream\": \"eni-3423-all\",\n" +
                                   "  \"subscriptionFilters\": [\n" +
                                   "    \"filter\"\n" +
                                   "  ],\n" +
                                   "  \"logEvents\": [\n" +
                                   "    {\n" +
                                   "      \"id\": \"3423\",\n" +
                                   "      \"timestamp\": " + CLOUD_WATCH_TIMESTAMP.getMillis() + ",\n" +
                                   "      \"message\": \"2 423432432432 eni-3244234 172.1.1.2 172.1.1.2 80 2264 6 1 52 1559738144 1559738204 ACCEPT OK\"\n" +
                                   "    },\n" +
                                   "    {\n" +
                                   "      \"id\": \"3423\",\n" +
                                   "      \"timestamp\": " + CLOUD_WATCH_TIMESTAMP.getMillis() + ",\n" +
                                   "      \"message\": \"2 423432432432 eni-3244234 172.1.1.2 172.1.1.2 80 2264 6 1 52 1559738144 1559738204 ACCEPT OK\"\n" +
                                   "    }\n" +
                                   "  ]\n" +
                                   "}";

        // Compress the test record, as CloudWatch subscriptions are compressed.
        return compressPayload(messageData);
    }

    /**
     * Build a data payload for a raw CloudWatch Kinesis subscription record.
     *
     * @see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/logs/SubscriptionFilters.html">CloudWatch Subcription Filters</a>
     */
    public static byte[] cloudWatchRawPayload() throws IOException {

        final String messageData = "{\n" +
                                   "  \"messageType\": \"DATA_MESSAGE\",\n" +
                                   "  \"owner\": \"459220251735\",\n" +
                                   "  \"logGroup\": \"test-flowlogs\",\n" +
                                   "  \"logStream\": \"eni-3423-all\",\n" +
                                   "  \"subscriptionFilters\": [\n" +
                                   "    \"filter\"\n" +
                                   "  ],\n" +
                                   "  \"logEvents\": [\n" +
                                   "    {\n" +
                                   "      \"id\": \"3423\",\n" +
                                   "      \"timestamp\": " + CLOUD_WATCH_TIMESTAMP.getMillis() + ",\n" +
                                   "      \"message\": \"Just a raw message\"\n" +
                                   "    },\n" +
                                   "    {\n" +
                                   "      \"id\": \"3423\",\n" +
                                   "      \"timestamp\": " + CLOUD_WATCH_TIMESTAMP.getMillis() + ",\n" +
                                   "      \"message\": \"Just another raw message\"\n" +
                                   "    }\n" +
                                   "  ]\n" +
                                   "}";

        // Compress the test record, as CloudWatch subscriptions are compressed.
        return compressPayload(messageData);
    }

    private static byte[] compressPayload(String messageData) throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream(messageData.getBytes(StandardCharsets.UTF_8).length);
        final GZIPOutputStream gzip = new GZIPOutputStream(bos);
        gzip.write(messageData.getBytes(StandardCharsets.UTF_8));
        gzip.close();
        final byte[] compressed = bos.toByteArray();
        bos.close();
        return compressed;
    }
}
