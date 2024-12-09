package org.graylog.plugins.views.search;

/**
 * Marker interface for search types that are not search engine related, but Data Warehouse/Iceberg related
 */
public interface DataWarehouseSearchType extends SearchType {

    String PREFIX = "data_warehouse_";
}
