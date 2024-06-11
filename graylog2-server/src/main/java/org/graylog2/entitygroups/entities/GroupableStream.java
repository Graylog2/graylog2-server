package org.graylog2.entitygroups.entities;

import org.graylog2.contentpacks.model.ModelType;
import org.graylog2.contentpacks.model.ModelTypes;

public class GroupableStream implements GroupableEntity {
    public static final String TYPE_NAME = "streams";

    @Override
    public String entityTypeName() {
        return TYPE_NAME;
    }

    @Override
    public ModelType modelType() {
        return ModelTypes.STREAM_V1;
    }
}
