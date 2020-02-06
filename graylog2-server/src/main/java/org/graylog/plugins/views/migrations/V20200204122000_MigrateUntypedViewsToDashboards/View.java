package org.graylog.plugins.views.migrations.V20200204122000_MigrateUntypedViewsToDashboards;

import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.Map;
import java.util.stream.Collectors;

class View {
    private static final String FIELD_ID = "_id";
    private static final String FIELD_SEARCH_ID = "search_id";
    private static final String FIELD_STATES = "state";

    private final Document viewDocument;

    View(Document viewDocument) {
        this.viewDocument = viewDocument;
    }

    ObjectId objectId() {
        return viewDocument.getObjectId(FIELD_ID);
    }

    String searchId() {
        return viewDocument.getString(FIELD_SEARCH_ID);
    }

    Map<String, ViewState> viewStates() {
        final Document states = viewDocument.get(FIELD_STATES, Document.class);
        return states.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> new ViewState((Document)entry.getValue())));

    }
}
