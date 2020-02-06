package org.graylog.plugins.views.migrations.V20200204122000_MigrateUntypedViewsToDashboards;

import org.bson.Document;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

class Search {
    private static final String FIELD_QUERIES = "queries";
    private final Document searchDocument;

    private final Set<Query> queries;

    Search(Document searchDocument) {
        this.searchDocument = searchDocument;
        @SuppressWarnings("rawtypes") final List rawQueriesList = searchDocument.get(FIELD_QUERIES, List.class);
        if (rawQueriesList == null) {
            this.queries = Collections.emptySet();
        } else {
            this.queries = new HashSet<Document>(rawQueriesList).stream()
                    .map(Query::new)
                    .collect(Collectors.toSet());
        }
    }

    Set<Query> queries() {
        return this.queries;
    }

    Optional<Query> queryById(String queryId) {
        return queries.stream()
                .filter(q -> queryId.equals(q.id()))
                .findFirst();
    }

    Document searchDocument() {
        return this.searchDocument;
    }
}
