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
package org.graylog2.indexer.searches;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class SearchFailureTest {

    @Test
    public void extractNumericFieldErrors() throws IOException {
        final String searchResult =
                " {\n" +
                "      \"took\" : 2,\n" +
                "      \"timed_out\" : false,\n" +
                "      \"_shards\" : {\n" +
                "        \"total\" : 64,\n" +
                "        \"failures\" : [\n" +
                "          {\n" +
                "            \"shard\" : 0,\n" +
                "            \"index\" : \"graylog_0\",\n" +
                "            \"reason\" : {\n" +
                "              \"type\" : \"query_shard_exception\",\n" +
                "              \"reason\" : \"failed to create query: { [QUERY] }\",\n" +
                "              \"index\" : \"graylog_0\",\n" +
                "              \"caused_by\" : {\n" +
                "                \"type\" : \"number_format_exception\",\n" +
                "                \"reason\" : \"For input string: \\\"BADQUERYSTRING\\\"\"\n" +
                "              }\n" +
                "            }\n" +
                "          }\n" +
                "        ]\n" +
                "      },\n" +
                "      \"hits\" : {\n" +
                "        \"total\" : 0,\n" +
                "        \"max_score\" : null,\n" +
                "        \"hits\" : [ ]\n" +
                "      }\n" +
                "    }\n";

        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(searchResult);
        final SearchFailure searchFailure = new SearchFailure(jsonNode.path("_shards"));
        final String expectedResult = "failed to create query: { [QUERY] }" +
                " caused_by: {\"type\":\"number_format_exception\",\"reason\":\"For input string: \\\"BADQUERYSTRING\\\"\"}";
        assertThat(searchFailure.getNonNumericFieldErrors()).hasSize(1);
        assertThat(searchFailure.getNonNumericFieldErrors().get(0)).isEqualTo(expectedResult);
        assertThat(searchFailure.getErrors().get(0)).isEqualTo(expectedResult);
    }
}
