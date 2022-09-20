package org.graylog.storage.elasticsearch7;

import org.graylog.shaded.elasticsearch7.org.elasticsearch.index.query.BoolQueryBuilder;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.index.query.QueryBuilders;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;

import java.util.Collection;
import java.util.Objects;

public class BoolQueryTools {

    public static void addTimeRange(BoolQueryBuilder boolQueryBuilder, final TimeRange timeRange, final String identifier) {
        boolQueryBuilder.must(
                Objects.requireNonNull(
                        TimeRangeQueryFactory.create(timeRange),
                        "Timerange for " + identifier + " cannot be found."
                )
        );
    }

    public static void addStreams(BoolQueryBuilder boolQueryBuilder, final Collection<String> streamIds) {
        boolQueryBuilder.must(QueryBuilders.termsQuery(Message.FIELD_STREAMS, streamIds));
    }
}
