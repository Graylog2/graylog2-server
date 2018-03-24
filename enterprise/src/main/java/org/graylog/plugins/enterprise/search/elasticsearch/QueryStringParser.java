package org.graylog.plugins.enterprise.search.elasticsearch;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import org.graylog.plugins.enterprise.search.Parameter;
import org.graylog.plugins.enterprise.search.QueryMetadata;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QueryStringParser {
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$(.+?)\\$");

    public QueryMetadata parse(String queryString) {
        if (Strings.isNullOrEmpty(queryString)) {
            return QueryMetadata.empty();
        }
        final Matcher matcher = PLACEHOLDER_PATTERN.matcher(queryString);
        ImmutableMap.Builder<String, Parameter> parameters = ImmutableMap.builder();
        while (matcher.find()) {
            final String name = matcher.group(1);
            parameters.put(name, Parameter.any(name));
        }
        return QueryMetadata.builder().parameters(parameters.build()).build();
    }
}
