package org.graylog.storage.elasticsearch6;

import org.elasticsearch.search.sort.SortOrder;
import org.graylog2.indexer.searches.Sorting;

import java.util.Locale;

public class SortOrderFactory {
    public SortOrder fromSorting(Sorting sorting) {
        return SortOrder.valueOf(sorting.getDirection().toString().toUpperCase(Locale.ENGLISH));
    }
}
