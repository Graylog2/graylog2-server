package org.graylog2.rest.helpers;

import org.bson.types.ObjectId;

public class DatabaseIdParser {
    public static class InvalidObjectIdException extends RuntimeException {
        public InvalidObjectIdException(String message) {
            super(message);
        }
    }

    public static ObjectId safeParseObjectId(String id) {
        try {
            return new ObjectId(id);
        } catch (IllegalArgumentException e) {
            throw new InvalidObjectIdException("Invalid id: " + id);
        }
    }
}
