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
package org.graylog.security.entities;

import com.google.common.collect.ImmutableSet;
import org.graylog.grn.GRN;
import org.graylog.grn.GRNDescriptor;
import org.graylog.grn.GRNDescriptorService;
import org.graylog.grn.GRNRegistry;
import org.graylog.security.DBGrantService;
import org.graylog.testing.GRNExtension;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog.testing.mongodb.MongoJackExtension;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.contentpacks.ContentPackService;
import org.graylog2.contentpacks.model.ModelId;
import org.graylog2.contentpacks.model.ModelTypes;
import org.graylog2.contentpacks.model.entities.EntityDescriptor;
import org.graylog2.contentpacks.model.entities.EntityExcerpt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(MongoJackExtension.class)
@ExtendWith(GRNExtension.class)
@ExtendWith(MockitoExtension.class)
@MongoDBFixtures("EntityDependencyResolverTest.json")
class EntityDependencyResolverTest {

    private EntityDependencyResolver entityDependencyResolver;
    private GRNRegistry grnRegistry;
    private ContentPackService contentPackService;
    private GRNDescriptorService grnDescriptorService;

    @BeforeEach
    void setUp(@Mock ContentPackService contentPackService,
               GRNRegistry grnRegistry,
               @Mock GRNDescriptorService grnDescriptorService,
               MongoDBTestService mongodb,
               MongoJackObjectMapperProvider objectMapperProvider) {

        this.grnRegistry = grnRegistry;
        DBGrantService dbGrantService = new DBGrantService(mongodb.mongoConnection(), objectMapperProvider, this.grnRegistry);
        this.contentPackService = contentPackService;
        this.grnDescriptorService = grnDescriptorService;
        entityDependencyResolver = new EntityDependencyResolver(contentPackService, grnRegistry, grnDescriptorService, dbGrantService);
    }

    @Test
    @DisplayName("Try a regular depency resolve")
    void resolve() {
        final String TEST_TITLE = "Test Stream Title";
        final EntityExcerpt streamExcerpt = EntityExcerpt.builder()
                .type(ModelTypes.STREAM_V1)
                .id(ModelId.of("54e3deadbeefdeadbeefaffe"))
                .title(TEST_TITLE).build();
        when(contentPackService.listAllEntityExcerpts()).thenReturn(ImmutableSet.of(streamExcerpt));

        final EntityDescriptor streamDescriptor = EntityDescriptor.builder().type(ModelTypes.STREAM_V1).id(ModelId.of("54e3deadbeefdeadbeefaffe")).build();
        when(contentPackService.resolveEntities(any())).thenReturn(ImmutableSet.of(streamDescriptor));

        when(grnDescriptorService.getDescriptor(any(GRN.class))).thenAnswer(a -> {
            GRN grnArg = a.getArgument(0);
            return GRNDescriptor.builder().grn(grnArg).title("dummy").build();
        });
        final GRN dashboard = grnRegistry.newGRN("dashboard", "33e3deadbeefdeadbeefaffe");

        final ImmutableSet<org.graylog.security.entities.EntityDescriptor> missingDependencies = entityDependencyResolver.resolve(dashboard);
        assertThat(missingDependencies).hasSize(1);
        assertThat(missingDependencies.asList().get(0)).satisfies(descriptor -> {
            assertThat(descriptor.id().toString()).isEqualTo("grn::::stream:54e3deadbeefdeadbeefaffe");
            assertThat(descriptor.title()).isEqualTo(TEST_TITLE);

            assertThat(descriptor.owners()).hasSize(1);
            assertThat(descriptor.owners().asList().get(0).id().toString()).isEqualTo("grn::::user:jane");
        });
    }

    @Test
    @DisplayName("Try resolve with a broken dependency")
    void resolveWithInclompleteDependency() {

        when(contentPackService.listAllEntityExcerpts()).thenReturn(ImmutableSet.of());
        final EntityDescriptor streamDescriptor = EntityDescriptor.builder().type(ModelTypes.STREAM_V1).id(ModelId.of("54e3deadbeefdeadbeefaffe")).build();
        when(contentPackService.resolveEntities(any())).thenReturn(ImmutableSet.of(streamDescriptor));

        when(grnDescriptorService.getDescriptor(any(GRN.class))).thenAnswer(a -> {
            GRN grnArg = a.getArgument(0);
            return GRNDescriptor.builder().grn(grnArg).title("dummy").build();
        });
        final GRN dashboard = grnRegistry.newGRN("dashboard", "33e3deadbeefdeadbeefaffe");

        final ImmutableSet<org.graylog.security.entities.EntityDescriptor> missingDependencies = entityDependencyResolver.resolve(dashboard);
        assertThat(missingDependencies).hasSize(1);
        assertThat(missingDependencies.asList().get(0)).satisfies(descriptor -> {
            assertThat(descriptor.id().toString()).isEqualTo("grn::::stream:54e3deadbeefdeadbeefaffe");
            assertThat(descriptor.title()).isEqualTo("unknown dependency: <grn::::stream:54e3deadbeefdeadbeefaffe>");
        });
    }
}
