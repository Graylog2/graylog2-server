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

import com.google.inject.assistedinject.FactoryModuleBuilder;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.indexer.indexset.MongoIndexSetService;
import org.graylog2.indexer.results.ScrollResult;
import org.graylog2.plugin.inject.Graylog2Module;

public class IndexerBindings extends Graylog2Module {
    @Override
    protected void configure() {
        bind(IndexSetService.class).to(MongoIndexSetService.class);

        install(new FactoryModuleBuilder().build(MongoIndexSet.Factory.class));
        bind(IndexSetRegistry.class).to(MongoIndexSetRegistry.class);

        install(new FactoryModuleBuilder().build(ScrollResult.Factory.class));
    }
}
