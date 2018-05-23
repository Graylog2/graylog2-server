/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
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
        String json = "{\"type\":{\"type\":\"string\",\"value\":\"relative\"},\"range\":{\"type\":\"integer\",\"value\":300}}";
        final TimeRangeEntity timeRangeEntity = objectMapper.readValue(json, TimeRangeEntity.class);
        assertThat(timeRangeEntity).isNotNull();
    }

    @Test
    public void testEmbeddedDeserialization() throws IOException {
        String json = "{" +
                "  \"cache_time\" : {" +
                "     \"value\" : 120," +
                "     \"type\" : \"integer\"" +
                "  }," +
                "  \"position\" : null," +
                "  \"description\" : {" +
                "     \"value\" : \"Histogram\"," +
                "     \"type\" : \"string\"" +
                "  }," +
                "  \"time_range\" : {" +
                "     \"type\" : {" +
                "        \"type\" : \"string\"," +
                "        \"value\" : \"relative\"" +
                "     }," +
                "     \"range\" : {" +
                "        \"type\" : \"integer\"," +
                "        \"value\" : 300" +
                "     }" +
                "  }," +
                "  \"type\" : {" +
                "     \"value\" : \"SEARCH_RESULT_CHART\"," +
                "     \"type\" : \"string\"" +
                "  }," +
                "  \"configuration\" : {" +
                "     \"timerange\" : {" +
                "        \"range\" : {" +
                "           \"value\" : 300," +
                "           \"type\" : \"integer\"" +
                "        }," +
                "        \"type\" : {" +
                "           \"value\" : \"relative\"," +
                "           \"type\" : \"string\"" +
                "        }" +
                "     }," +
                "     \"query\" : {" +
                "        \"value\" : \"\"," +
                "        \"type\" : \"string\"" +
                "     }," +
                "     \"interval\" : {" +
                "        \"type\" : \"string\"," +
                "        \"value\" : \"minute\"" +
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