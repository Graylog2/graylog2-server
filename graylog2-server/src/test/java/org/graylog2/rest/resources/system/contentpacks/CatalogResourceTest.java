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
package org.graylog2.rest.resources.system.contentpacks;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import org.graylog2.contentpacks.ContentPackInstallationPersistenceService;
import org.graylog2.contentpacks.ContentPackService;
import org.graylog2.contentpacks.EntityDescriptorIds;
import org.graylog2.contentpacks.constraints.ConstraintChecker;
import org.graylog2.contentpacks.facades.EntityFacade;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.rest.models.system.contentpacks.responses.CatalogIndexResponse;
import org.graylog2.rest.models.system.contentpacks.responses.CatalogResolveRequest;
import org.graylog2.rest.models.system.contentpacks.responses.CatalogResolveResponse;
import org.graylog2.shared.bindings.GuiceInjectorHolder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CatalogResourceTest {
    static {
        GuiceInjectorHolder.createInjector(Collections.emptyList());
    }

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private EntityFacade<Void> mockEntityFacade;

    private ContentPackService contentPackService;
    private CatalogResource catalogResource;

    @Before
    public void setUp() {
        final ContentPackInstallationPersistenceService contentPackInstallationPersistenceService =
                mock(ContentPackInstallationPersistenceService.class);
        final Set<ConstraintChecker> constraintCheckers = Collections.emptySet();
        final Map<ModelType, EntityFacade<?>> entityFacades = Collections.singletonMap(ModelType.of("test", "1"), mockEntityFacade);
        contentPackService = new ContentPackService(contentPackInstallationPersistenceService, constraintCheckers, entityFacades);
        catalogResource = new CatalogResource(contentPackService);
    }

    @Test
    public void showEntityIndex() {
        final ImmutableSet<EntityExcerpt> entityExcerpts = ImmutableSet.of(
                EntityExcerpt.builder()
                        .id(ModelId.of("1234567890"))
                        .type(ModelType.of("test", "1"))
                        .title("Test Entity")
                        .build()
        );
        when(mockEntityFacade.listEntityExcerpts()).thenReturn(entityExcerpts);
        final CatalogIndexResponse catalogIndexResponse = catalogResource.showEntityIndex();

        assertThat(catalogIndexResponse.entities())
                .hasSize(1)
                .containsAll(entityExcerpts);
    }

    @Test
    public void resolveEntities() {
        final EntityDescriptor entityDescriptor = EntityDescriptor.builder()
                .id(ModelId.of("1234567890"))
                .type(ModelType.of("test", "1"))
                .build();
        final MutableGraph<EntityDescriptor> entityDescriptors = GraphBuilder.directed().build();
        entityDescriptors.addNode(entityDescriptor);

        final EntityV1 entity = EntityV1.builder()
                .id(ModelId.of("1234567890"))
                .type(ModelType.of("test", "1"))
                .data(new ObjectNode(JsonNodeFactory.instance).put("test", "1234"))
                .build();
        when(mockEntityFacade.resolveNativeEntity(entityDescriptor)).thenReturn(entityDescriptors);
        when(mockEntityFacade.exportEntity(eq(entityDescriptor), any(EntityDescriptorIds.class))).thenReturn(Optional.of(entity));

        final CatalogResolveRequest request = CatalogResolveRequest.create(entityDescriptors.nodes());

        final CatalogResolveResponse catalogResolveResponse = catalogResource.resolveEntities(request);

        assertThat(catalogResolveResponse.entities()).containsOnly(entity);
    }
}
