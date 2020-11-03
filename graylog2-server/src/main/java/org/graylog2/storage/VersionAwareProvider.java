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

import org.graylog2.plugin.Version;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Map;

public class VersionAwareProvider<T> implements Provider<T> {
    private final Version elasticsearchMajorVersion;
    private final Map<Version, Provider<T>> pluginBindings;

    @Inject
    public VersionAwareProvider(@ElasticsearchVersion Version elasticsearchVersion, Map<Version, Provider<T>> pluginBindings) {
        this.elasticsearchMajorVersion = majorVersionFrom(elasticsearchVersion);
        this.pluginBindings = pluginBindings;
    }

    @Override
    public T get() {
        final Provider<T> provider = this.pluginBindings.get(elasticsearchMajorVersion);
        if (provider == null) {
            throw new UnsupportedElasticsearchException(elasticsearchMajorVersion);
        }
        return provider.get();
    }

    private Version majorVersionFrom(Version version) {
        return Version.from(version.getVersion().getMajorVersion(), 0, 0);
    }
}
