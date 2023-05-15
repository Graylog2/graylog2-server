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
import org.graylog.plugins.views.search.rest.TestSearchUser;
import org.graylog2.contentpacks.ContentPackInstallationPersistenceService;
import org.graylog2.contentpacks.ContentPackService;
import org.graylog2.contentpacks.EntityDescriptorIds;
import org.graylog2.contentpacks.constraints.ConstraintChecker;
import org.graylog2.contentpacks.facades.EntityFacade;
import org.graylog2.contentpacks.facades.EntityWithExcerptFacade;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.graylog2.contentpacks.model.entities.EntityV1;
import org.graylog2.rest.models.system.contentpacks.responses.CatalogIndexResponse;
import org.graylog2.rest.models.system.contentpacks.responses.CatalogResolveRequest;
import org.graylog2.rest.models.system.contentpacks.responses.CatalogResolveResponse;
import org.graylog2.rest.resources.system.contentpacks.titles.model.EntitiesTitleResponse;
import org.graylog2.rest.resources.system.contentpacks.titles.model.EntityIdentifier;
import org.graylog2.rest.resources.system.contentpacks.titles.model.EntityTitleRequest;
import org.graylog2.rest.resources.system.contentpacks.titles.model.EntityTitleResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CatalogResourceTest {

    private EntityFacade<Void> mockEntityFacade = Mockito.mock(EntityFacade.class);

    private ContentPackService contentPackService;

    @Before
    public void setUp() {
        final ContentPackInstallationPersistenceService contentPackInstallationPersistenceService =
                mock(ContentPackInstallationPersistenceService.class);
        final Set<ConstraintChecker> constraintCheckers = Collections.emptySet();
        final Map<ModelType, EntityWithExcerptFacade<?, ?>> entityFacades = Collections.singletonMap(ModelType.of("test", "1"), mockEntityFacade);
        contentPackService = new ContentPackService(contentPackInstallationPersistenceService, constraintCheckers, entityFacades);
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

        final CatalogResource resource = new CatalogResource(contentPackService, (r, p) -> EntitiesTitleResponse.EMPTY_RESPONSE);

        final CatalogIndexResponse catalogIndexResponse = resource.showEntityIndex();

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

        final CatalogResource resource = new CatalogResource(contentPackService, (r, p) -> EntitiesTitleResponse.EMPTY_RESPONSE);

        final CatalogResolveResponse catalogResolveResponse = resource.resolveEntities(request);

        assertThat(catalogResolveResponse.entities()).containsOnly(entity);
    }

    @Test
    public void getTitles() {
        final EntityTitleRequest request = new EntityTitleRequest(List.of(new EntityIdentifier("id", "x")));
        final EntitiesTitleResponse expectedResponse = new EntitiesTitleResponse(
                Set.of(new EntityTitleResponse("id", "x", "Title")),
                Set.of()
        );

        final CatalogResource resource = new CatalogResource(contentPackService, (r, p) -> expectedResponse);

        final EntitiesTitleResponse actualResponse = resource.getTitles(request, TestSearchUser.builder().build());
        assertEquals(expectedResponse, actualResponse);
    }
}
