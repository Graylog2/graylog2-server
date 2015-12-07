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
package org.graylog2.indexer.esplugin;

import com.google.common.collect.ImmutableSet;
import org.elasticsearch.common.component.LifecycleComponent;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.plugins.Plugin;

import java.util.Collection;

public class MonitorPlugin extends Plugin {
    @Override
    public String name() {
        return "graylog-monitor";
    }

    @Override
    public String description() {
        return "Monitors the current cluster state.";
    }

    @Override
    public Collection<Class<? extends LifecycleComponent>> nodeServices() {
        return ImmutableSet.<Class<? extends LifecycleComponent>>of(
                ClusterStateMonitor.class,
                IndexChangeMonitor.class);
    }

    @Override
    public Collection<Module> nodeModules() {
        return ImmutableSet.<Module>of(new MonitorModule());
    }
}
