package org.graylog2.database.grouping;

import org.apache.shiro.subject.Subject;

public interface EntityFieldGroupingService {

    EntityFieldBucketResponse groupByField(String collectionName,
                                           String fieldName,
                                           String query,
                                           String bucketsFilter,
                                           int page,
                                           int pageSize,
                                           SortOrder sortOrder,
                                           SortField sortField,
                                           Subject subject);

    enum SortOrder {
        ASC,
        DESC
    }

    enum SortField {
        COUNT,
        VALUE
    }
}
