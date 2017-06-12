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
import org.graylog2.indexer.IndexMappingFactory;

public class ElasticsearchModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(Gson.class).toInstance(new GsonBuilder().create());
        bind(JestClient.class).toProvider(JestClientProvider.class).asEagerSingleton();
        bind(IndexMappingFactory.class).asEagerSingleton();
    }
}
