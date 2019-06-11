package org.graylog.plugins.views.search.elasticsearch;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.graylog.plugins.views.search.QueryMetadata;
import org.graylog.plugins.views.search.QueryMetadata;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QueryStringParser {
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$(.+?)\\$");

    public QueryMetadata parse(String queryString) {
        if (Strings.isNullOrEmpty(queryString)) {
            return QueryMetadata.empty();
        }
        final Matcher matcher = PLACEHOLDER_PATTERN.matcher(queryString);
        Set<String> paramNames = Sets.newHashSet();
        while (matcher.find()) {
            final String name = matcher.group(1);
            paramNames.add(name);
        }
        return QueryMetadata.builder().usedParameterNames(ImmutableSet.copyOf(paramNames)).build();
    }
}
