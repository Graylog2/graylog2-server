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
package org.graylog2.rest.resources.system.contentpacks;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import org.graylog2.contentpacks.catalogs.CatalogIndex;
import org.graylog2.contentpacks.facades.EntityFacade;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.contentpacks.model.entities.EntityWithConstraints;
import org.graylog2.rest.models.system.contenpacks.responses.CatalogIndexResponse;
import org.graylog2.rest.models.system.contenpacks.responses.CatalogResolveRequest;
import org.graylog2.rest.models.system.contenpacks.responses.CatalogResolveResponse;
import org.graylog2.shared.bindings.GuiceInjectorHolder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class CatalogResourceTest {
    static {
        GuiceInjectorHolder.createInjector(Collections.emptyList());
    }

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private EntityFacade<Void> mockEntityCatalog;

    private CatalogIndex catalogIndex;
    private CatalogResource catalogResource;

    @Before
    public void setUp() {
        final ImmutableMap<ModelType, EntityFacade<?>> entityCatalogs = ImmutableMap.of(ModelType.of("test"), mockEntityCatalog);
        catalogIndex = new CatalogIndex(entityCatalogs);
        catalogResource = new CatalogResource(catalogIndex);
    }

    @Test
    public void showEntityIndex() {
        final ImmutableSet<EntityExcerpt> entityExcerpts = ImmutableSet.of(
                EntityExcerpt.builder()
                        .id(ModelId.of("1234567890"))
                        .type(ModelType.of("test"))
                        .title("Test Entity")
                        .build()
        );
        when(mockEntityCatalog.listEntityExcerpts()).thenReturn(entityExcerpts);
        final CatalogIndexResponse catalogIndexResponse = catalogResource.showEntityIndex();

        assertThat(catalogIndexResponse.entities())
                .hasSize(1)
                .containsAll(entityExcerpts);
    }

    @Test
    public void resolveEntities() {
        final EntityDescriptor entityDescriptor = EntityDescriptor.builder()
                .id(ModelId.of("1234567890"))
                .type(ModelType.of("test"))
                .build();
        final MutableGraph<EntityDescriptor> entityDescriptors = GraphBuilder.directed().build();
        entityDescriptors.addNode(entityDescriptor);

        final EntityV1 entity = EntityV1.builder()
                .id(ModelId.of("1234567890"))
                .type(ModelType.of("test"))
                .data(new ObjectNode(JsonNodeFactory.instance).put("test", "1234"))
                .build();
        final EntityWithConstraints entityWithConstraints = EntityWithConstraints.create(entity);
        when(mockEntityCatalog.resolve(entityDescriptor)).thenReturn(entityDescriptors);
        when(mockEntityCatalog.collectEntity(entityDescriptor)).thenReturn(Optional.of(entityWithConstraints));

        final CatalogResolveRequest request = CatalogResolveRequest.create(entityDescriptors.nodes());

        final CatalogResolveResponse catalogResolveResponse = catalogResource.resolveEntities(request);

        assertThat(catalogResolveResponse.entities()).containsOnly(entity);
    }
}