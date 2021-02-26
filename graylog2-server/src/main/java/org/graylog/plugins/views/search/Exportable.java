package org.graylog.plugins.views.search;

public interface Exportable {
    default boolean isExportable() {
        return false;
    }
}
