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

import models.api.results.SearchResult;
import play.api.templates.HtmlFormat;

public class QueryErrorHelper {

    public static String markupOriginalQuery(SearchResult response) {
        final String query = response.getOriginalQuery();
        // TODO we don't highlight multiline queries yet
        if (response.getError().beginLine > 1 || response.getError().beginLine != response.getError().endLine) {
            return query;
        }
        final int beginColumn = response.getError().beginColumn;
        final int endColumn = response.getError().endColumn;

        return HtmlFormat.escape(query.substring(0, beginColumn))
                + "<span class=\"parse-error\">" + HtmlFormat.escape(query.substring(beginColumn, endColumn)) + "</span>"
                + HtmlFormat.escape(query.substring(endColumn, query.length()));
    }
}
