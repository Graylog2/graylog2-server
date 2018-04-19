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
import com.google.common.collect.ImmutableMap;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.entities.Entity;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.OutputEntity;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.streams.OutputImpl;
import org.graylog2.streams.OutputService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

public class OutputCodecTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    @Mock
    private OutputService outputService;
    private OutputCodec codec;

    @Before
    public void setUp() {
        codec = new OutputCodec(objectMapper, outputService);
    }

    @Test
    public void encode() {
        final ImmutableMap<String, Object> configuration = ImmutableMap.of(
                "some-setting", "foobar"
        );
        final OutputImpl output = OutputImpl.create(
                "01234567890",
                "Output Title",
                "org.graylog2.output.SomeOutputClass",
                "admin",
                configuration,
                new Date(0L),
                null
        );
        final Entity entity = codec.encode(output);

        assertThat(entity).isInstanceOf(EntityV1.class);
        assertThat(entity.id()).isEqualTo(ModelId.of("01234567890"));
        assertThat(entity.type()).isEqualTo(ModelType.of("output"));

        final EntityV1 entityV1 = (EntityV1) entity;
        final OutputEntity outputEntity = objectMapper.convertValue(entityV1.data(), OutputEntity.class);
        assertThat(outputEntity.title()).isEqualTo("Output Title");
        assertThat(outputEntity.type()).isEqualTo("org.graylog2.output.SomeOutputClass");
        assertThat(outputEntity.configuration()).containsEntry("some-setting", "foobar");
    }

    @Test
    public void createExcerpt() {
        final ImmutableMap<String, Object> configuration = ImmutableMap.of();
        final OutputImpl output = OutputImpl.create(
                "01234567890",
                "Output Title",
                "org.graylog2.output.SomeOutputClass",
                "admin",
                configuration,
                new Date(0L),
                null
        );
        final EntityExcerpt excerpt = codec.createExcerpt(output);

        assertThat(excerpt.id()).isEqualTo(ModelId.of(output.getId()));
        assertThat(excerpt.type()).isEqualTo(ModelType.of("output"));
        assertThat(excerpt.title()).isEqualTo(output.getTitle());
    }
}