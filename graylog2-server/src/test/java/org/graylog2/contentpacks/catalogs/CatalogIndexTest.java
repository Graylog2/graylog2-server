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
package org.graylog2.contentpacks.catalogs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.graylog2.contentpacks.codecs.OutputCodec;
import org.graylog2.contentpacks.codecs.StreamCodec;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.database.NotFoundException;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.plugin.streams.Output;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.streams.OutputImpl;
import org.graylog2.streams.OutputService;
import org.graylog2.streams.StreamImpl;
import org.graylog2.streams.StreamMock;
import org.graylog2.streams.StreamRuleService;
import org.graylog2.streams.StreamService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class CatalogIndexTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    @Mock
    private StreamService streamService;
    @Mock
    private StreamRuleService streamRuleService;
    @Mock
    private IndexSetService indexSetService;
    @Mock
    private OutputService outputService;

    private CatalogIndex catalogIndex;

    @Before
    public void setUp() throws Exception {
        final Map<ModelType, EntityCatalog> catalogs = ImmutableMap.of(
                ModelTypes.STREAM, new StreamCatalog(streamService, new StreamCodec(objectMapper, streamService, streamRuleService, indexSetService)),
                ModelTypes.OUTPUT, new OutputCatalog(outputService, new OutputCodec(objectMapper, outputService))
        );

        catalogIndex = new CatalogIndex(catalogs);
    }

    @Test
    public void resolveEntitiesWithEmptyInput() {
        final Set<EntityDescriptor> resolvedEntities = catalogIndex.resolveEntities(Collections.emptySet());
        assertThat(resolvedEntities).isEmpty();
    }

    @Test
    public void resolveEntitiesWithNoDependencies() throws NotFoundException {
        final StreamMock streamMock = new StreamMock(ImmutableMap.of(
                "_id", "stream-1234",
                StreamImpl.FIELD_TITLE, "Stream Title"
        ));

        when(streamService.load("stream-1234")).thenReturn(streamMock);

        final ImmutableSet<EntityDescriptor> unresolvedEntities = ImmutableSet.of(
                EntityDescriptor.create(ModelId.of("stream-1234"), ModelTypes.STREAM)
        );

        final Set<EntityDescriptor> resolvedEntities = catalogIndex.resolveEntities(unresolvedEntities);
        assertThat(resolvedEntities).containsOnly(EntityDescriptor.create(ModelId.of("stream-1234"), ModelTypes.STREAM));
    }

    @Test
    public void resolveEntitiesWithTransitiveDependencies() throws NotFoundException {
        final StreamMock streamMock = new StreamMock(ImmutableMap.of(
                "_id", "stream-1234",
                StreamImpl.FIELD_TITLE, "Stream Title")) {
            @Override
            public Set<Output> getOutputs() {
                return Collections.singleton(
                        OutputImpl.create(
                                "output-1234",
                                "Output Title",
                                "org.example.outputs.SomeOutput",
                                "admin",
                                Collections.emptyMap(),
                                new Date(0L),
                                null
                        )
                );
            }
        };

        when(streamService.load("stream-1234")).thenReturn(streamMock);

        final ImmutableSet<EntityDescriptor> unresolvedEntities = ImmutableSet.of(
                EntityDescriptor.create(ModelId.of("stream-1234"), ModelTypes.STREAM)
        );

        final Set<EntityDescriptor> resolvedEntities = catalogIndex.resolveEntities(unresolvedEntities);
        assertThat(resolvedEntities).containsOnly(
                EntityDescriptor.create(ModelId.of("stream-1234"), ModelTypes.STREAM),
                EntityDescriptor.create(ModelId.of("output-1234"), ModelTypes.OUTPUT)
        );
    }
}