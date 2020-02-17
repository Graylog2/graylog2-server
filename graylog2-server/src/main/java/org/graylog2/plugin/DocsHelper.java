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
    PAGE_SENDING_IPFIXPATH("integrations/inputs/ipfix_input.html"),
    PAGE_ES_CONFIGURATION("configuration/elasticsearch.html"),
    PAGE_LDAP_TROUBLESHOOTING("users_and_roles/external_auth.html#troubleshooting");

    private static final String DOCS_URL = "http://docs.graylog.org/en/";

    private final String path;

    DocsHelper(String path) {
        this.path = path;
    }

    @Override
    public String toString() {
        final com.github.zafarkhaja.semver.Version version = Version.CURRENT_CLASSPATH.getVersion();
        final String shortVersion = version.getMajorVersion() + "." + version.getMinorVersion();

        return DOCS_URL + shortVersion + "/pages/" + path;
    }

    public String toLink(String title) {
        return "<a href=\"" + toString() + "\" target=\"_blank\">" + title + "</a>";
    }
}
