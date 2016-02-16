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
package org.elasticsearch.node;

import org.elasticsearch.Version;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.internal.InternalSettingsPreparer;
import org.elasticsearch.plugins.Plugin;

import java.util.Collection;

/**
 * A custom Elasticsearch {@link Node} which allows adding plugins from the classpath.
 */
public final class GraylogNode extends Node {
    /**
     * Constructs a node with the given settings.
     *
     * @param preparedSettings Base settings to configure the node with
     * @param classpathPlugins A list of classpath plugins to load on startup
     */
    public GraylogNode(Settings preparedSettings, Collection<Class<? extends Plugin>> classpathPlugins) {
        super(InternalSettingsPreparer.prepareEnvironment(preparedSettings, null), Version.CURRENT, classpathPlugins);
    }
}
