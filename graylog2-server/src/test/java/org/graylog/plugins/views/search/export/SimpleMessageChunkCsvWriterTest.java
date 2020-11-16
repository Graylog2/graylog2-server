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
package org.graylog.plugins.views.search.export;

import org.graylog.plugins.views.search.Search;
import org.graylog2.rest.MoreMediaTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.plugins.views.search.export.LinkedHashSetUtil.linkedHashSetOf;
import static org.graylog.plugins.views.search.export.TestData.simpleMessage;
import static org.graylog.plugins.views.search.export.TestData.simpleMessageChunk;

class SimpleMessageChunkCsvWriterTest {
    private SimpleMessageChunkCsvWriter sut;

    @BeforeEach
    void setUp() {
        sut = new SimpleMessageChunkCsvWriter();
    }

    @Test
    void isWritableForSimpleMessages() {
        boolean isWritable = sut.isWriteable(SimpleMessageChunk.class, null, null, MoreMediaTypes.TEXT_CSV_TYPE);
        assertThat(isWritable).isTrue();
    }

    @Test
    void isWritableForSimpleMessagesAutoValueType() {
        boolean isWritable = sut.isWriteable(AutoValue_SimpleMessageChunk.class, SimpleMessageChunk.class, null, MoreMediaTypes.TEXT_CSV_TYPE);
        assertThat(isWritable).isTrue();
    }

    @Test
    void isNotWritableForOtherClasses() {
        boolean isWritable = sut.isWriteable(Search.class, null, null, MoreMediaTypes.TEXT_CSV_TYPE);
        assertThat(isWritable).isFalse();
    }

    @Test
    void writesCsv() {
        SimpleMessageChunk chunk = TestData.simpleMessageChunk("timestamp,source,message",
                new Object[]{"2015-01-01 01:00:00.000", "source-1", "Behold the tap dancing chimp!"},
                new Object[]{"2015-01-02 01:00:00.000", "source-2", "Behold the yodelling parrot!"});

        String result = write(chunk);

        assertThat(result.split("\n"))
                .containsExactly(
                        "\"2015-01-01 01:00:00.000\",\"source-1\",\"Behold the tap dancing chimp!\"",
                        "\"2015-01-02 01:00:00.000\",\"source-2\",\"Behold the yodelling parrot!\"");
    }

    @ParameterizedTest
    @CsvSource(
            value = {
                    "aaa, bbb#\"aaa, bbb\"",
                    "aaa\\n bbb#\"aaa\\n bbb\"",
                    "aaa\"bbb#\"aaa\"\"bbb\""
            }, delimiter = '#'
    )
    void quotesCertainStrings(String value, String expected) {
        SimpleMessageChunk chunk = simpleMessageChunk("message", new Object[]{value});

        String result = write(chunk);

        assertThat(result).startsWith(expected);
    }

    @Test
    void leavesMissingFieldEmpty() {
        SimpleMessageChunk chunk = SimpleMessageChunk.from(
                linkedHashSetOf("timestamp", "source", "message"),
                simpleMessage("timestamp,source,message", new Object[]{"2015-01-01 01:00:00.000", "source-1", "some text"}),
                simpleMessage("timestamp,message", new Object[]{"2015-01-02 01:00:00.000", "more text"}));

        String result = write(chunk);

        assertThat(result.split("\n"))
                .containsExactly(
                        "\"2015-01-01 01:00:00.000\",\"source-1\",\"some text\"",
                        "\"2015-01-02 01:00:00.000\",,\"more text\"");
    }

    @Test
    void writesHeaderForFirstChunk() {
        SimpleMessageChunk chunk = simpleMessageChunk("timestamp, source, message",
                new Object[]{"2015-01-01 01:00:00.000", "source-1", "some text"});
        SimpleMessageChunk firstChunk = chunk.toBuilder().isFirstChunk(true).build();

        String result = write(firstChunk);

        assertThat(result.split("\n"))
                .containsExactly(
                        "\"timestamp\",\"source\",\"message\"",
                        "\"2015-01-01 01:00:00.000\",\"source-1\",\"some text\"");
    }

    private String write(SimpleMessageChunk chunk) {
        ByteArrayOutputStream entityStream = new ByteArrayOutputStream();
        try {
            sut.writeTo(chunk, SimpleMessageChunk.class, null, null, null, null, entityStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new String(entityStream.toByteArray(), StandardCharsets.UTF_8);
    }
}
