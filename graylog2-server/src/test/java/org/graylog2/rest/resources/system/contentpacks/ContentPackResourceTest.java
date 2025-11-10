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

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import org.graylog2.contentpacks.ContentPackInstallationPersistenceService;
import org.graylog2.contentpacks.ContentPackPersistenceService;
import org.graylog2.contentpacks.ContentPackService;
import org.graylog2.contentpacks.model.ContentPack;
import org.graylog2.contentpacks.model.ContentPackInstallation;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.constraints.ConstraintCheckResult;
import org.graylog2.jackson.AutoValueSubtypeResolver;
import org.graylog2.rest.models.system.contentpacks.responses.ContentPackList;
import org.graylog2.rest.models.system.contentpacks.responses.ContentPackMetadata;
import org.graylog2.rest.models.system.contentpacks.responses.ContentPackResponse;
import org.graylog2.rest.models.system.contentpacks.responses.ContentPackRevisions;
import org.graylog2.security.WithAuthorization;
import org.graylog2.security.WithAuthorizationExtension;
import org.graylog2.shared.bindings.GuiceInjectorHolder;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.shared.security.RestPermissions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ExtendWith(WithAuthorizationExtension.class)
public class ContentPackResourceTest {

    private static final String CONTENT_PACK = "" +
            "{\n" +
            "    \"v\": \"1\",\n" +
            "    \"id\": \"78547c87-af21-4392-FAAT-614da5baf6c3\",\n" +
            "    \"rev\": 1,\n" +
            "    \"name\": \"The most smallest content pack\",\n" +
            "    \"summary\": \"This is the smallest and most useless content pack\",\n" +
            "    \"description\": \"### We do not saw!\\n But we might kill!\",\n" +
            "    \"vendor\": \"Graylog, Inc. <egwene@graylog.com>\",\n" +
            "    \"url\": \"https://github.com/graylog-labs/small-content-pack.git\",\n" +
            "    \"parameters\": [ ],\n" +
            "    \"entities\": [ ]\n" +
            "}";

    @Mock
    private ContentPackService contentPackService;
    @Mock
    private ContentPackPersistenceService contentPackPersistenceService;
    @Mock
    private ContentPackInstallationPersistenceService contentPackInstallationPersistenceService;

    @Mock
    private Set<ContentPackInstallation> contentPackInstallations;

    private PermittedTestResource contentPackResource;
    private ObjectMapper objectMapper;

    public ContentPackResourceTest() {
        GuiceInjectorHolder.createInjector(Collections.emptyList());
    }

    @BeforeEach
    public void setUp() {
        objectMapper = new ObjectMapperProvider().get();
        objectMapper.setSubtypeResolver(new AutoValueSubtypeResolver());
        contentPackResource = new PermittedTestResource(
                contentPackService,
                contentPackPersistenceService,
                contentPackInstallationPersistenceService);
    }

    @Test
    @WithAuthorization(permissions = {})
    public void testListContentPacksRequiresPermissionFail() {
        assertThatThrownBy(() -> contentPackResource.listContentPacks())
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @WithAuthorization(permissions = RestPermissions.CONTENT_PACK_CREATE)
    public void uploadContentPack() throws Exception {
        final ContentPack contentPack = objectMapper.readValue(CONTENT_PACK, ContentPack.class);
        when(contentPackPersistenceService.filterMissingResourcesAndInsert(contentPack)).thenReturn(Optional.ofNullable(contentPack));

        final Response response = contentPackResource.createContentPack(contentPack);

        verify(contentPackPersistenceService, times(1)).filterMissingResourcesAndInsert(contentPack);
        assertThat(response.getStatusInfo()).isEqualTo(Response.Status.CREATED);
    }

    @Test
    @WithAuthorization(permissions = RestPermissions.CONTENT_PACK_READ)
    public void listAndLatest() throws Exception {
        final ContentPack contentPack = objectMapper.readValue(CONTENT_PACK, ContentPack.class);
        final Set<ContentPack> contentPacks = Collections.singleton(contentPack);
        final Map<ModelId, Map<Integer, ContentPackMetadata>> metaDataMap = Collections.emptyMap();
        final ContentPackList expectedList = ContentPackList.create(contentPacks.size(), contentPacks, metaDataMap);

        when(contentPackPersistenceService.loadAll()).thenReturn(Collections.singleton(contentPack));
        final ContentPackList contentPackList = contentPackResource.listContentPacks();
        verify(contentPackPersistenceService, times(1)).loadAll();
        assertThat(contentPackList).isEqualTo(expectedList);

        when(contentPackPersistenceService.loadAllLatest()).thenReturn(Collections.singleton(contentPack));
        final ContentPackList contentPackLatest = contentPackResource.listLatestContentPacks();
        verify(contentPackPersistenceService, times(1)).loadAll();
        assertThat(contentPackLatest).isEqualTo(expectedList);
    }

    @Test
    @WithAuthorization(permissions = RestPermissions.CONTENT_PACK_READ)
    public void getContentPack() throws Exception {
        final ContentPack contentPack = objectMapper.readValue(CONTENT_PACK, ContentPack.class);
        final Set<ContentPack> contentPackSet = Collections.singleton(contentPack);
        final Set<ConstraintCheckResult> constraints = Collections.emptySet();

        final Map<Integer, ContentPack> contentPacks = Collections.singletonMap(1, contentPack);
        final Map<Integer, Set<ConstraintCheckResult>> constraintMap = Collections.singletonMap(1, constraints);
        final ContentPackRevisions expectedRevisions = ContentPackRevisions.create(contentPacks, constraintMap);
        final ModelId id = ModelId.of("1");

        when(contentPackPersistenceService.findAllById(id)).thenReturn(contentPackSet);
        final ContentPackRevisions contentPackRevisions = contentPackResource.listContentPackRevisions(id);
        verify(contentPackPersistenceService, times(1)).findAllById(id);
        assertThat(contentPackRevisions).isEqualTo(expectedRevisions);

        when(contentPackPersistenceService.findByIdAndRevision(id, 1)).thenReturn(Optional.ofNullable(contentPack));
        final ContentPackResponse contentPackResponse = contentPackResource.getContentPackRevisions(id, 1);
        verify(contentPackPersistenceService, times(1)).findByIdAndRevision(id, 1);
        assertThat(contentPackResponse.contentPack()).isEqualTo(contentPack);
    }

    @Test
    @WithAuthorization(permissions = RestPermissions.CONTENT_PACK_DELETE)
    public void deleteContentPack() throws Exception {
        final ModelId id = ModelId.of("1");
        when(contentPackPersistenceService.deleteById(id)).thenReturn(1);
        contentPackResource.deleteContentPack(id);
        verify(contentPackPersistenceService, times(1)).deleteById(id);

        when(contentPackPersistenceService.deleteByIdAndRevision(id, 1)).thenReturn(1);
        contentPackResource.deleteContentPack(id, 1);
        verify(contentPackPersistenceService, times(1)).deleteByIdAndRevision(id, 1);
    }

    @Test
    @WithAuthorization(permissions = RestPermissions.CONTENT_PACK_DELETE)
    public void notDeleteContentPack() throws Exception {
        final ModelId id = ModelId.of("1");
        when(contentPackInstallationPersistenceService.findByContentPackId(id)).thenReturn(contentPackInstallations);
        boolean exceptionCalled = false;
        try {
            contentPackResource.deleteContentPack(id);
        } catch (BadRequestException e) {
            exceptionCalled = true;
        }
        assertThat(exceptionCalled).isEqualTo(true);
        verify(contentPackInstallationPersistenceService, times(1)).findByContentPackId(id);
        verify(contentPackPersistenceService, times(0)).deleteById(id);

        when(contentPackInstallationPersistenceService.findByContentPackIdAndRevision(id, 1)).thenReturn(contentPackInstallations);
        exceptionCalled = false;
        try {
            contentPackResource.deleteContentPack(id, 1);
        } catch (BadRequestException e) {
            exceptionCalled = true;
        }
        assertThat(exceptionCalled).isEqualTo(true);
        verify(contentPackInstallationPersistenceService, times(1)).findByContentPackIdAndRevision(id, 1);
        verify(contentPackPersistenceService, times(0)).deleteByIdAndRevision(id, 1);
    }

    static class PermittedTestResource extends ContentPackResource {
        PermittedTestResource(ContentPackService contentPackService,
                              ContentPackPersistenceService contentPackPersistenceService,
                              ContentPackInstallationPersistenceService contentPackInstallationPersistenceService) {
            super(contentPackService, contentPackPersistenceService, contentPackInstallationPersistenceService);
        }

        @Override
        protected UriBuilder getUriBuilderToSelf() {
            return UriBuilder.fromUri("http://testserver/api");
        }
    }
}
