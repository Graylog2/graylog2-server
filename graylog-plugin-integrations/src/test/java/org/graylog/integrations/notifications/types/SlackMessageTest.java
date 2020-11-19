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
package org.graylog.integrations.notifications.types;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


public class SlackMessageTest {


    @Test
    public void test_good_usename() throws IOException {
        SlackMessage message = new SlackMessage("#FF2052", ":turtle:", "https://media.defcon.org/DEF CON 1/DEF CON 1 logo.jpg", "aaa", "#general", false, "this is a happy message", "This is a happy custom message");
        String expected = message.getJsonString();
        List<String> username = getJsonNodeFieldValue(expected, "username");
        assertThat(username).isNotEmpty();
        assertThat(username).isNotNull();
    }

    @Test
    public void test_empty_usernames() throws IOException {
        SlackMessage message = new SlackMessage("#FF2052", ":turtle:", "https://media.defcon.org/DEF CON 1/DEF CON 1 logo.jpg", null, "aaa", false, "sss", "sss");
        String anotherMessage = message.getJsonString();
        List<String> userNames = getJsonNodeFieldValue(anotherMessage, "username");
        assertThat(userNames).isEmpty();
        assertThat(userNames).isNotNull();

    }

    List<String> getJsonNodeFieldValue(String expected, String fieldName) throws IOException {
        final byte[] bytes = expected.getBytes();
        JsonNode jsonNode = new ObjectMapper().readTree(bytes);
        return jsonNode.findValuesAsText(fieldName);
    }


}