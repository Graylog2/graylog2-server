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

import com.google.inject.Binder;
import com.google.inject.Module;
import org.graylog2.Configuration;
import org.graylog2.plugin.BaseConfiguration;

import static java.util.Objects.requireNonNull;

public class ConfigurationModule implements Module {
    private final Configuration configuration;

    public ConfigurationModule(Configuration configuration) {
        this.configuration = requireNonNull(configuration);
    }

    @Override
    public void configure(Binder binder) {
        binder.bind(Configuration.class).toInstance(configuration);
        binder.bind(BaseConfiguration.class).toInstance(configuration);
    }
}
