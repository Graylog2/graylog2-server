package org.graylog2.database.grouping;

import org.apache.shiro.subject.Subject;

import java.util.List;

public interface EntityFieldGroupingService {

    List<EntityFieldGroup> groupByField(String collectionName,
                                        String fieldName,
                                        String query,
                                        String groupFilter,
                                        int page,
                                        int pageSize,
                                        Subject subject);
}
