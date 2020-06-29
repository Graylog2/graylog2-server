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
import javax.inject.Named;
import javax.inject.Provider;
import java.util.Map;

public class VersionAwareProvider<T> implements Provider<T> {
    private final Version elasticsearchVersion;
    private final String elasticsearchMajorVersion;
    private final Map<Version, Provider<T>> pluginBindings;

    @Inject
    public VersionAwareProvider(@Named("elasticsearch_version") String elasticsearchMajorVersion, Map<Version, Provider<T>> pluginBindings) {
        this.elasticsearchVersion = constructElasticsearchVersion(elasticsearchMajorVersion);
        this.elasticsearchMajorVersion = elasticsearchMajorVersion;
        this.pluginBindings = pluginBindings;
    }

    private static Version constructElasticsearchVersion(String elasticsearchVersion) {
        final int majorVersion = Integer.parseInt(elasticsearchVersion);
        return Version.from(majorVersion, 0, 0);
    }

    @Override
    public T get() {
        final Provider<T> provider = this.pluginBindings.get(elasticsearchVersion);
        if (provider == null) {
            throw new IllegalStateException("Incomplete Elasticsearch implementation for version \"" + elasticsearchMajorVersion + "\".");
        }
        return provider.get();
    }
}
