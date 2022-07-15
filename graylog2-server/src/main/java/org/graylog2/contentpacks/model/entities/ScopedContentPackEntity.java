package org.graylog2.contentpacks.model.entities;

import org.graylog2.contentpacks.model.entities.references.ValueReference;

public interface ScopedContentPackEntity {
    String FIELD_SCOPE = "scope";
    ValueReference scope();
}
