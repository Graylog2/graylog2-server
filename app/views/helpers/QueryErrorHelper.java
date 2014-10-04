/*
 * Copyright 2014 TORCH UG
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package views.helpers;

import org.graylog2.restclient.models.api.responses.SearchResultResponse;
import org.graylog2.restclient.models.api.results.SearchResult;
import play.twirl.api.HtmlFormat;

public class QueryErrorHelper {

    public static String markupOriginalQuery(SearchResult response) {
        final String query = response.getOriginalQuery();
        if (!(response.getError() instanceof SearchResultResponse.ParseError)) {
            return query;
        }
        final SearchResultResponse.ParseError error = (SearchResultResponse.ParseError) response.getError();

        // TODO we don't highlight multiline queries yet
        if (error.beginLine > 1 || error.beginLine != error.endLine) {
            return query;
        }
        final int beginColumn = error.beginColumn;
        final int endColumn = error.endColumn;

        return HtmlFormat.escape(query.substring(0, beginColumn))
                + "<span class=\"parse-error\">" + HtmlFormat.escape(query.substring(beginColumn, endColumn)) + "</span>"
                + HtmlFormat.escape(query.substring(endColumn, query.length()));
    }

    public static boolean canMarkupParseError(SearchResultResponse.QueryError error) {
        return error instanceof SearchResultResponse.ParseError;
    }

    public static boolean isGenericError(SearchResultResponse.QueryError error) {
        return error instanceof SearchResultResponse.GenericError;
    }


}
