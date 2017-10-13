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
package org.graylog2.indexer;

import com.github.zafarkhaja.semver.Version;
import org.graylog2.indexer.cluster.Node;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class IndexMappingFactory {
    private final Node node;

    @Inject
    public IndexMappingFactory(Node node) {
        this.node = node;
    }

    public IndexMapping createIndexMapping() {
        final Version elasticsearchVersion = node.getVersion().orElseThrow(() -> new ElasticsearchException("Unable to retrieve Elasticsearch version."));
        if (elasticsearchVersion.satisfies("^5.0.0")) {
            return new IndexMapping5();
        } else {
            throw new ElasticsearchException("Unsupported Elasticsearch version: " + elasticsearchVersion);
        }
    }
}
