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
import org.graylog2.indexer.indexset.IndexSetConfig;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class IndexMappingFactory {
    private final Node node;

    @Inject
    public IndexMappingFactory(Node node) {
        this.node = node;
    }

    public IndexMappingTemplate createIndexMapping(IndexSetConfig.TemplateType templateType) {
        final Version elasticsearchVersion = node.getVersion().orElseThrow(() -> new ElasticsearchException("Unable to retrieve Elasticsearch version."));

        if (IndexSetConfig.TemplateType.EVENTS.equals(templateType)) {
            return new EventsIndexMapping(elasticsearchVersion);
        }

        return indexMappingFor(elasticsearchVersion);
    }

    public static IndexMapping indexMappingFor(Version elasticsearchVersion) {
        if (elasticsearchVersion.satisfies("^5.0.0")) {
            return new IndexMapping5();
        } else if (elasticsearchVersion.satisfies("^6.0.0")) {
            return new IndexMapping6();
        } else if (elasticsearchVersion.satisfies("^7.0.0")) {
            return new IndexMapping7();
        } else {
            throw new ElasticsearchException("Unsupported Elasticsearch version: " + elasticsearchVersion);
        }
    }
}
