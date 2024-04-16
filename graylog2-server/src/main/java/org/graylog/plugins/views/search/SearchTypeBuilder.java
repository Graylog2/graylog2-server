package org.graylog.plugins.views.search;

import org.graylog.plugins.views.search.timeranges.DerivedTimeRange;

public interface SearchTypeBuilder {
    SearchType build();

    SearchTypeBuilder timerange(DerivedTimeRange timerange);
}
