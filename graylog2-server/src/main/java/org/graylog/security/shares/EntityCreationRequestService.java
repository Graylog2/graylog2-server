package org.graylog.security.shares;

import jakarta.inject.Inject;
import org.graylog.grn.GRN;

import java.util.Set;

public class EntityCreationRequestService {
    Set<CollectionRequestHandler> collectionRequestHandlers;

    @Inject
    public EntityCreationRequestService(Set<CollectionRequestHandler> collectionRequestHandlers) {
        this.collectionRequestHandlers = collectionRequestHandlers;
    }

    public void handleCollections(GRN entity, Set<GRN> collections) {
        for (CollectionRequestHandler handler : collectionRequestHandlers) {
            handler.addToCollection(entity, collections);
        }
    }
}
