package org.graylog.plugins.views.migrations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.migrations.Migration;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.plugins.views.migrations.V20200409083200_RemoveRootQueriesFromMigratedDashboards.MigrationCompleted;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class V20200409083200_RemoveRootQueriesFromMigratedDashboardsTest {
    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private ClusterConfigService clusterConfigService;

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();

    private Migration migration;

    private MongoCollection<Document> viewsCollection;
    private MongoCollection<Document> searchesCollection;

    @Before
    public void setUp() {
        this.searchesCollection = spy(mongodb.mongoConnection().getMongoDatabase().getCollection("searches"));
        this.viewsCollection = spy(mongodb.mongoConnection().getMongoDatabase().getCollection("views"));
        this.migration = new V20200409083200_RemoveRootQueriesFromMigratedDashboards(clusterConfigService, this.viewsCollection, this.searchesCollection);
    }

    @Test
    public void runsIfNoDashboardsArePresent() {
        this.migration.upgrade();
    }

    @Test
    public void writesMigrationCompletedAfterSuccess() {
        this.migration.upgrade();

        final MigrationCompleted migrationCompleted = captureMigrationCompleted();
        assertThat(migrationCompleted.modifiedViewsCount()).isZero();
    }

    @Test
    public void doesNotRunIfMigrationHasCompletedBefore() {
        when(clusterConfigService.get(MigrationCompleted.class)).thenReturn(MigrationCompleted.create(0));

        this.migration.upgrade();

        verify(viewsCollection, never()).find(any(Bson.class));
        verify(searchesCollection, never()).find(any(Bson.class));
    }

    @Test
    @MongoDBFixtures("V20200409083200_RemoveRootQueriesFromMigratedDashboards/sample.json")
    public void findsCorrectDocuments() throws JsonProcessingException, JSONException {
        migration.upgrade();

        final ArrayList<Document> searches = mongodb.mongoConnection()
                .getMongoDatabase()
                .getCollection("searches")
                .find()
                .into(new ArrayList<>());
        JSONAssert.assertEquals(resourceFile("V20200409083200_RemoveRootQueriesFromMigratedDashboards/sample-expected.json"), toJSON(searches), false);
    }

    private MigrationCompleted captureMigrationCompleted() {
        final ArgumentCaptor<MigrationCompleted> migrationCompletedCaptor = ArgumentCaptor.forClass(MigrationCompleted.class);
        verify(clusterConfigService, times(1)).write(migrationCompletedCaptor.capture());
        return migrationCompletedCaptor.getValue();
    }

    private String toJSON(Object object) throws JsonProcessingException {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
    }

    private String resourceFile(String filename) {
        try {
            final URL resource = this.getClass().getResource(filename);
            final Path path = Paths.get(resource.toURI());
            final byte[] bytes = Files.readAllBytes(path);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
