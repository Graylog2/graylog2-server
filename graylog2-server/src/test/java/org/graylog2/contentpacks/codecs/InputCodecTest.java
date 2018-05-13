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
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.EntityWithConstraints;
import org.graylog2.contentpacks.model.entities.InputEntity;
import org.graylog2.contentpacks.model.entities.references.ValueReference;
import org.graylog2.dashboards.DashboardImpl;
import org.graylog2.inputs.InputImpl;
import org.graylog2.inputs.InputService;
import org.graylog2.inputs.converters.ConverterFactory;
import org.graylog2.inputs.extractors.ExtractorFactory;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.inputs.Extractor;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.shared.inputs.InputRegistry;
import org.graylog2.shared.inputs.MessageInputFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class InputCodecTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    @Mock
    private InputService inputService;
    @Mock
    private InputRegistry inputRegistry;
    @Mock
    private MessageInputFactory messageInputFactory;
    @Mock
    private ExtractorFactory extractorFactory;
    @Mock
    private ConverterFactory converterFactory;
    @Mock
    private ServerStatus serverStatus;

    private InputCodec codec;

    @Before
    public void setUp() {
        codec = new InputCodec(objectMapper, inputService, inputRegistry, messageInputFactory, extractorFactory, converterFactory, serverStatus);
    }

    @Test
    public void encode() {
        final ImmutableMap<String, Object> fields = ImmutableMap.of(
                MessageInput.FIELD_TITLE, "Input Title",
                MessageInput.FIELD_TYPE, "org.graylog2.inputs.SomeInput",
                MessageInput.FIELD_CONFIGURATION, Collections.emptyMap()
        );
        final InputImpl input = new InputImpl(fields);
        final ImmutableList<Extractor> extractors = ImmutableList.of();
        final InputWithExtractors inputWithExtractors = InputWithExtractors.create(input, extractors);
        final EntityWithConstraints entityWithConstraints = codec.encode(inputWithExtractors);
        final Entity entity = entityWithConstraints.entity();

        assertThat(entity).isInstanceOf(EntityV1.class);
        assertThat(entity.id()).isEqualTo(ModelId.of(input.getId()));
        assertThat(entity.type()).isEqualTo(ModelType.of("input"));

        final EntityV1 entityV1 = (EntityV1) entity;
        final InputEntity inputEntity = objectMapper.convertValue(entityV1.data(), InputEntity.class);
        assertThat(inputEntity.title()).isEqualTo(ValueReference.of("Input Title"));
        assertThat(inputEntity.type()).isEqualTo(ValueReference.of("org.graylog2.inputs.SomeInput"));
        assertThat(inputEntity.configuration()).isEmpty();
    }

    @Test
    public void createExcerpt() {
        final ImmutableMap<String, Object> fields = ImmutableMap.of(
                DashboardImpl.FIELD_TITLE, "Dashboard Title"
        );
        final InputImpl input = new InputImpl(fields);
        final InputWithExtractors inputWithExtractors = InputWithExtractors.create(input);
        final EntityExcerpt excerpt = codec.createExcerpt(inputWithExtractors);

        assertThat(excerpt.id()).isEqualTo(ModelId.of(input.getId()));
        assertThat(excerpt.type()).isEqualTo(ModelType.of("input"));
        assertThat(excerpt.title()).isEqualTo(input.getTitle());
    }
}