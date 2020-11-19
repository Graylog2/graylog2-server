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
package org.graylog2.contentpacks.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog2.contentpacks.model.entities.DashboardWidgetEntity;
import org.graylog2.contentpacks.model.entities.RelativeRangeEntity;
import org.graylog2.contentpacks.model.entities.TimeRangeEntity;
import org.graylog2.plugin.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class ValueReferenceTypeIdResolverTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testDeserialization() throws IOException {
        String json = "{\"type\":{\"@type\":\"string\",\"@value\":\"relative\"},\"range\":{\"@type\":\"integer\",\"@value\":300}}";
        final TimeRangeEntity timeRangeEntity = objectMapper.readValue(json, TimeRangeEntity.class);
        assertThat(timeRangeEntity).isNotNull();
    }

    @Test
    public void testEmbeddedDeserialization() throws IOException {
        String json = "{" +
                "  \"id\" : {" +
                "     \"@value\" : \"12345\"," +
                "     \"@type\" : \"string\"" +
                "  }," +
                "  \"cache_time\" : {" +
                "     \"@value\" : 120," +
                "     \"@type\" : \"integer\"" +
                "  }," +
                "  \"position\" : null," +
                "  \"description\" : {" +
                "     \"@value\" : \"Histogram\"," +
                "     \"@type\" : \"string\"" +
                "  }," +
                "  \"time_range\" : {" +
                "     \"type\" : {" +
                "        \"@type\" : \"string\"," +
                "        \"@value\" : \"relative\"" +
                "     }," +
                "     \"range\" : {" +
                "        \"@type\" : \"integer\"," +
                "        \"@value\" : 300" +
                "     }" +
                "  }," +
                "  \"type\" : {" +
                "     \"@value\" : \"SEARCH_RESULT_CHART\"," +
                "     \"@type\" : \"string\"" +
                "  }," +
                "  \"configuration\" : {" +
                "     \"timerange\" : {" +
                "        \"range\" : {" +
                "           \"@value\" : 300," +
                "           \"@type\" : \"integer\"" +
                "        }," +
                "        \"type\" : {" +
                "           \"@value\" : \"relative\"," +
                "           \"@type\" : \"string\"" +
                "        }" +
                "     }," +
                "     \"query\" : {" +
                "        \"@value\" : \"\"," +
                "        \"@type\" : \"string\"" +
                "     }," +
                "     \"interval\" : {" +
                "        \"@type\" : \"string\"," +
                "        \"@value\" : \"minute\"" +
                "     }" +
                "  }" +
                "}";
        final DashboardWidgetEntity entity = objectMapper.readValue(json, DashboardWidgetEntity.class);
        assertThat(entity).isNotNull();
    }

    @Test
    public void testSerialization() throws IOException, InvalidRangeParametersException {
        final RelativeRangeEntity relativeRange = RelativeRangeEntity.of(RelativeRange.create(300));
        final String json = objectMapper.writeValueAsString(relativeRange);
        assertThat(json).isNotNull();
    }
}