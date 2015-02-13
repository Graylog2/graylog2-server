/*
 * Copyright 2012-2015 TORCH GmbH, 2015 Graylog, Inc.
 *
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
package views.helpers;

import org.graylog2.restclient.models.api.responses.QueryParseError;
import org.graylog2.restclient.models.api.responses.SearchResultResponse;
import org.graylog2.restclient.models.api.results.SearchResult;
import play.twirl.api.HtmlFormat;

import static com.google.common.base.MoreObjects.firstNonNull;

public class QueryErrorHelper {

    public static String markupOriginalQuery(QueryParseError error) {
        final String query = error.query;
        if (isGenericError(error)) {
            return query;
        }

        // TODO we don't highlight multiline queries yet
        if (error.beginLine != null && (error.beginLine > 1 || !error.beginLine.equals(error.endLine))) {
            return query;
        }
        final int beginColumn = firstNonNull(error.beginColumn, 1);
        final int endColumn = firstNonNull(error.endColumn, 1);

        return HtmlFormat.escape(query.substring(0, beginColumn))
                + "<span class=\"parse-error\">" + HtmlFormat.escape(query.substring(beginColumn, endColumn)) + "</span>"
                + HtmlFormat.escape(query.substring(endColumn, query.length()));
    }

    public static boolean canMarkupParseError(QueryParseError error) {
        return error.beginColumn != null
                && error.beginLine != null
                && error.endColumn != null
                && error.endLine != null;
    }

    public static boolean isGenericError(QueryParseError error) {
        return !canMarkupParseError(error);
    }


}
