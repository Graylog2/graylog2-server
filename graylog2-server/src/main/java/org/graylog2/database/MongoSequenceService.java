package org.graylog2.database;

import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Updates;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.bson.Document;
import org.graylog2.plugin.system.NodeId;

import java.util.Objects;
import java.util.Set;

@Singleton
public class MongoSequenceService {

    static final String COLLECTION_NAME = "sequence_counters";
    static final String FIELD_VALUE = "value";
    static final String FIELD_LAST_UPDATED_AT = "last_updated_at";
    static final String FIELD_LAST_UPDATED_BY = "last_updated_by";

    private final MongoCollection<Document> collection;
    private final NodeId nodeId;
    private final Set<String> registeredTopics;

    @Inject
    public MongoSequenceService(MongoCollections mongoCollections,
                                NodeId nodeId,
                                @SequenceTopics Set<String> registeredTopics) {
        this.collection = mongoCollections.nonEntityCollection(COLLECTION_NAME, Document.class)
                .withWriteConcern(WriteConcern.JOURNALED);
        this.nodeId = nodeId;
        this.registeredTopics = Set.copyOf(registeredTopics);
    }

    public long incrementAndGet(String topic) {
        validateTopic(topic);
        final Document result = collection.findOneAndUpdate(
                Filters.eq("_id", topic),
                Updates.combine(
                        Updates.inc(FIELD_VALUE, 1L),
                        Updates.currentDate(FIELD_LAST_UPDATED_AT),
                        Updates.set(FIELD_LAST_UPDATED_BY, nodeId.getNodeId())
                ),
                new FindOneAndUpdateOptions()
                        .upsert(true)
                        .returnDocument(ReturnDocument.AFTER)
        );
        Objects.requireNonNull(result, "upserted sequence entry is null, this should never happen");
        return result.getLong(FIELD_VALUE);
    }

    public long getCurrentValue(String topic) {
        validateTopic(topic);
        final Document doc = collection.find(Filters.eq("_id", topic)).first();
        return doc == null ? 0L : doc.getLong(FIELD_VALUE);
    }

    // Package-private for test access
    MongoCollection<Document> getCollection() {
        return collection;
    }

    private void validateTopic(String topic) {
        if (!registeredTopics.contains(topic)) {
            throw new IllegalArgumentException("Unknown sequence topic: " + topic);
        }
    }
}
