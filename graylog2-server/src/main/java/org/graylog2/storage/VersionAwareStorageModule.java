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
package org.graylog2.storage;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import org.graylog.events.search.MoreSearchAdapter;
import org.graylog.plugins.views.search.engine.GeneratedQueryContext;
import org.graylog.plugins.views.search.engine.QueryBackend;
import org.graylog2.indexer.IndexToolsAdapter;
import org.graylog2.indexer.cluster.ClusterAdapter;
import org.graylog2.indexer.cluster.NodeAdapter;
import org.graylog2.indexer.counts.CountsAdapter;
import org.graylog2.indexer.fieldtypes.IndexFieldTypePollerAdapter;
import org.graylog2.indexer.indices.IndicesAdapter;
import org.graylog2.indexer.messages.MessagesAdapter;
import org.graylog2.indexer.searches.SearchesAdapter;
import org.graylog2.migrations.V20170607164210_MigrateReopenedIndicesToAliases;
import org.graylog2.storage.providers.ClusterAdapterProvider;
import org.graylog2.storage.providers.CountsAdapterProvider;
import org.graylog2.storage.providers.ElasticsearchBackendProvider;
import org.graylog2.storage.providers.IndexFieldTypePollerAdapterProvider;
import org.graylog2.storage.providers.IndexToolsAdapterProvider;
import org.graylog2.storage.providers.IndicesAdapterProvider;
import org.graylog2.storage.providers.MessagesAdapterProvider;
import org.graylog2.storage.providers.MoreSearchAdapterProvider;
import org.graylog2.storage.providers.NodeAdapterProvider;
import org.graylog2.storage.providers.SearchesAdapterProvider;
import org.graylog2.storage.providers.V20170607164210_MigrateReopenedIndicesToAliasesClusterStateAdapterProvider;

public class VersionAwareStorageModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(CountsAdapter.class).toProvider(CountsAdapterProvider.class);
        bind(IndicesAdapter.class).toProvider(IndicesAdapterProvider.class);
        bind(SearchesAdapter.class).toProvider(SearchesAdapterProvider.class);
        bind(MoreSearchAdapter.class).toProvider(MoreSearchAdapterProvider.class);
        bind(MessagesAdapter.class).toProvider(MessagesAdapterProvider.class);
        bind(ClusterAdapter.class).toProvider(ClusterAdapterProvider.class);
        bind(NodeAdapter.class).toProvider(NodeAdapterProvider.class);
        bind(IndexFieldTypePollerAdapter.class).toProvider(IndexFieldTypePollerAdapterProvider.class);
        bind(IndexToolsAdapter.class).toProvider(IndexToolsAdapterProvider.class);
        bind(V20170607164210_MigrateReopenedIndicesToAliases.ClusterState.class).toProvider(V20170607164210_MigrateReopenedIndicesToAliasesClusterStateAdapterProvider.class);
        bind(new TypeLiteral<QueryBackend<? extends GeneratedQueryContext>>() {}).toProvider(ElasticsearchBackendProvider.class);
    }
}
