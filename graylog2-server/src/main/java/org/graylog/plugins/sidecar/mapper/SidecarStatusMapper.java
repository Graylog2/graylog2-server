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
package org.graylog.plugins.sidecar.mapper;

import org.graylog.plugins.sidecar.rest.models.Sidecar;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.CASE_INSENSITIVE;

public class SidecarStatusMapper {
    private static final Pattern searchQueryStatusRegex = Pattern.compile("\\bstatus:(running|failing|unknown)\\b", CASE_INSENSITIVE);

    /**
     * Replaces status strings in search query with their number representations,
     * e.g. <code>status:running</code> will be transformed into <code>status:0</code>.
     *
     * @param query Search query that may contain one or more status strings
     * @return Search query with all status strings replaced with status codes
     */
    public String replaceStringStatusSearchQuery(String query) {
        final Matcher matcher = searchQueryStatusRegex.matcher(query);
        final StringBuffer stringBuffer = new StringBuffer();
        while(matcher.find()) {
            final String status = matcher.group(1);
            matcher.appendReplacement(stringBuffer, "status:" + Sidecar.Status.fromString(status).getStatusCode());
        }
        matcher.appendTail(stringBuffer);
        return stringBuffer.toString();
    }
}
