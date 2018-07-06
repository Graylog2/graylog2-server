/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.migrations;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.graylog2.database.MongoConnection;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.grok.GrokPatternsChangedEvent;
import org.graylog2.grok.MongoDbGrokPatternService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static com.mongodb.client.model.Filters.eq;

/**
 * Migration removing duplicate Grok patterns and adding a unique index to the "grok_patterns" collection in
 * MongoDB.
 */
public class V2018070614390000_EnforceUniqueGrokPatterns extends Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V2018070614390000_EnforceUniqueGrokPatterns.class);

    private final MongoCollection<Document> collection;
    private final ClusterEventBus clusterEventBus;
    private boolean indexCreated = false;

    @Inject
    public V2018070614390000_EnforceUniqueGrokPatterns(MongoConnection mongoConnection, ClusterEventBus clusterEventBus) {
        this(mongoConnection.getMongoDatabase().getCollection(MongoDbGrokPatternService.COLLECTION_NAME), clusterEventBus);
    }

    V2018070614390000_EnforceUniqueGrokPatterns(MongoCollection<Document> collection, ClusterEventBus clusterEventBus) {
        this.collection = collection;
        this.clusterEventBus = clusterEventBus;
    }

    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2018-07-06T14:39:00Z");
    }

    @Override
    public void upgrade() {
        boolean indexExists = false;
        for (Document document : collection.listIndexes()) {
            if (MongoDbGrokPatternService.INDEX_NAME.equals(document.getString("name")) && document.getBoolean("unique")) {
                indexExists = true;
                break;
            }
        }

        if (indexExists) {
            LOG.debug("Unique index for Grok patterns already exists, skipping migration.");
            return;
        }

        final Collection<String> grokPatterns = new HashSet<>();
        final Map<ObjectId, String> duplicatePatterns = new HashMap<>();
        for (Document document : collection.find()) {
            final ObjectId id = document.getObjectId("_id");
            final String name = document.getString("name");
            final String pattern = document.getString("pattern");
            if (grokPatterns.contains(name)) {
                LOG.info("Marking duplicate Grok pattern <{}> for removal: {}\t{}", id, name, pattern);
                duplicatePatterns.put(id, name);
            } else {
                LOG.debug("Recording Grok pattern <{}>: {}\t{}", id, name, pattern);
                grokPatterns.add(name);
            }
        }

        for (ObjectId id : duplicatePatterns.keySet()) {
            LOG.info("Deleting duplicate Grok pattern with ID <{}>", id);
            collection.deleteOne(eq("_id", id));
        }

        final IndexOptions indexOptions = new IndexOptions()
                .name(MongoDbGrokPatternService.INDEX_NAME)
                .unique(true);
        collection.createIndex(Indexes.ascending("name"), indexOptions);

        if (!duplicatePatterns.isEmpty()) {
            clusterEventBus.post(GrokPatternsChangedEvent.create(Collections.emptySet(), ImmutableSet.copyOf(duplicatePatterns.values())));
        }

        indexCreated = true;
    }

    @VisibleForTesting
    boolean isIndexCreated() {
        return indexCreated;
    }
}
