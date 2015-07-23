package org.graylog2.database;

import com.google.common.base.Function;
import org.bson.types.ObjectId;

import javax.annotation.Nullable;

public class ObjectIdStringFunction implements Function<ObjectId, String> {
    @Nullable
    @Override
    public String apply(ObjectId input) {
        return input.toHexString();
    }
}
