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
package org.graylog.plugins.cef.codec;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import org.assertj.core.api.AbstractObjectAssert;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageFactory;
import org.graylog2.plugin.TestMessageFactory;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.journal.RawMessage;
import org.joda.time.DateTime;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

// need to suppress because we use the test parameters in the test name format string
@SuppressWarnings("unused")
public class CEFCodecFixturesTest {
    private final MessageFactory messageFactory = new TestMessageFactory();

    public static class Fixture {
        public String testString;
        public String description;

        // Configuration of the codec
        public Map<String, Object> codecConfiguration = Collections.emptyMap();

        // Remote address of the raw message
        public String remoteAddress;

        // Expected message fields
        public Date expectedTimestamp;
        public String expectedSource;

        // Expected CEF fields
        public Date timestamp;
        public String host;
        public int cefVersion;
        public String deviceVendor;
        public String deviceProduct;
        public String deviceVersion;
        public String deviceEventClassId;
        public String name;
        public String severity;
        public Map<String, Object> extensions = Collections.emptyMap();

        @Override
        public String toString() {
            return new StringJoiner(", ", this.getClass().getSimpleName() + "[", "]")
                    .add("description = " + description)
                    .add("CEF = " + testString)
                    .toString();
        }
    }

    public static Collection<Object[]> data() throws Exception {
        final ObjectMapper mapper = new ObjectMapper()
                .enable(JsonParser.Feature.ALLOW_COMMENTS)
                .enable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS)
                .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                .enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
        final URL fixturesURL = Resources.getResource("fixtures");
        final Path fixturesPath = Paths.get(fixturesURL.toURI());
        final File[] fixtureFiles;
        try (Stream<Path> stream = Files.list(fixturesPath)) {
            fixtureFiles = stream.map(Path::toFile)
                    .toArray(File[]::new);
        }

        final List<Object[]> fixtures = new ArrayList<>(fixtureFiles.length);
        for (File fixtureFile : fixtureFiles) {
            final Fixture fixture = mapper.readValue(fixtureFile, Fixture.class);
            fixtures.add(new Object[]{fixture, fixtureFile.getName(), fixture.description});
        }

        return fixtures;
    }

    /**
     * JUnit does not support injecting parameters from parameterized tests into its lifecycle methods.
     * Those are meant to set up things that are independent of actual tests, so we need to have a helper method.
     *
     * @param fixture the current fixture
     * @return the parsed message
     */
    private Message initCEFCodecFixturesTest(Fixture fixture) {
        final byte[] bytes = fixture.testString.getBytes(StandardCharsets.UTF_8);
        final InetSocketAddress remoteAddress = fixture.remoteAddress == null ? null : new InetSocketAddress(fixture.remoteAddress, 0);
        final RawMessage rawMessage = new RawMessage(bytes, remoteAddress);
        final CEFCodec codec = new CEFCodec(new Configuration(fixture.codecConfiguration), messageFactory);

        var message = codec.decodeSafe(rawMessage);
        assertThat(message).isPresent();
        return message.get();
    }

    @MethodSource("data")
    @ParameterizedTest(name = "[{1}] {2}")
    public void timestamp(Fixture fixture, String fileName, String description) throws Exception {
        var message = initCEFCodecFixturesTest(fixture);
        if (fixture.expectedTimestamp != null) {
            assertThat(message.getTimestamp().toDate()).isEqualTo(fixture.expectedTimestamp);
        }
    }

    @MethodSource("data")
    @ParameterizedTest(name = "[{1}] {2}")
    public void source(Fixture fixture, String fileName, String description) throws Exception {
        var message = initCEFCodecFixturesTest(fixture);
        if (fixture.expectedSource != null) {
            assertThat(message.getSource()).isEqualTo(fixture.expectedSource);
        }
    }

    private void containsEntry(Message message, String name, Object value) {
        if (value != null) {
            assertThat(message.getFields()).containsEntry(name, value);
        }
    }

    @MethodSource("data")
    @ParameterizedTest(name = "[{1}] {2}")
    public void deviceVendor(Fixture fixture, String fileName, String description) throws Exception {
        var message = initCEFCodecFixturesTest(fixture);
        containsEntry(message, "device_vendor", fixture.deviceVendor);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "[{1}] {2}")
    public void deviceProduct(Fixture fixture, String fileName, String description) throws Exception {
        var message = initCEFCodecFixturesTest(fixture);
        containsEntry(message, "device_product", fixture.deviceProduct);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "[{1}] {2}")
    public void deviceVersion(Fixture fixture, String fileName, String description) throws Exception {
        var message = initCEFCodecFixturesTest(fixture);
        containsEntry(message, "device_version", fixture.deviceVersion);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "[{1}] {2}")
    public void deviceEventClassId(Fixture fixture, String fileName, String description) throws Exception {
        var message = initCEFCodecFixturesTest(fixture);
        containsEntry(message, "event_class_id", fixture.deviceEventClassId);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "[{1}] {2}")
    public void name(Fixture fixture, String fileName, String description) throws Exception {
        var message = initCEFCodecFixturesTest(fixture);
        containsEntry(message, "name", fixture.name);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "[{1}] {2}")
    public void severity(Fixture fixture, String fileName, String description) throws Exception {
        var message = initCEFCodecFixturesTest(fixture);
        containsEntry(message, "severity", fixture.severity);
    }

    @MethodSource("data")
    @ParameterizedTest(name = "[{1}] {2}")
    public void extensions(Fixture fixture, String fileName, String description) throws Exception {
        var message = initCEFCodecFixturesTest(fixture);
        final Map<String, Object> extensions = fixture.extensions;
        if (!extensions.isEmpty()) {
            for (Map.Entry<String, Object> extension : extensions.entrySet()) {
                assertThat(message.getFields()).containsKey(extension.getKey());

                // Because Java type system...
                final Object fieldContent = message.getField(extension.getKey());
                final AbstractObjectAssert<?, Object> assertFieldContent = assertThat(fieldContent)
                        .describedAs(extension.getKey());
                if (fieldContent instanceof Integer) {
                    assertFieldContent.isEqualTo(((Number) extensions.get(extension.getKey())).intValue());
                } else if (fieldContent instanceof Long) {
                    assertFieldContent.isEqualTo(((Number) extensions.get(extension.getKey())).longValue());
                } else if (fieldContent instanceof Float) {
                    assertFieldContent.isEqualTo(((Number) extensions.get(extension.getKey())).floatValue());
                } else if (fieldContent instanceof Double) {
                    assertFieldContent.isEqualTo(((Number) extensions.get(extension.getKey())).doubleValue());
                } else if (fieldContent instanceof DateTime) {
                    assertFieldContent.isEqualTo(DateTime.parse((String) extensions.get(extension.getKey())));
                } else {
                    assertFieldContent.isEqualTo(extensions.get(extension.getKey()));
                }
            }
        }
    }
}
