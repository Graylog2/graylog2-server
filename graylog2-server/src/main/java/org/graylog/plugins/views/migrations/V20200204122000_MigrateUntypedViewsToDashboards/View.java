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
import org.bson.types.ObjectId;

import java.util.Map;
import java.util.stream.Collectors;

class View {
    private static final String FIELD_ID = "_id";
    private static final String FIELD_SEARCH_ID = "search_id";
    private static final String FIELD_STATES = "state";
    private static final String FIELD_TYPE = "type";
    private static final String TYPE_DASHBOARD = "DASHBOARD";

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

    void makeDashboard() {
        viewDocument.put(FIELD_TYPE, TYPE_DASHBOARD);
    }

    Document viewDocument() {
        return this.viewDocument;
    }
}
