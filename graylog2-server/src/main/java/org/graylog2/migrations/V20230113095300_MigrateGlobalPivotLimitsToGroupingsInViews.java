package org.graylog2.migrations;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.WriteModel;
import one.util.streamex.EntryStream;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.graylog2.database.MongoConnection;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class V20230113095300_MigrateGlobalPivotLimitsToGroupingsInViews extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20230113095300_MigrateGlobalPivotLimitsToGroupingsInViews.class);
    private final ClusterConfigService clusterConfigService;
    private final MongoCollection<Document> views;
    private final Document matchValuePivots = doc("config.type", "values");

    @Inject
    public V20230113095300_MigrateGlobalPivotLimitsToGroupingsInViews(MongoConnection mongoConnection, ClusterConfigService clusterConfigService) {
        this.clusterConfigService = clusterConfigService;
        this.views = mongoConnection.getMongoDatabase().getCollection("views");
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2023-01-13T09:53:00Z");
    }

    @Override
    public void upgrade() {
        if (clusterConfigService.get(V20220929145442_MigratePivotLimitsInViews.MigrationCompleted.class) == null) {
            LOG.debug("Previous migration did not run - no need to migrate!");
            return;
        }
        if (clusterConfigService.get(MigrationCompleted.class) != null) {
            LOG.debug("Migration already completed!");
        }

        final List<ViewWidgetLimitMigration> widgetLimitMigrations = StreamSupport.stream(this.views.find().spliterator(), false)
            .flatMap(document -> {
                final String viewId = document.get("_id", ObjectId.class).toHexString();
                final Map<String, Document> state = document.get("state", Collections.emptyMap());
                return state.entrySet().stream()
                    .flatMap(entry -> {
                        final String queryId = entry.getKey();
                        final List<Document> widgets = entry.getValue().get("widgets", Collections.emptyList());
                        return EntryStream.of(widgets)
                            .filter(widget -> "aggregation".equals(widget.getValue().getString("type")))
                            .filter(this::widgetHasValuePivots)
                            .flatMap(widgetEntry -> {
                                final Document widget = widgetEntry.getValue();
                                final Integer widgetIndex = widgetEntry.getKey();
                                final Document config = widget.get("config", new Document());
                                final Optional<Integer> rowLimit = Optional.ofNullable(config.getInteger("row_limit"));
                                final Optional<Integer> columnLimit = Optional.ofNullable(config.getInteger("column_limit"));

                                if (widgetIndex != null && (rowLimit.isPresent() || columnLimit.isPresent())) {
                                    return Stream.of(new ViewWidgetLimitMigration(viewId, queryId, widgetIndex, rowLimit, columnLimit));
                                }
                                return Stream.empty();
                            });
                    });
            })
            .collect(Collectors.toList());

        final List<WriteModel<Document>> operations = widgetLimitMigrations.stream()
                .flatMap(widgetMigration -> {
                    final ImmutableList.Builder<WriteModel<Document>> builder = ImmutableList.builder();
                    builder.add(
                            updateView(
                                    widgetMigration.viewId,
                                    doc("$unset", doc(widgetConfigPath(widgetMigration) + ".row_limit", 1))
                            )
                    );
                    widgetMigration.rowLimit().ifPresent(rowLimit -> {
                        builder.add(
                                updateView(
                                        widgetMigration.viewId,
                                        doc("$set", doc(widgetConfigPath(widgetMigration) + ".row_pivots.$[config].config.limit", rowLimit)),
                                        matchValuePivots
                                )
                        );
                    });
                    builder.add(
                            updateView(
                                    widgetMigration.viewId,
                                    doc("$unset", doc(widgetConfigPath(widgetMigration) + ".column_limit", 1))
                            )
                    );
                    widgetMigration.columnLimit().ifPresent(columnLimit -> {
                        builder.add(
                                updateView(
                                        widgetMigration.viewId,
                                        doc("$set", doc(widgetConfigPath(widgetMigration) + ".column_pivots.$[config].config.limit", columnLimit)),
                                        matchValuePivots
                                )
                        );
                    });
                    return builder.build().stream();
                })
                .collect(Collectors.toList());

        if (!operations.isEmpty()) {
            LOG.debug("Updating {} widgets ...", widgetLimitMigrations.size());
            this.views.bulkWrite(operations);
        }

        clusterConfigService.write(new V20220929145442_MigratePivotLimitsInViews.MigrationCompleted(widgetLimitMigrations.size()));
    }

    private String widgetConfigPath(ViewWidgetLimitMigration widgetMigration) {
        return "state." + widgetMigration.queryId() + ".widgets." + widgetMigration.widgetIndex() + ".config";
    }

    private WriteModel<Document> updateView(String viewId, Document update, List<Bson> arrayFilters) {
        return new UpdateOneModel<>(
                Filters.eq("_id", new ObjectId(viewId)),
                update,
                new UpdateOptions().upsert(false).arrayFilters(arrayFilters)
        );
    }

    private WriteModel<Document> updateView(String viewId, Document update, Bson arrayFilters) {
        return updateView(viewId, update, Collections.singletonList(arrayFilters));
    }

    private WriteModel<Document> updateView(String viewId, Document update) {
        return updateView(viewId, update, (List<Bson>) null);
    }

    private Document doc(String key, Object value) {
        return new Document(key, value);
    }

    private boolean widgetHasValuePivots(Map.Entry<Integer, Document> widgetEntry) {
        final Document widget = widgetEntry.getValue();
        final Document config = widget.get("config", new Document());
        final List<Document> rowPivots = config.getList("row_pivots", Document.class, Collections.emptyList());
        if (rowPivots.stream().anyMatch(rowPivot -> "values".equals(rowPivot.get("type")))) {
            return true;
        }
        final List<Document> columnPivots = config.getList("column_pivots", Document.class, Collections.emptyList());
        return columnPivots.stream().anyMatch(rowPivot -> "values".equals(rowPivot.get("type")));
    }

    public record ViewWidgetLimitMigration(String viewId, String queryId, Integer widgetIndex, Optional<Integer> rowLimit, Optional<Integer> columnLimit) {}

    public record MigrationCompleted(@JsonProperty("migrated_widgets") Integer migratedViews) {}
}
