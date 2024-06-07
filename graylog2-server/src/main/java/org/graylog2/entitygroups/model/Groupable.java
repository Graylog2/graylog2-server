package org.graylog2.entitygroups.model;

import org.graylog2.contentpacks.model.ModelType;

public interface Groupable {
    String name();

    ModelType type();
}
