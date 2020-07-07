package org.graylog.storage.elasticsearch7;

import com.google.auto.value.AutoValue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@AutoValue
abstract class ParsedElasticsearchException {
    private static final Pattern exceptionPattern = Pattern
            .compile("ElasticsearchException\\[Elasticsearch exception \\[type=(?<type>[\\w_]+), (?:reason=(?<reason>.+?)\\]+;)");

    abstract String type();
    abstract String reason();

    static ParsedElasticsearchException create(String type, String reason) {
        return new AutoValue_ParsedElasticsearchException(type, reason);
    }

    static ParsedElasticsearchException from(String s) {
        final Matcher matcher = exceptionPattern.matcher(s);
        if (matcher.find()) {
            final String type = matcher.group("type");
            final String reason = matcher.group("reason");

            return create(type, reason);
        }

        throw new IllegalArgumentException("Unable to parse Elasticsearch exception: " + s);
    }
}
