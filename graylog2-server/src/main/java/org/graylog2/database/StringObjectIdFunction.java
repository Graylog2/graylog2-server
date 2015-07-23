package org.graylog2.database;

import com.google.common.base.Function;
import org.bson.types.ObjectId;

import javax.annotation.Nullable;

public class StringObjectIdFunction implements Function<String, ObjectId> {
    @Nullable
    @Override
    public ObjectId apply(String input) {
        return new ObjectId(input);
    }
}
