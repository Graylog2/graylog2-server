package org.graylog2.migrations;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.mongodb.client.MongoCollection;
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

public class V20230113095300_MigrateGlobalPivotLimitsToGroupingsInSearches extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20230113095300_MigrateGlobalPivotLimitsToGroupingsInViews.class);
    private final ClusterConfigService clusterConfigService;
    private final MongoCollection<Document> searches;
    private final Document matchValuePivots = doc("pivot.type", "values");

    @Inject
    public V20230113095300_MigrateGlobalPivotLimitsToGroupingsInSearches(MongoConnection mongoConnection, ClusterConfigService clusterConfigService) {
        this.clusterConfigService = clusterConfigService;
        this.searches = mongoConnection.getMongoDatabase().getCollection("searches");
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2023-01-13T09:53:01Z");
    }

    @Override
    public void upgrade() {
        if (clusterConfigService.get(V20220930095323_MigratePivotLimitsInSearches.MigrationCompleted.class) == null) {
            LOG.debug("Previous migration did not run - no need to migrate!");
            return;
        }
        if (clusterConfigService.get(MigrationCompleted.class) != null) {
            LOG.debug("Migration already completed!");
        }

        final List<SearchPivotLimitMigration> pivotLimitMigrations = StreamSupport.stream(this.searches.find().spliterator(), false)
            .flatMap(document -> {
                final String searchId = document.get("_id", ObjectId.class).toHexString();
                final List<Document> queries = document.get("queries", Collections.emptyList());
                return EntryStream.of(queries)
                    .flatMap(entry -> {
                        final Integer queryIndex = entry.getKey();
                        final List<Document> searchTypes = entry.getValue().get("search_types", Collections.emptyList());
                        return EntryStream.of(searchTypes)
                            .filter(searchType -> "pivot".equals(searchType.getValue().getString("type")))
                            .filter(this::searchTypeHasValuePivots)
                            .flatMap(searchTypeEntry -> {
                                final Document searchType = searchTypeEntry.getValue();
                                final Integer searchTypeIndex = searchTypeEntry.getKey();
                                final Optional<Integer> rowLimit = Optional.ofNullable(searchType.getInteger("row_limit"));
                                final Optional<Integer> columnLimit = Optional.ofNullable(searchType.getInteger("column_limit"));

                                if (searchTypeIndex != null && (rowLimit.isPresent() || columnLimit.isPresent())) {
                                    return Stream.of(new SearchPivotLimitMigration(searchId, queryIndex, searchTypeIndex, rowLimit, columnLimit));
                                }
                                return Stream.empty();
                            });
                    });
            })
            .collect(Collectors.toList());

        final List<WriteModel<Document>> operations = pivotLimitMigrations.stream()
                .flatMap(pivotMigration -> {
                    final ImmutableList.Builder<WriteModel<Document>> builder = ImmutableList.builder();
                    builder.add(
                        updateSearch(
                            pivotMigration.searchId(),
                            doc("$unset", doc(pivotPath(pivotMigration) + ".row_limit", 1))
                        )
                    );
                    pivotMigration.rowLimit().ifPresent(rowLimit -> {
                        builder.add(
                            updateSearch(
                                pivotMigration.searchId(),
                                doc("$set", doc(pivotPath(pivotMigration) + ".row_groups.$[pivot].limit", rowLimit)),
                                matchValuePivots
                            )
                        );
                    });
                    builder.add(
                            updateSearch(
                                    pivotMigration.searchId(),
                                    doc("$unset", doc(pivotPath(pivotMigration) + ".column_limit", 1))
                            )
                    );
                    pivotMigration.columnLimit().ifPresent(columnLimit -> {
                        builder.add(
                            updateSearch(
                                pivotMigration.searchId(),
                                doc("$set", doc(pivotPath(pivotMigration) + ".column_groups.$[pivot].limit", columnLimit)),
                                matchValuePivots
                            )
                        );
                    });
                    return builder.build().stream();
                })
                .collect(Collectors.toList());

        if (!operations.isEmpty()) {
            LOG.debug("Updating {} search types ...", pivotLimitMigrations.size());
            this.searches.bulkWrite(operations);
        }

        clusterConfigService.write(new MigrationCompleted(pivotLimitMigrations.size()));
    }

    private boolean searchTypeHasValuePivots(Map.Entry<Integer, Document> searchTypeEntry) {
        final Document searchType = searchTypeEntry.getValue();
        final List<Document> rowPivots = searchType.getList("row_groups", Document.class, Collections.emptyList());
        if (rowPivots.stream().anyMatch(rowPivot -> "values".equals(rowPivot.get("type")))) {
            return true;
        }
        final List<Document> columnPivots = searchType.getList("column_groups", Document.class, Collections.emptyList());
        return columnPivots.stream().anyMatch(rowPivot -> "values".equals(rowPivot.get("type")));
    }

    private String pivotPath(SearchPivotLimitMigration pivotMigration) {
        return "queries." + pivotMigration.queryIndex() + ".search_types." + pivotMigration.searchTypeIndex();
    }

    private WriteModel<Document> updateSearch(String searchId, Document update, List<Bson> arrayFilters) {
        return new UpdateOneModel<>(
                doc("_id", new ObjectId(searchId)),
                update,
                new UpdateOptions().upsert(false).arrayFilters(arrayFilters)
        );
    }

    private WriteModel<Document> updateSearch(String searchId, Document update, Bson arrayFilter) {
        return updateSearch(searchId, update, Collections.singletonList(arrayFilter));
    }

    private WriteModel<Document> updateSearch(String searchId, Document update) {
        return updateSearch(searchId, update, (List<Bson>) null);
    }

    private Document doc(String key, Object value) {
        return new Document(key, value);
    }

    public record SearchPivotLimitMigration(String searchId, Integer queryIndex, Integer searchTypeIndex, Optional<Integer> rowLimit, Optional<Integer> columnLimit) {}

    public record MigrationCompleted(@JsonProperty("migrated_search_types") Integer migratedSearchTypes) {}
}
