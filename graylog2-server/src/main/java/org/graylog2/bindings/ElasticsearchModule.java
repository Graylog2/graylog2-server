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
package org.graylog2.bindings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.AbstractModule;
import io.searchbox.client.JestClient;
import org.graylog2.bindings.providers.JestClientProvider;
import org.graylog2.configuration.ElasticsearchClientConfiguration;
import org.graylog2.indexer.IndexMapping;
import org.graylog2.indexer.IndexMapping2;
import org.graylog2.indexer.IndexMapping5;

public class ElasticsearchModule extends AbstractModule {
    private final ElasticsearchClientConfiguration elasticsearchClientConfiguration;

    public ElasticsearchModule(ElasticsearchClientConfiguration elasticsearchClientConfiguration) {
        this.elasticsearchClientConfiguration = elasticsearchClientConfiguration;
    }

    @Override
    protected void configure() {
        bind(Gson.class).toInstance(new GsonBuilder().create());
        bind(JestClient.class).toProvider(JestClientProvider.class).asEagerSingleton();

        switch (elasticsearchClientConfiguration.getVersion()) {
            case 2:
                bind(IndexMapping.class).to(IndexMapping2.class).asEagerSingleton();
                break;
            case 5:
                bind(IndexMapping.class).to(IndexMapping5.class).asEagerSingleton();
                break;
            default:
                throw new IllegalArgumentException("Invalid Elasticsearch version: " + elasticsearchClientConfiguration.getVersion());
        }
    }
}
