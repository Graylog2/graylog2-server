/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.plugins.views.migrations.V20200204122000_MigrateUntypedViewsToDashboards;

import org.bson.Document;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

class Search {
    private static final String FIELD_QUERIES = "queries";
    private final Document searchDocument;

    private final Set<Query> queries;

    Search(Document searchDocument) {
        this.searchDocument = searchDocument;
        @SuppressWarnings("unchecked") final Collection<Document> rawQueriesList = searchDocument.get(FIELD_QUERIES, Collection.class);
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
