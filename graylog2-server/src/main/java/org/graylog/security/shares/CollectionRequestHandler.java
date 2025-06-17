package org.graylog.security.shares;

import org.graylog.grn.GRN;

import java.util.Set;

public interface CollectionRequestHandler {
    /**
     * Pluggable handler to add given entity to the specified collections.
     */
    default void addToCollection(GRN entity, Set<GRN> collections) {
        // Intentionally left empty - ignore this in the community edition.
    }
}
