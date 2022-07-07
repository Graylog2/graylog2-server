package org.graylog2.database.entities;

public class DefaultEntityScope implements EntityScope {

    public static final String NAME = "default";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean isMutable() {
        return true;
    }
}
