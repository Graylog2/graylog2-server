package org.graylog2.migrations;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.joschi.jadconfig.util.Size;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.bson.Document;
import org.graylog2.database.MongoConnection;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;

public class V20251208120700_MigrateClusterEventsToCappedCollection extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20251208120700_MigrateClusterEventsToCappedCollection.class);
    private static final String COLLECTION_NAME = "cluster_events";
    private final ClusterConfigService clusterConfigService;
    private final MongoConnection mongoConnection;
    private final Size maxEventsCollectionSize;

    @Inject
    public V20251208120700_MigrateClusterEventsToCappedCollection(ClusterConfigService clusterConfigService, MongoConnection mongoConnection,
                                                                  @Named("max_events_collection_size") Size maxEventsCollectionSize) {
        this.clusterConfigService = clusterConfigService;
        this.mongoConnection = mongoConnection;
        this.maxEventsCollectionSize = maxEventsCollectionSize;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2025-12-08T12:07:00Z");
    }

    @Override
    public void upgrade() {
        if (clusterConfigService.get(MigrationCompleted.class) != null) {
            LOG.debug("Migration already completed.");
            return;
        }

        final var collectionExists = mongoConnection.getDatabase().collectionExists(COLLECTION_NAME);
        if (collectionExists) {
            mongoConnection.getMongoDatabase().runCommand(
                    new Document("convertToCapped", COLLECTION_NAME)
                    .append("size", maxEventsCollectionSize.toBytes())
            );
        }

        clusterConfigService.write(new MigrationCompleted(collectionExists));
    }

    record MigrationCompleted(@JsonProperty("collection_existed_before") boolean collectionExistedBefore) {}
}
