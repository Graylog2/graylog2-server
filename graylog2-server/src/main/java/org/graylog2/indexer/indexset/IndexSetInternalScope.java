package org.graylog2.indexer.indexset;

import org.graylog2.database.entities.EntityScope;

public class IndexSetInternalScope extends EntityScope {
    public static final String NAME = "GRAYLOG_INTERNAL_INDEXSET_SCOPE";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public boolean isDeletable() {
        return false;
    }
}
