/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.bindings;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;
import org.graylog2.indexer.rotation.MessageCountRotationStrategy;
import org.graylog2.indexer.rotation.SizeBasedRotationStrategy;
import org.graylog2.indexer.rotation.TimeBasedRotationStrategy;
import org.graylog2.plugin.indexer.rotation.RotationStrategy;

public class RotationStrategyBindings extends AbstractModule {
    @Override
    protected void configure() {
        final MapBinder<String, RotationStrategy> mapBinder = MapBinder.newMapBinder(binder(),
                                                                                     String.class,
                                                                                     RotationStrategy.class);
        mapBinder.addBinding("count").to(MessageCountRotationStrategy.class);
        mapBinder.addBinding("size").to(SizeBasedRotationStrategy.class);
        mapBinder.addBinding("time").to(TimeBasedRotationStrategy.class);
    }

}