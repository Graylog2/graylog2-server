package org.graylog.plugins.views.migrations.V20200204122000_MigrateUntypedViewsToDashboards;

import org.bson.Document;

import java.util.Collections;
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
            @SuppressWarnings("unchecked") final List<Document> widgetList = viewStateDocument.get("widgets", List.class);
            return widgetList.stream()
                    .map(Widget::new)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private Set<String> searchTypeIdsForWidgetId(String widgetId) {
        final Document widgetMapping = viewStateDocument.get(FIELD_WIDGET_MAPPING, Document.class);
        if (widgetMapping == null) {
            return Collections.emptySet();
        }
        @SuppressWarnings("unchecked") final Set<String> result = widgetMapping.get(widgetId, Set.class);
        return result;
    }
}
