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
package org.graylog.plugins.map;

import org.graylog.plugins.map.geoip.MaxmindDataAdapter;
import org.graylog.plugins.map.geoip.processor.GeoIpProcessor;
import org.graylog.plugins.map.widget.strategy.MapWidgetStrategy;
import org.graylog2.plugin.PluginModule;

public class MapWidgetModule extends PluginModule {
    @Override
    protected void configure() {
        addMessageProcessor(GeoIpProcessor.class, GeoIpProcessor.Descriptor.class);
        addWidgetStrategy(MapWidgetStrategy.class, MapWidgetStrategy.Factory.class);
        registerRestControllerPackage(getClass().getPackage().getName());

        installLookupDataAdapter(MaxmindDataAdapter.NAME,
                MaxmindDataAdapter.class,
                MaxmindDataAdapter.Factory.class,
                MaxmindDataAdapter.Config.class);
    }
}
