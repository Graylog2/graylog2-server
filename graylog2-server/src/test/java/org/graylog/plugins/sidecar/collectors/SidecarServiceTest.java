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
package org.graylog.plugins.sidecar.collectors;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.graylog.plugins.sidecar.rest.models.Collector;
import org.graylog.plugins.sidecar.rest.models.Configuration;
import org.graylog.plugins.sidecar.rest.models.NodeDetails;
import org.graylog.plugins.sidecar.rest.models.Sidecar;
import org.graylog.plugins.sidecar.rest.requests.ConfigurationAssignment;
import org.graylog.plugins.sidecar.services.CollectorService;
import org.graylog.plugins.sidecar.services.ConfigurationService;
import org.graylog.plugins.sidecar.services.SidecarService;
import org.graylog.testing.inject.TestPasswordSecretModule;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.NotFoundException;
import org.graylog2.shared.bindings.ObjectMapperModule;
import org.graylog2.shared.bindings.ValidatorModule;
import org.jukito.JukitoRunner;
import org.jukito.UseModules;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.validation.Validator;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(JukitoRunner.class)
@UseModules({ObjectMapperModule.class, ValidatorModule.class, TestPasswordSecretModule.class})
public class SidecarServiceTest {
    private static final String collectionName = "sidecars";

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();
    @Mock
    private CollectorService collectorService;

    @Mock private ConfigurationService configurationService;

    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    private SidecarService sidecarService;

    @Before
    public void setUp(MongoJackObjectMapperProvider mapperProvider,
                      Validator validator) throws Exception {
        this.sidecarService = new SidecarService(collectorService, configurationService,  mongodb.mongoConnection(), mapperProvider, validator);
    }

    @Test
    public void testCountEmptyCollection() throws Exception {
        final long result = this.sidecarService.count();

        assertEquals(0, result);
    }

    @Test
    @MongoDBFixtures("collectorsMultipleDocuments.json")
    public void testCountNonEmptyCollection() throws Exception {
        final long result = this.sidecarService.count();

        assertEquals(3, result);
    }

    @Test
    public void testSaveFirstRecord() throws Exception {
        String nodeId = "nodeId";
        String nodeName = "nodeName";
        String version = "0.0.1";
        String os = "DummyOS 1.0";
        final Sidecar sidecar = Sidecar.create(
                nodeId,
                nodeName,
                NodeDetails.create(
                        os,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null),
                version
                );

        final Sidecar result = this.sidecarService.save(sidecar);
        MongoCollection<Document> collection = mongodb.mongoConnection().getMongoDatabase().getCollection(collectionName);
        Document document = collection.find().first();
        Document nodeDetails = document.get("node_details", Document.class);

        assertNotNull(result);
        assertEquals(nodeId, document.get("node_id"));
        assertEquals(nodeName, document.get("node_name"));
        assertEquals(version, document.get("sidecar_version"));
        assertEquals(os, nodeDetails.get("operating_system"));
    }

    @Test
    @MongoDBFixtures("collectorsMultipleDocuments.json")
    public void testAll() throws Exception {
        final List<Sidecar> sidecars = this.sidecarService.all();

        assertNotNull(sidecars);
        assertEquals(3, sidecars.size());
    }

    @Test
    public void testAllEmptyCollection() throws Exception {
        final List<Sidecar> sidecars = this.sidecarService.all();

        assertNotNull(sidecars);
        assertEquals(0, sidecars.size());
    }

    @Test
    @MongoDBFixtures("collectorsMultipleDocuments.json")
    public void testFindById() throws Exception {
        final String collector1id = "uniqueid1";

        final Sidecar sidecar = this.sidecarService.findByNodeId(collector1id);

        assertNotNull(sidecar);
        assertEquals(collector1id, sidecar.nodeId());
    }

    @Test
    @MongoDBFixtures("collectorsMultipleDocuments.json")
    public void testFindByIdNonexisting() throws Exception {
        final String collector1id = "nonexisting";

        final Sidecar sidecar = this.sidecarService.findByNodeId(collector1id);

        assertNull(sidecar);
    }

    @Test
    @MongoDBFixtures("collectorsMultipleDocuments.json")
    public void testDestroy() throws Exception {
        final Sidecar sidecar = mock(Sidecar.class);
        when(sidecar.id()).thenReturn("581b3bff8e4dc4270055dfcb");

        final int result = this.sidecarService.delete(sidecar.id());
        assertEquals(1, result);
        assertEquals(2, mongodb.mongoConnection().getMongoDatabase().getCollection(collectionName).countDocuments());
    }

    @Test
    public void simpleTagAssignment() {
        final Configuration configuration = getConfiguration();
        when(configurationService.findByTags(anySet())).thenReturn(List.of(configuration));
        final Collector collector = getCollector();

        when(collectorService.all()).thenReturn(List.of(collector));
        Sidecar sidecar = getTestSidecar();
        sidecar = sidecar.toBuilder().nodeDetails(getNodeDetails("linux", Set.of("tag1"))).build();

        sidecar = sidecarService.updateTaggedConfigurationAssignments(sidecar);

        assertThat(sidecar.assignments()).hasSize(1);
        assertThat(sidecar.assignments().get(0).assignedFromTags()).isEqualTo(Set.of("tag1"));
    }

    @Test
    public void mergeWithManualAssignments() {
        final Configuration configuration = getConfiguration();
        when(configurationService.findByTags(anySet())).thenReturn(List.of(configuration));
        final Collector collector = getCollector();

        when(collectorService.all()).thenReturn(List.of(collector));
        Sidecar sidecar = getTestSidecar();
        sidecar = sidecar.toBuilder().nodeDetails(getNodeDetails("linux", Set.of("tag1")))
                .assignments(List.of(ConfigurationAssignment.create("some collector", "some config", null)))
                .build();

        sidecar = sidecarService.updateTaggedConfigurationAssignments(sidecar);

        assertThat(sidecar.assignments()).hasSize(2);
        assertThat(sidecar.assignments()).satisfies(assignments -> {
            assertThat(assignments.stream().filter(a -> a.assignedFromTags().equals(Set.of())).findAny()).isPresent();
            assertThat(assignments.stream().filter(a -> a.assignedFromTags().equals(Set.of("tag1"))).findAny()).isPresent();
        });
    }

    @Test
    public void updateExistingAssignment() {
        final Configuration configuration = getConfiguration();
        when(configurationService.findByTags(anySet())).thenReturn(List.of(configuration));
        final Collector collector = getCollector();

        when(collectorService.all()).thenReturn(List.of(collector));
        Sidecar sidecar = getTestSidecar();
        final ConfigurationAssignment existingAssignment = ConfigurationAssignment.create(collector.id(), configuration.id(), Set.of("tag1"));
        sidecar = sidecar.toBuilder().nodeDetails(getNodeDetails("linux", Set.of("tag1")))
                .assignments(List.of(existingAssignment))
                .build();

        sidecar = sidecarService.updateTaggedConfigurationAssignments(sidecar);

        assertThat(sidecar.assignments()).hasSize(1);
        assertThat(sidecar.assignments()).first().isEqualTo(existingAssignment);
    }

    @Test
    public void updateExistingAssignments() {
        final Configuration configuration = getConfiguration();
        when(configurationService.findByTags(anySet())).thenReturn(List.of(configuration));
        final Collector collector = getCollector();

        when(collectorService.all()).thenReturn(List.of(collector));
        Sidecar sidecar = getTestSidecar();
        final ConfigurationAssignment existingTag3Assignment = ConfigurationAssignment.create(collector.id(), configuration.id(), Set.of("tag3"));
        final ConfigurationAssignment existingManualAssignment = ConfigurationAssignment.create("some-collector", "some-config", null);
        sidecar = sidecar.toBuilder().nodeDetails(getNodeDetails("linux", Set.of("tag1")))
                .assignments(List.of(existingTag3Assignment, existingManualAssignment))
                .build();

        sidecar = sidecarService.updateTaggedConfigurationAssignments(sidecar);

        assertThat(sidecar.assignments()).hasSize(2);
        assertThat(sidecar.assignments()).satisfies(assignments -> {
            assertThat(assignments.stream().filter(a -> a.assignedFromTags().equals(Set.of())).findAny()).isPresent();
            assertThat(assignments.stream().filter(a -> a.assignedFromTags().equals(Set.of("tag3"))).findAny()).isEmpty();
            assertThat(assignments.stream().filter(a -> a.assignedFromTags().equals(Set.of("tag1"))).findAny()).isPresent();
        });
    }

    @Test
    public void ignoresTagsWithWrongOS() {
        final Configuration configuration = getConfiguration();
        when(configurationService.findByTags(anySet())).thenReturn(List.of(configuration));
        final Collector collector = getCollector();

        when(collectorService.all()).thenReturn(List.of(collector));
        Sidecar sidecar = getTestSidecar();
        sidecar = sidecar.toBuilder().nodeDetails(getNodeDetails("windows", Set.of("tag1"))).build();

        sidecar = sidecarService.updateTaggedConfigurationAssignments(sidecar);

        // The tagged config is linux only
        assertThat(sidecar.assignments()).hasSize(0);
    }

    @Test
    public void replacesManualAssignmentsWithTaggedOnes() {
        final Configuration configuration = getConfiguration();
        when(configurationService.findByTags(anySet())).thenReturn(List.of(configuration));
        final Collector collector = getCollector();

        when(collectorService.all()).thenReturn(List.of(collector));
        Sidecar sidecar = getTestSidecar();
        final ConfigurationAssignment manualAssignment = ConfigurationAssignment.create(collector.id(), configuration.id(), null);
        sidecar = sidecar.toBuilder().nodeDetails(getNodeDetails("linux", Set.of("tag1")))
                .assignments(List.of(manualAssignment))
                .build();

        sidecar = sidecarService.updateTaggedConfigurationAssignments(sidecar);

        assertThat(sidecar.assignments()).hasSize(1);
        assertThat(sidecar.assignments()).satisfies(assignments -> {
            assertThat(assignments.stream().filter(a -> a.assignedFromTags().equals(Set.of("tag1"))).findAny()).isPresent();
        });
    }

    @Test
    @MongoDBFixtures("sidecars.json")
    public void applyManualAssignment() throws NotFoundException {
        Sidecar sidecar = sidecarService.findByNodeId("node-id");

        final Configuration configuration = getConfiguration();
        when(configurationService.find(anyString())).thenReturn(configuration);
        final Collector collector = getCollector();
        when(collectorService.find(anyString())).thenReturn(collector);

        assertThat(sidecar.assignments()).isEmpty();
        final ConfigurationAssignment manualAssignment = ConfigurationAssignment.create(collector.id(), configuration.id(), null);
        sidecar = sidecarService.applyManualAssignments(sidecar.nodeId(), List.of(manualAssignment));

        assertThat(sidecar.assignments()).hasSize(1);
    }

    @Test
    @MongoDBFixtures("sidecars.json")
    public void applyManualAssignmentKeepTagged() throws NotFoundException {
        Sidecar sidecar = sidecarService.findByNodeId("node-id");
        final ConfigurationAssignment taggedAssignment = ConfigurationAssignment.create("some-collector", "some-config", Set.of("tag"));
        sidecar = sidecarService.save(sidecar.toBuilder().assignments(List.of(taggedAssignment)).build());

        final Configuration configuration = getConfiguration();
        when(configurationService.find(anyString())).thenReturn(configuration);
        final Collector collector = getCollector();
        when(collectorService.find(anyString())).thenReturn(collector);

        assertThat(sidecar.assignments()).hasSize(1);
        final ConfigurationAssignment manualAssignment = ConfigurationAssignment.create(collector.id(), configuration.id(), null);
        sidecar = sidecarService.applyManualAssignments(sidecar.nodeId(), List.of(manualAssignment));

        assertThat(sidecar.assignments()).hasSize(2);
    }

    @Test
    @MongoDBFixtures("sidecars.json")
    public void ignoreModificationOfTaggedAssignments() throws NotFoundException {
        Sidecar sidecar = sidecarService.findByNodeId("node-id");

        final Configuration configuration = getConfiguration();
        when(configurationService.find(anyString())).thenReturn(configuration);
        final Collector collector = getCollector();
        when(collectorService.find(anyString())).thenReturn(collector);
        final ConfigurationAssignment taggedAssignment = ConfigurationAssignment.create(collector.id(), configuration.id(), Set.of("tag"));
        sidecar = sidecarService.save(sidecar.toBuilder().assignments(List.of(taggedAssignment)).build());


        assertThat(sidecar.assignments()).hasSize(1);
        final ConfigurationAssignment manualAssignment = ConfigurationAssignment.create(collector.id(), configuration.id(), null);
        sidecar = sidecarService.applyManualAssignments(sidecar.nodeId(), List.of(manualAssignment));

        assertThat(sidecar.assignments()).hasSize(1);
        // Tagged assignment is kept intact
        assertThat(sidecar.assignments().get(0).assignedFromTags()).isEqualTo(Set.of("tag"));
    }

    private static Configuration getConfiguration() {
        return Configuration.create("config-id", "collector-id", "config-name", "color", "template", Set.of("tag1"));
    }

    private static Collector getCollector() {
        return Collector.create("collector-id", "collector-name", "service", "linux",
                "/path", "param", "valid param", "");
    }

    private Sidecar getTestSidecar() {
        return Sidecar.create(
                "node-id",
                "node-name",
                getNodeDetails("linux", null),
                "1.3.0"
        );
    }

    private NodeDetails getNodeDetails(String os, Set<String> tags) {
        return NodeDetails.create(
                os,
                null,
                null,
                null,
                null,
                tags,
                null);
    }
}
