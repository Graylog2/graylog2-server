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
package org.graylog2;

import com.github.joschi.nosqlunit.elasticsearch.http.ElasticsearchOperation;
import com.lordofthejars.nosqlunit.core.AbstractNoSqlTestRule;
import com.lordofthejars.nosqlunit.core.DatabaseOperation;
import io.searchbox.client.JestClient;
import java.util.Collections;
import java.util.Set;

public class ElasticsearchRule extends AbstractNoSqlTestRule {

    private static final String EXTENSION = "json";

    private DatabaseOperation<? extends JestClient> databaseOperation;

    public static Builder newElasticsearchRule() {
        return new Builder();
    }

    public static class Builder {

        private ElasticsearchConfiguration elasticsearchConfiguration;
        private Object target;

        private Builder() {
        }

        /**
         * Configure the {@link ElasticsearchRule} with the given configuration.
         *
         * @param elasticsearchConfiguration The configuration for the {@link ElasticsearchRule}
         */
        public Builder configure(ElasticsearchConfiguration elasticsearchConfiguration) {
            this.elasticsearchConfiguration = elasticsearchConfiguration;
            return this;
        }

        public Builder unitInstance(Object target) {
            this.target = target;
            return this;
        }

        /**
         * Build an {@link ElasticsearchRule} with the default configuration (connecting to {@literal http://localhost:9200}).
         *
         * @return An {@link ElasticsearchRule} using the default configuration
         */
        public ElasticsearchRule remoteElasticsearch() {
            return new ElasticsearchRule(ElasticsearchConfiguration.remoteElasticsearch().build());
        }

        /**
         * Build an {@link ElasticsearchRule} with the default configuration (connecting to the given Elasticsearch node).
         *
         * @return An {@link ElasticsearchRule} using the default configuration
         */
        public ElasticsearchRule remoteElasticsearch(String server) {
            return remoteElasticsearch(Collections.singleton(server));
        }

        /**
         * Build an {@link ElasticsearchRule} with the default configuration (connecting to the given Elasticsearch nodes).
         *
         * @return An {@link ElasticsearchRule} using the default configuration
         */
        public ElasticsearchRule remoteElasticsearch(Set<String> servers) {
            return new ElasticsearchRule(ElasticsearchConfiguration.remoteElasticsearch(servers).build());
        }

        /**
         * Build an {@link ElasticsearchRule} with the given configuration.
         *
         * @return An {@link ElasticsearchRule} using the given configuration
         * @see #configure(ElasticsearchConfiguration)
         */
        public ElasticsearchRule build() {

            if (this.elasticsearchConfiguration == null) {
                throw new IllegalArgumentException("Configuration object should be provided.");
            }

            return new ElasticsearchRule(elasticsearchConfiguration, target);
        }

    }

    public ElasticsearchRule(ElasticsearchConfiguration elasticsearchConfiguration) {
        super(elasticsearchConfiguration.getConnectionIdentifier());
        this.databaseOperation = new ElasticsearchOperation(
                elasticsearchConfiguration.getClient(),
                elasticsearchConfiguration.isDeleteAllIndices(),
                elasticsearchConfiguration.isCreateIndices(),
                elasticsearchConfiguration.getIndexSettings(),
                elasticsearchConfiguration.getIndexTemplates()
        );
    }

    /*With JUnit 10 is impossible to get target from a Rule, it seems that future versions will support it. For now constructor is apporach is the only way.*/
    public ElasticsearchRule(ElasticsearchConfiguration elasticsearchConfiguration, Object target) {
        this(elasticsearchConfiguration);
        setTarget(target);
    }

    @Override
    public DatabaseOperation getDatabaseOperation() {
        return this.databaseOperation;
    }

    @Override
    public String getWorkingExtension() {
        return EXTENSION;
    }

    @Override
    public void close() {
        this.databaseOperation.connectionManager().shutdownClient();
    }
}
