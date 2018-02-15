package org.graylog.plugins.enterprise.search.elasticsearch;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import org.graylog.plugins.enterprise.search.QueryInfo;
import org.graylog.plugins.enterprise.search.QueryParameter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QueryStringParser {
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$(.+?)\\$");

    public QueryInfo parse(String queryString) {
        if (Strings.isNullOrEmpty(queryString)) {
            return QueryInfo.empty();
        }
        final Matcher matcher = PLACEHOLDER_PATTERN.matcher(queryString);
        ImmutableMap.Builder<String, QueryParameter> parameters = ImmutableMap.builder();
        while (matcher.find()) {
            final String parameter = matcher.group(1);
            parameters.put(parameter, QueryParameter.any(parameter));
        }
        return QueryInfo.builder().parameters(parameters.build()).build();
    }
}
