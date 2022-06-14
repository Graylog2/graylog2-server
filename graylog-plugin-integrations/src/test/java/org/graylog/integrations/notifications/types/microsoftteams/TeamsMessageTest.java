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
package org.graylog.integrations.notifications.types.microsoftteams;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TeamsMessageTest {

    ObjectMapper objectMapper = new ObjectMapper();
    @Test
    public void validateTitle() throws IOException {
        JsonNode customMessage = objectMapper.readTree("{\"name\":\"Type\",\"value\":\"test-dummy-v1\"}");
        TeamsMessage message = new TeamsMessage("#FF0000", "", "\"**Alert Event Definition Test Title triggered:**\"",customMessage,"_Event Definition Test Description_" );
        String expected = message.getJsonString();
        List<String> text = getJsonNodeFieldValue(expected, "text");
        assertThat(text).isNotEmpty();
    }

    List<String> getJsonNodeFieldValue(String expected, String fieldName) throws IOException {
        final byte[] bytes = expected.getBytes();
        JsonNode jsonNode = new ObjectMapper().readTree(bytes);
        return jsonNode.findValuesAsText(fieldName);
    }
}
