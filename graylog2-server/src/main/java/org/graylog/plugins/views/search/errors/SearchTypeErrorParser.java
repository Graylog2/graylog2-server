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
package org.graylog.plugins.views.search.errors;

import org.graylog.plugins.views.search.Query;
import org.graylog2.indexer.ElasticsearchException;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchTypeErrorParser {
    public static SearchTypeError parse(Query query, String searchTypeId, ElasticsearchException ex) {

        final Integer resultWindowLimit = parseResultLimit(ex);

        if (resultWindowLimit != null)
            return new ResultWindowLimitError(query, searchTypeId, resultWindowLimit, ex);

        return new SearchTypeError(query, searchTypeId, ex);
    }

    private static Integer parseResultLimit(Throwable throwable) {
        return parseResultLimit(throwable.getMessage());
    }

    private static Integer parseResultLimit(String description) {
        if (description.toLowerCase(Locale.US).contains("result window is too large")) {
            final Matcher matcher = Pattern.compile("[0-9]+").matcher(description);
            if (matcher.find())
                return Integer.parseInt(matcher.group(0));
        }
        return null;
    }

}
