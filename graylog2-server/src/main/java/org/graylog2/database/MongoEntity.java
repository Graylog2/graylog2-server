package org.graylog2.database;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.mongojack.Id;
import org.mongojack.ObjectId;

/**
 * Common interface for entities stored in MongoDB.
 */
public interface MongoEntity {

    /**
     * ID of the entity. Will be stored as field "_id" with type ObjectId in MongoDB.
     *
     * @return Hex string representation of the entity's ID
     */
    @ObjectId
    @Id
    @JsonProperty("id")
    String id();
}
