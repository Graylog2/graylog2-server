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
package org.graylog2.bindings.providers;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.graylog2.Configuration;
import org.graylog2.plugin.indexer.rotation.RotationStrategy;

import java.util.Map;

public class RotationStrategyProvider implements Provider<RotationStrategy> {

    private final Map<String, RotationStrategy> strategies;
    private final String rotationStrategy;

    @Inject
    public RotationStrategyProvider(Map<String, RotationStrategy> strategies, Configuration configuration) {
        this.strategies = strategies;
        rotationStrategy = configuration.getRotationStrategy();
    }

    @Override
    public RotationStrategy get() {
        final RotationStrategy strategy = strategies.get(rotationStrategy);
        if (strategy == null) {
            return strategies.get("count"); // TODO how can we do this in a more flexible way?
        }
        return strategy;
    }
}
