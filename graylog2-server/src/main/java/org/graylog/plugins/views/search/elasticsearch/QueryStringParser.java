/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
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
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$([a-zA-Z_]\\w*)\\$");

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
