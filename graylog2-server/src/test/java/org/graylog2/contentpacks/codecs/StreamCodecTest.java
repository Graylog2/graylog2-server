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
package org.graylog2.contentpacks.codecs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.bson.types.ObjectId;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.EntityWithConstraints;
import org.graylog2.contentpacks.model.entities.StreamEntity;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.dashboards.DashboardImpl;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.plugin.streams.Output;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.plugin.streams.StreamRuleType;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.streams.StreamImpl;
import org.graylog2.streams.StreamRuleImpl;
import org.graylog2.streams.StreamRuleService;
import org.graylog2.streams.StreamService;
import org.graylog2.streams.matchers.StreamRuleMock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.assertj.core.api.Assertions.assertThat;

public class StreamCodecTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();
    @Mock
    private StreamService streamService;
    @Mock
    private StreamRuleService streamRuleService;
    @Mock
    private IndexSetService indexSetService;

    private StreamCodec codec;

    @Before
    public void setUp() {
        codec = new StreamCodec(objectMapper, streamService, streamRuleService, indexSetService);
    }

    @Test
    public void encode() {
        final ImmutableMap<String, Object> streamFields = ImmutableMap.of(
                StreamImpl.FIELD_TITLE, "Stream Title",
                StreamImpl.FIELD_DESCRIPTION, "Stream Description",
                StreamImpl.FIELD_DISABLED, false
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
        final EntityWithConstraints entityWithConstraints = codec.encode(stream);
        final Entity entity = entityWithConstraints.entity();

        assertThat(entity).isInstanceOf(EntityV1.class);
        assertThat(entity.id()).isEqualTo(ModelId.of(streamId.toHexString()));
        assertThat(entity.type()).isEqualTo(ModelTypes.STREAM);

        final EntityV1 entityV1 = (EntityV1) entity;
        final StreamEntity streamEntity = objectMapper.convertValue(entityV1.data(), StreamEntity.class);
        assertThat(streamEntity.title()).isEqualTo(ValueReference.of("Stream Title"));
        assertThat(streamEntity.description()).isEqualTo(ValueReference.of("Stream Description"));
        assertThat(streamEntity.disabled()).isEqualTo(ValueReference.of(false));
        assertThat(streamEntity.streamRules()).hasSize(1);
    }

    @Test
    public void createExcerpt() {
        final ImmutableMap<String, Object> fields = ImmutableMap.of(
                DashboardImpl.FIELD_TITLE, "Stream Title"
        );
        final StreamImpl stream = new StreamImpl(fields);
        final EntityExcerpt excerpt = codec.createExcerpt(stream);

        assertThat(excerpt.id()).isEqualTo(ModelId.of(stream.getId()));
        assertThat(excerpt.type()).isEqualTo(ModelType.of("stream"));
        assertThat(excerpt.title()).isEqualTo(stream.getTitle());
    }

}