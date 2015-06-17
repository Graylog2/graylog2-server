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

package org.graylog2.plugin;

public enum DocsHelper {
    PAGE_SENDING_JSONPATH("sending_data.html#json-path-from-http-api-input"),
    PAGE_ES_CONFIGURATION("configuring_es.html");

    private static final String DOCS_URL = "http://docs.graylog.org/en/";

    private final String path;

    DocsHelper(String path) {
        this.path = path;
    }

    @Override
    public String toString() {
        final String version = Version.CURRENT_CLASSPATH.major + "." + Version.CURRENT_CLASSPATH.minor;

        return DOCS_URL + version + "/pages/" + path;
    }

    public String toLink(String title) {
        return "<a href=\"" + toString() + "\" target=\"_blank\">" + title + "</a>";
    }
}
