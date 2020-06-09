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
package org.graylog2.indexer.cluster;

import com.github.zafarkhaja.semver.Version;
import org.graylog2.indexer.ElasticsearchException;

import javax.inject.Inject;
import java.util.Optional;

public class Node {
    private final NodeAdapter nodeAdapter;

    @Inject
    public Node(NodeAdapter nodeAdapter) {
        this.nodeAdapter = nodeAdapter;
    }

    public Optional<Version> getVersion() {
        return nodeAdapter.version()
            .map(this::parseVersion);
    }

    private Version parseVersion(String version) {
        try {
            return Version.valueOf(version);
        } catch (Exception e) {
            throw new ElasticsearchException("Unable to parse Elasticsearch version: " + version, e);
        }
    }
}
