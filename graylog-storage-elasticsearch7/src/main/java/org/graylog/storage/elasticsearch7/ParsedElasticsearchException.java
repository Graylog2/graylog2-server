package org.graylog.storage.elasticsearch7;

import com.google.auto.value.AutoValue;

import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@AutoValue
abstract class ParsedElasticsearchException {
    /*
    ElasticsearchException[Elasticsearch exception [type=mapper_parsing_exception, reason=failed to parse field [_ourcustomfield]
    of type [long] in document with id '2f1b81f1-c050-11ea-ad64-d2850321fca4'. Preview of field's value: 'fourty-two']];
    nested: ElasticsearchException[Elasticsearch exception [type=illegal_argument_exception, reason=For input string: "fourty-two"]];
     */
    private static final Pattern exceptionPattern = Pattern
            .compile("^ElasticsearchException\\[Elasticsearch exception \\[type=([\\w_]+), (reason=(.+?)\\]\\];)?");

    abstract String type();
    abstract String reason();

    static ParsedElasticsearchException create(String type, String reason) {
        return new AutoValue_ParsedElasticsearchException(type, reason);
    }

    static ParsedElasticsearchException from(String s) {
        final Matcher matcher = exceptionPattern.matcher(s);
        if (matcher.matches()) {
            final MatchResult result = matcher.toMatchResult();
            final String type = result.group(1);
            final String reason = result.group(3);

            return create(type, reason);
        }

        throw new IllegalArgumentException("Unable to parse Elasticsearch exception: " + s);
    }
}
