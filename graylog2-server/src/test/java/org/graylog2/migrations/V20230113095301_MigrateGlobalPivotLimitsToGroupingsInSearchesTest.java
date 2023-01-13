package org.graylog2.migrations;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog2.database.MongoConnection;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(MockitoExtension.class)
class V20230113095301_MigrateGlobalPivotLimitsToGroupingsInSearchesTest {
    private final V20230113095301_MigrateGlobalPivotLimitsToGroupingsInSearches migration;
    private final MongoCollection<Document> collection;

    private final ClusterConfigService clusterConfigService;

    public V20230113095301_MigrateGlobalPivotLimitsToGroupingsInSearchesTest(MongoDBTestService mongoDBTestService, @Mock ClusterConfigService clusterConfigService) {
        final MongoConnection mongoConnection = mongoDBTestService.mongoConnection();
        collection = mongoConnection.getMongoDatabase().getCollection("searches");
        migration = new V20230113095301_MigrateGlobalPivotLimitsToGroupingsInSearches(mongoConnection, clusterConfigService);
        this.clusterConfigService = clusterConfigService;
    }

    @Test
    @MongoDBFixtures("V20230113095301_MigrateGlobalPivotLimitsToGroupingsInSearchesTest_empty.json")
    void notMigratingAnythingIfPreviousMigrationDidNotRun() {
        this.migration.upgrade();

        verify(this.clusterConfigService, never()).write(any());
    }

    @Test
    @MongoDBFixtures("V20230113095301_MigrateGlobalPivotLimitsToGroupingsInSearchesTest_empty.json")
    void notMigratingAnythingIfNoSearchesArePresent() {
        markPreviousMigrationAsBeingRun();

        this.migration.upgrade();

        assertThat(migrationCompleted().migratedSearchTypes()).isZero();
    }

    @Test
    @MongoDBFixtures("V20230113095301_MigrateGlobalPivotLimitsToGroupingsInSearchesTest_simpleSearch.json")
    void migratingSimpleView() {
        markPreviousMigrationAsBeingRun();

        this.migration.upgrade();

        assertThat(migrationCompleted().migratedSearchTypes()).isEqualTo(1);

        final Document document = this.collection.find().first();
        assertThat(document).isNotNull();

        final Document searchType = getSearchTypes(document).get(0);

        assertThat(limits(rowGroups(searchType))).containsExactly(10);
        assertThat(limits(columnGroups(searchType))).isEmpty();
        assertThatFieldsAreUnset(searchType);
    }

    @Test
    @MongoDBFixtures("V20230113095301_MigrateGlobalPivotLimitsToGroupingsInSearchesTest_multiplePivots.json")
    void migratingMultiplePivots() {
        markPreviousMigrationAsBeingRun();

        this.migration.upgrade();

        assertThat(migrationCompleted().migratedSearchTypes()).isEqualTo(4);

        final Document document = this.collection.find().first();

        final List<Document> searchTypes = getSearchTypes(document);

        assertThat(limits(rowGroups(searchTypes.get(0)))).containsExactly(3);
        assertThat(limits(columnGroups(searchTypes.get(0)))).containsExactly(10);
        assertThatFieldsAreUnset(searchTypes.get(0));

        assertThat(limits(rowGroups(searchTypes.get(1)))).containsExactly(20, null, 20);
        assertThat(limits(columnGroups(searchTypes.get(1)))).isEmpty();
        assertThatFieldsAreUnset(searchTypes.get(1));

        assertThat(limits(rowGroups(searchTypes.get(2)))).containsExactly(15, 15, 15);
        assertThat(limits(columnGroups(searchTypes.get(2)))).isEmpty();
        assertThatFieldsAreUnset(searchTypes.get(2));

        assertThat(limits(rowGroups(searchTypes.get(3)))).isEmpty();
        assertThat(limits(columnGroups(searchTypes.get(3)))).containsExactly(null, 15, 15);
        assertThatFieldsAreUnset(searchTypes.get(3));
    }

    private void assertThatFieldsAreUnset(Document searchType) {
        assertThat(searchType.getInteger("row_limit")).isNull();
        assertThat(searchType.getInteger("column_limit")).isNull();
    }

    private void markPreviousMigrationAsBeingRun() {
        when(clusterConfigService.get(V20220930095323_MigratePivotLimitsInSearches.MigrationCompleted.class))
                .thenReturn(new V20220930095323_MigratePivotLimitsInSearches.MigrationCompleted(0));
    }

    private V20230113095301_MigrateGlobalPivotLimitsToGroupingsInSearches.MigrationCompleted migrationCompleted() {
        final ArgumentCaptor<V20230113095301_MigrateGlobalPivotLimitsToGroupingsInSearches.MigrationCompleted> migrationCompletedCaptor = ArgumentCaptor
                .forClass(V20230113095301_MigrateGlobalPivotLimitsToGroupingsInSearches.MigrationCompleted.class);
        verify(this.clusterConfigService, times(1)).write(migrationCompletedCaptor.capture());
        return migrationCompletedCaptor.getValue();
    }

    private List<Document> getSearchTypes(Document document) {
        return document.getList("queries", Document.class)
                .get(0)
                .getList("search_types", Document.class);
    }

    private List<Integer> limits(List<Document> groups) {
        return groups.stream()
                .map(rowGroup -> rowGroup.getInteger("limit"))
                .collect(Collectors.toList());
    }

    private List<Document> rowGroups(@Nullable Document searchType) {
        return searchType.getList("row_groups", Document.class);
    }

    private List<Document> columnGroups(@Nullable Document searchType) {
        return searchType.getList("column_groups", Document.class);
    }
}
