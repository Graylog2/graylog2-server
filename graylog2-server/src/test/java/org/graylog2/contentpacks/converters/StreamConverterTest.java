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
package org.graylog2.contentpacks.converters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.bson.types.ObjectId;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.dashboards.DashboardImpl;
import org.graylog2.plugin.streams.Output;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.plugin.streams.StreamRuleType;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.streams.StreamImpl;
import org.graylog2.streams.StreamRuleImpl;
import org.graylog2.streams.matchers.StreamRuleMock;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class StreamConverterTest {
    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();
    private final StreamConverter converter = new StreamConverter(objectMapper);

    @Test
    public void convert() {
        final ImmutableMap<String, Object> streamFields = ImmutableMap.of(
                StreamImpl.FIELD_TITLE, "Stream Title"
        );

        final ImmutableMap<String, Object> streamRuleFields = ImmutableMap.<String, Object>builder()
                .put("_id", "1234567890")
                .put(StreamRuleImpl.FIELD_TYPE, StreamRuleType.EXACT.getValue())
                .put(StreamRuleImpl.FIELD_DESCRIPTION, "description")
                .put(StreamRuleImpl.FIELD_FIELD, "field")
                .put(StreamRuleImpl.FIELD_VALUE, "value")
                .put(StreamRuleImpl.FIELD_INVERTED, false)
                .put(StreamRuleImpl.FIELD_STREAM_ID, "1234567890")
                .build();
        final ImmutableList<StreamRule> streamRules = ImmutableList.of(
                new StreamRuleMock(streamRuleFields)
        );
        final ImmutableSet<Output> outputs = ImmutableSet.of();
        final ObjectId streamId = new ObjectId();
        final StreamImpl stream = new StreamImpl(streamId, streamFields, streamRules, outputs, null);
        final Entity entity = converter.convert(stream);

        assertThat(entity).isInstanceOf(EntityV1.class);
        assertThat(entity.id()).isEqualTo(ModelId.of(streamId.toHexString()));
        assertThat(entity.type()).isEqualTo(ModelType.of("stream"));

        final EntityV1 entityV1 = (EntityV1) entity;
        final JsonNode data = entityV1.data();
//        assertThat(data.path("")).isEqualTo(null);
    }
}