package org.graylog2.entitygroups.entities;

import org.graylog2.contentpacks.model.ModelType;

public interface GroupableEntity {
    // The name that will be used for grouping entities of this type.
    String entityTypeName();

    // The ModelType to be used for content pack handling for entities of this type.
    default ModelType modelType() {
        throw new UnsupportedOperationException("Content pack support is not implemented for " + entityTypeName());
    }
}
