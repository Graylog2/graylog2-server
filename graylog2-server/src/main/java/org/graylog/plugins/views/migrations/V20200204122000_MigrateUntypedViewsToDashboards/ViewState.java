/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.plugins.views.migrations.V20200204122000_MigrateUntypedViewsToDashboards;

import org.bson.Document;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

class ViewState {
    private static final String FIELD_WIDGET_MAPPING = "widget_mapping";
    private final Document viewStateDocument;

    ViewState(Document viewStateDocument) {
        this.viewStateDocument = viewStateDocument;
    }

    List<Widget> widgets() {
        if (viewStateDocument.get("widgets") instanceof List) {
            @SuppressWarnings("unchecked") final Collection<Document> widgetList = viewStateDocument.get("widgets", Collection.class);
            return widgetList.stream()
                    .map(Widget::new)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    Set<String> searchTypeIdsForWidgetId(String widgetId) {
        final Document widgetMapping = viewStateDocument.get(FIELD_WIDGET_MAPPING, Document.class);
        if (widgetMapping == null) {
            return Collections.emptySet();
        }
        @SuppressWarnings("unchecked") final Collection<String> rawWidgetsList = widgetMapping.get(widgetId, Collection.class);
        if (rawWidgetsList == null) {
            return Collections.emptySet();
        }
        return new HashSet<>(rawWidgetsList);
    }
}
