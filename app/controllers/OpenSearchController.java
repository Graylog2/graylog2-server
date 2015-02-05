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
package controllers;

import play.mvc.Result;

public class OpenSearchController extends BaseController  {

    public static Result index() {
        final String relative = routes.SearchController.index("{searchTerms}", "relative", 3600, "", "", "", "", 1, "", "", "", "", -1).absoluteURL(request());
        final String unescaped = relative.replaceAll("%7B", "{").replaceAll("%7D", "}").replaceAll("&", "&amp;");
        String content =
                "<OpenSearchDescription xmlns=\"http://a9.com/-/spec/opensearch/1.1/\"\n" +
                "                       xmlns:moz=\"http://www.mozilla.org/2006/browser/search/\">\n" +
                "    <ShortName>Graylog</ShortName>\n" +
                "    <Description>Search Graylog (last hour)</Description>\n" +
                "    <InputEncoding>UTF-8</InputEncoding>\n" +
                "    <Url type=\"text/html\" method=\"get\" template=\""+ unescaped +"\"/>\n" +
                "    <moz:SearchForm>"+unescaped+"</moz:SearchForm>\n" +
                "</OpenSearchDescription>\n";

        //                "    <!-- Image width=\"16\" height=\"16\" type=\"image/x-icon\">/favicon.ico</Image -->\n" +

        return ok(content);
    }
}
