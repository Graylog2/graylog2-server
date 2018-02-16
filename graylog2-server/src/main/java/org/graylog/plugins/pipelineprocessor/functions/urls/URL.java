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
package org.graylog.plugins.pipelineprocessor.functions.urls;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import okhttp3.HttpUrl;

import java.util.List;
import java.util.Map;

/**
 * This class simply delegates the safe methods to the {@link java.net.URL}.
 *
 * Specifically we want to disallow called {@link java.net.URL#getContent()} from a rule.
 */
public class URL {
    private final HttpUrl url;
    private Map<String, String> queryMap;

    public URL(java.net.URL url) {
        this.url = HttpUrl.get(url);
    }

    public URL(java.net.URI uri) {
        this.url = HttpUrl.get(uri);
    }

    public URL(String urlString) {
        url = HttpUrl.parse(urlString);
    }

    public String getQuery() {
        return url.encodedQuery();
    }

    public Map<String, String> getQueryParams() {
        if (queryMap == null) {
            final Map<String, String> queryMap = Maps.newHashMapWithExpectedSize(url.querySize());
            for(String name : url.queryParameterNames()) {
                final List<String> values = url.queryParameterValues(name);
                final String valueString = Joiner.on(',').join(values);
                queryMap.put(name, valueString);
            }
            this.queryMap = queryMap;
        }
        return queryMap;
    }

    public String getUserInfo() {
        final String username = url.encodedUsername();
        return username.isEmpty() ? "" : username + ':' + url.encodedPassword();
    }

    public String getHost() {
        return url.host();
    }

    public String getPath() {
        return url.encodedPath();
    }

    public String getFile() {
        return url.querySize() == 0 ? url.encodedPath() : url.encodedPath() + '?' + url.encodedQuery();
    }

    public String getProtocol() {
        return url.scheme();
    }

    public int getDefaultPort() {
        return url.port();
    }

    /**
     * alias for #getRef, fragment is more commonly used
     */
    public String getFragment() {
        return url.encodedFragment();
    }

    public String getRef() {
        return getFragment();
    }

    public String getAuthority() {
        final String userInfo = getUserInfo();
        return userInfo.isEmpty() ? getHost() + ':' + getPort() : getUserInfo() + '@' + getHost() + ':' + getPort();
    }

    public int getPort() {
        return url.port();
    }

    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Override
    public boolean equals(Object obj) {
        return url.equals(obj);
    }

    @Override
    public int hashCode() {
        return url.hashCode();
    }
}
