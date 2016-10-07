/**
 * This file is part of Graylog Pipeline Processor.
 *
 * Graylog Pipeline Processor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog Pipeline Processor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog Pipeline Processor.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.plugins.pipelineprocessor.functions.urls;

import com.google.common.collect.Maps;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.Map;

/**
 * This class simply delegates the safe methods to the java.net.URL.
 *
 * Specifically we want to disallow called java.net.URL#getContent from a rule.
 */
public class URL {

    private final java.net.URL url;
    private Map<String, String> queryMap;

    public URL(java.net.URL url) {
        this.url = url;
    }

    public URL(String urlString) throws MalformedURLException {
        url = URI.create(urlString).toURL();
    }

    public String getQuery() {
        return url.getQuery();
    }

    public Map<String, String> getQueryParams() {
        if (queryMap == null) {
            queryMap = splitQuery(getQuery());
        }
        return queryMap;
    }

    private static Map<String, String> splitQuery(String query) {
        final Map<String, String> nameValues = Maps.newHashMap();
        final String[] kvPairs = query.split("&");
        for (String pair : kvPairs) {
            try {
                final int idx = pair.indexOf("=");
                final String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), "UTF-8") : pair;
                final String value = idx > 0 && pair.length() > idx + 1 ? URLDecoder.decode(pair.substring(idx + 1),
                                                                                            "UTF-8") : null;
                if (nameValues.containsKey(key)) {
                    nameValues.put(key, nameValues.get(key) + "," + value); // TODO well, this is also crappy
                } else {
                    nameValues.put(key, value);
                }
            } catch (UnsupportedEncodingException ignored) {
                // ignored because UTF-8 is a required encoding
            }
        }
        return nameValues;
    }

    public String getUserInfo() {
        return url.getUserInfo();
    }

    public String getHost() {
        return url.getHost();
    }

    public String getPath() {
        return url.getPath();
    }

    public String getFile() {
        return url.getFile();
    }

    public String getProtocol() {
        return url.getProtocol();
    }

    public int getDefaultPort() {
        return url.getDefaultPort();
    }

    /**
     * alias for #getRef, fragment is more commonly used
     */
    public String getFragment() {
        return url.getRef();
    }

    public String getRef() {
        return url.getRef();
    }

    public String getAuthority() {
        return url.getAuthority();
    }

    public int getPort() {
        return url.getPort();
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
