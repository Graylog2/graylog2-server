package org.mongojack;

import com.mongodb.MongoException;
import org.bson.BsonValue;
import org.bson.types.ObjectId;

import static org.graylog2.shared.utilities.StringUtils.f;

public interface WriteResult<T, K> {
    default T getSavedObject() {
        throw new MongoException("No objects to return");
    }

    int getN();

    boolean wasAcknowledged();

    default K getSavedId() {
        throw new MongoException("No Id to return");
    }

    default Object getUpsertedId() {
        throw new MongoException("No Id to return");
    }

    default boolean isUpdateOfExisting() {
        throw new MongoException("No update information available");
    }

    @SuppressWarnings("unchecked")
    static <L> L toIdType(BsonValue id, Class<L> idType) {
        if (String.class.isAssignableFrom(idType)) {
            return (L) id.asString().toString();
        }
        if (ObjectId.class.isAssignableFrom(idType)) {
            return (L) id.asObjectId().getValue();
        }
        throw new IllegalArgumentException(f("Only String and ObjectID types supported for ID. Got %s.",
                id.getBsonType()));
    }
}
