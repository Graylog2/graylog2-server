package org.graylog.storage.elasticsearch7;

import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.sort.SortOrder;
import org.graylog2.indexer.searches.Sorting;

import java.util.Locale;

public class SortOrderMapper {
    public SortOrder fromSorting(Sorting sorting) {
        return SortOrder.valueOf(sorting.getDirection().toString().toUpperCase(Locale.ENGLISH));
    }
}
