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
package org.graylog2.contentpacks;

import org.graylog2.contentpacks.catalogs.CatalogIndex;
import org.graylog2.contentpacks.catalogs.DashboardCatalog;
import org.graylog2.contentpacks.catalogs.GrokPatternCatalog;
import org.graylog2.contentpacks.catalogs.InputCatalog;
import org.graylog2.contentpacks.catalogs.LookupCacheCatalog;
import org.graylog2.contentpacks.catalogs.LookupDataAdapterCatalog;
import org.graylog2.contentpacks.catalogs.LookupTableCatalog;
import org.graylog2.contentpacks.catalogs.OutputCatalog;
import org.graylog2.contentpacks.catalogs.StreamCatalog;
import org.graylog2.contentpacks.jersey.ModelIdParamConverter;
import org.graylog2.plugin.PluginModule;

public class ContentPacksModule extends PluginModule {

    @Override
    protected void configure() {
        bind(ContentPackPersistenceService.class).asEagerSingleton();
        bind(CatalogIndex.class).asEagerSingleton();

        jerseyAdditionalComponentsBinder().addBinding().toInstance(ModelIdParamConverter.Provider.class);

        addEntityCatalog(DashboardCatalog.TYPE, DashboardCatalog.class);
        addEntityCatalog(GrokPatternCatalog.TYPE, GrokPatternCatalog.class);
        addEntityCatalog(InputCatalog.TYPE, InputCatalog.class);
        addEntityCatalog(LookupCacheCatalog.TYPE, LookupCacheCatalog.class);
        addEntityCatalog(LookupDataAdapterCatalog.TYPE, LookupDataAdapterCatalog.class);
        addEntityCatalog(LookupTableCatalog.TYPE, LookupTableCatalog.class);
        addEntityCatalog(OutputCatalog.TYPE, OutputCatalog.class);
        addEntityCatalog(StreamCatalog.TYPE, StreamCatalog.class);
    }
}
