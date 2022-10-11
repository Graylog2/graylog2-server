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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(MockitoExtension.class)
class V20220930095323_MigratePivotLimitsInSearchesTest {
    private final Migration migration;
    private final MongoCollection<Document> collection;

    private final ClusterConfigService clusterConfigService;

    public V20220930095323_MigratePivotLimitsInSearchesTest(MongoDBTestService mongoDBTestService, @Mock ClusterConfigService clusterConfigService) {
        final MongoConnection mongoConnection = mongoDBTestService.mongoConnection();
        collection = mongoConnection.getMongoDatabase().getCollection("searches");
        migration = new V20220930095323_MigratePivotLimitsInSearches(mongoConnection, clusterConfigService);
        this.clusterConfigService = clusterConfigService;
    }


    @Test
    @MongoDBFixtures("V20220930095323_MigratePivotLimitsInSearchesTest_empty.json")
    void migratingEmptyCollection() {
        this.migration.upgrade();

        assertThat(migrationCompleted().migratedSearchTypes()).isZero();
    }

    @Test
    @MongoDBFixtures("V20220930095323_MigratePivotLimitsInSearchesTest_simpleSearch.json")
    void migratingSimpleView() {
        this.migration.upgrade();

        assertThat(migrationCompleted().migratedSearchTypes()).isEqualTo(1);

        final Document document = this.collection.find().first();

        final Document searchType = getSearchTypes(document).get(0);

        validateSearchType(searchType, 10, null);
    }

    @Test
    @MongoDBFixtures("V20220930095323_MigratePivotLimitsInSearchesTest_multiplePivots.json")
    void migratingMultiplePivots() {
        this.migration.upgrade();

        assertThat(migrationCompleted().migratedSearchTypes()).isEqualTo(4);

        final Document document = this.collection.find().first();

        final List<Document> searchTypes = getSearchTypes(document);

        validateSearchType(searchTypes.get(0), 3, 10);
        validateSearchType(searchTypes.get(1), 20, null);
        validateSearchType(searchTypes.get(2), 15, null);
        validateSearchType(searchTypes.get(3), null, 15);
    }

    private void validateSearchType(Document searchType, Integer newRowLimit, Integer newColumnLimit) {
        assertThat(rowLimit(searchType)).isEqualTo(newRowLimit);
        assertThat(columnLimit(searchType)).isEqualTo(newColumnLimit);

        for (Document pivot : rowGroups(searchType)) {
            assertThat(pivotLimit(pivot)).isNull();
        }

        for (Document pivot : columnGroups(searchType)) {
            assertThat(pivotLimit(pivot)).isNull();
        }
    }

    private Integer pivotLimit(Document pivot) {
        return pivot.getInteger("limit");
    }

    private V20220930095323_MigratePivotLimitsInSearches.MigrationCompleted migrationCompleted() {
        final ArgumentCaptor<V20220930095323_MigratePivotLimitsInSearches.MigrationCompleted> migrationCompletedCaptor = ArgumentCaptor.forClass(V20220930095323_MigratePivotLimitsInSearches.MigrationCompleted.class);
        verify(this.clusterConfigService, times(1)).write(migrationCompletedCaptor.capture());
        return migrationCompletedCaptor.getValue();
    }

    private Integer rowLimit(Document searchType) {
        return searchType.getInteger("row_limit");
    }

    private Integer columnLimit(Document searchType) {
        return searchType.getInteger("column_limit");
    }

    private List<Document> getSearchTypes(@Nullable Document document) {
        return document.getList("queries", Document.class)
                .get(0)
                .getList("search_types", Document.class);
    }

    private List<Document> rowGroups(@Nullable Document searchType) {
        return searchType.getList("row_groups", Document.class);
    }

    private List<Document> columnGroups(@Nullable Document searchType) {
        return searchType.getList("column_groups", Document.class);
    }
}
