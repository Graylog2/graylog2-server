package org.graylog.plugins.views.migrations.V20200204122000_MigrateUntypedViewsToDashboards;

import org.bson.Document;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

class Search {
    private static final String FIELD_QUERIES = "queries";
    private static final String FIELD_QUERY_ID = "id";
    private final Document searchDocument;

    Search(Document searchDocument) {
        this.searchDocument = searchDocument;
    }

    Optional<Query> queryById(String queryId) {
        @SuppressWarnings("rawtypes") final List rawQueriesList = searchDocument.get(FIELD_QUERIES, List.class);
        if (rawQueriesList == null) {
            return Optional.empty();
        }
        @SuppressWarnings("unchecked") final Set<Document> queries = new HashSet<Document>(rawQueriesList);
        return queries.stream()
                .filter(q -> queryId.equals(q.getString(FIELD_QUERY_ID)))
                .findFirst()
                .map(Query::new);
    }
}
