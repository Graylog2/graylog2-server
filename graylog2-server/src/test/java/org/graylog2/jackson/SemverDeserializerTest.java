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
package org.graylog2.jackson;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.vdurmont.semver4j.Semver;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SemverDeserializerTest {
    private ObjectMapper objectMapper;

    @Before
    public void setUp() throws Exception {
        objectMapper = new ObjectMapper()
                .registerModule(new SimpleModule().addDeserializer(Semver.class, new SemverDeserializer()));
    }

    @Test
    public void successfullyDeserializesString() throws IOException {
        final Semver version = objectMapper.readValue("\"1.3.7-rc.2+build.2.b8f12d7\"", Semver.class);
        assertThat(version).isEqualTo(new Semver("1.3.7-rc.2+build.2.b8f12d7", Semver.SemverType.NPM));
    }

    @Test
    public void successfullyDeserializesInteger() throws IOException {
        final Semver version = objectMapper.readValue("5", Semver.class);
        assertThat(version).isEqualTo(new Semver("5", Semver.SemverType.LOOSE));
    }

    @Test
    public void successfullyDeserializesNull() throws IOException {
        final Semver version = objectMapper.readValue("null", Semver.class);
        assertThat(version).isNull();
    }

    @Test
    public void failsForInvalidVersion() {
        assertThatThrownBy(() -> objectMapper.readValue("\"foobar\"", Semver.class))
                .isInstanceOf(JsonMappingException.class)
                .hasMessageStartingWith("Invalid version (no major version): foobar");
    }

    @Test
    public void failsForInvalidType() {
        assertThatThrownBy(() -> objectMapper.readValue("[]", Semver.class))
                .isInstanceOf(JsonMappingException.class)
                .hasMessageStartingWith("Unexpected token (START_ARRAY), expected VALUE_STRING: expected String or Number");
    }
}