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
class V20220929145442_MigratePivotLimitsInViewsTest {
    private final Migration migration;
    private final MongoCollection<Document> collection;

    private final ClusterConfigService clusterConfigService;

    public V20220929145442_MigratePivotLimitsInViewsTest(MongoDBTestService mongoDBTestService, @Mock ClusterConfigService clusterConfigService) {
        final MongoConnection mongoConnection = mongoDBTestService.mongoConnection();
        collection = mongoConnection.getMongoDatabase().getCollection("views");
        migration = new V20220929145442_MigratePivotLimitsInViews(mongoConnection, clusterConfigService);
        this.clusterConfigService = clusterConfigService;
    }

    @Test
    @MongoDBFixtures("V20220929145442_MigratePivotLimitsInViewsTest_empty.json")
    void migratingEmptyCollection() {
        this.migration.upgrade();

        assertThat(migrationCompleted().migratedViews()).isZero();
    }

    @Test
    @MongoDBFixtures("V20220929145442_MigratePivotLimitsInViewsTest_simpleView.json")
    void migratingSimpleView() {
        this.migration.upgrade();

        assertThat(migrationCompleted().migratedViews()).isEqualTo(1);

        final Document document = this.collection.find().first();

        final Document widget = getWidgets(document).get(0);

        validateWidget(widget, 5, null);
    }

    @Test
    @MongoDBFixtures("V20220929145442_MigratePivotLimitsInViewsTest_multiplePivots.json")
    void migratingMultiplePivots() {
        this.migration.upgrade();

        assertThat(migrationCompleted().migratedViews()).isEqualTo(4);

        final Document document = this.collection.find().first();

        final List<Document> widgets = getWidgets(document);

        validateWidget(widgets.get(0), 3, 10);
        validateWidget(widgets.get(1), 20, null);
        validateWidget(widgets.get(2), 15, null);
        validateWidget(widgets.get(3), null, 15);
    }

    private void validateWidget(Document widget, Integer newRowLimit, Integer newColumnLimit) {
        assertThat(rowLimit(widget)).isEqualTo(newRowLimit);
        assertThat(columnLimit(widget)).isEqualTo(newColumnLimit);

        for (Document pivot : rowPivots(widget)) {
            assertThat(pivotLimit(pivot)).isNull();
        }

        for (Document pivot : columnPivots(widget)) {
            assertThat(pivotLimit(pivot)).isNull();
        }
    }

    private Integer pivotLimit(Document pivot) {
        return pivot.getEmbedded(List.of("config", "limit"), Integer.class);
    }

    private V20220929145442_MigratePivotLimitsInViews.MigrationCompleted migrationCompleted() {
        final ArgumentCaptor<V20220929145442_MigratePivotLimitsInViews.MigrationCompleted> migrationCompletedCaptor = ArgumentCaptor.forClass(V20220929145442_MigratePivotLimitsInViews.MigrationCompleted.class);
        verify(this.clusterConfigService, times(1)).write(migrationCompletedCaptor.capture());
        return migrationCompletedCaptor.getValue();
    }

    private Integer rowLimit(Document widget) {
        return widget.getEmbedded(List.of("config", "row_limit"), Integer.class);
    }

    private Integer columnLimit(Document widget) {
        return widget.getEmbedded(List.of("config", "column_limit"), Integer.class);
    }

    private List<Document> getWidgets(@Nullable Document document) {
        return document.getEmbedded(List.of("state", "e4a3962e-7477-4ea0-8290-48a0da8b2c78", "widgets"), List.class);
    }

    private List<Document> rowPivots(@Nullable Document widget) {
        return widget.getEmbedded(List.of("config", "row_pivots"), List.class);
    }

    private List<Document> columnPivots(@Nullable Document widget) {
        return widget.getEmbedded(List.of("config", "column_pivots"), List.class);
    }
}
