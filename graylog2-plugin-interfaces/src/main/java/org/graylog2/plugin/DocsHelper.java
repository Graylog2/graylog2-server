/**
 * The MIT License
 * Copyright (c) 2012 Graylog, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
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

package org.graylog2.plugin;

public enum DocsHelper {
    PAGE_SENDING_JSONPATH("sending_data.html#json-path-from-http-api-input"),
    PAGE_ES_CONFIGURATION("configuring_es.html");

    public static String DOCS_URL = "http://docs.graylog.org/en/";
    public static final String HELP_DOCS = "http://docs.graylog.org/";
    public static final String HELP_COMMUNITY = "https://www.graylog.org/community-support/";
    public static final String HELP_COMMERCIAL = "https://www.graylog.com/support/";

    private final String path;

    DocsHelper(String path) {
        this.path = path;
    }

    @Override
    public String toString() {
        final String version = Version.CURRENT_CLASSPATH.major + "." + Version.CURRENT_CLASSPATH.minor;

        final StringBuffer sb = new StringBuffer(DOCS_URL)
                .append(version)
                .append("/pages/")
                .append(path);

        return sb.toString();
    }

    public String toLink(String title) {
        final StringBuffer sb = new StringBuffer("<a href=\"")
                .append(toString())
                .append("\" target=\"_blank\">")
                .append(title)
                .append("</a>");

        return sb.toString();
    }
}
