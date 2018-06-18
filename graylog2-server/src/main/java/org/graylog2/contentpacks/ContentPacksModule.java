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
import org.graylog2.contentpacks.constraints.GraylogVersionConstraintChecker;
import org.graylog2.contentpacks.facades.CollectorFacade;
import org.graylog2.contentpacks.facades.DashboardFacade;
import org.graylog2.contentpacks.facades.GrokPatternFacade;
import org.graylog2.contentpacks.facades.InputFacade;
import org.graylog2.contentpacks.facades.LookupCacheFacade;
import org.graylog2.contentpacks.facades.LookupDataAdapterFacade;
import org.graylog2.contentpacks.facades.LookupTableFacade;
import org.graylog2.contentpacks.facades.OutputFacade;
import org.graylog2.contentpacks.facades.PipelineFacade;
import org.graylog2.contentpacks.facades.PipelineRuleFacade;
import org.graylog2.contentpacks.facades.CollectorConfigurationFacade;
import org.graylog2.contentpacks.facades.StreamFacade;
import org.graylog2.contentpacks.jersey.ModelIdParamConverter;
import org.graylog2.plugin.PluginModule;

public class ContentPacksModule extends PluginModule {

    @Override
    protected void configure() {
        bind(ContentPackPersistenceService.class).asEagerSingleton();
        bind(ContentPackService.class).asEagerSingleton();
        bind(CatalogIndex.class).asEagerSingleton();

        jerseyAdditionalComponentsBinder().addBinding().toInstance(ModelIdParamConverter.Provider.class);

        addEntityFacade(CollectorConfigurationFacade.TYPE, CollectorConfigurationFacade.class);
        addEntityFacade(CollectorFacade.TYPE, CollectorFacade.class);
        addEntityFacade(DashboardFacade.TYPE, DashboardFacade.class);
        addEntityFacade(GrokPatternFacade.TYPE, GrokPatternFacade.class);
        addEntityFacade(InputFacade.TYPE, InputFacade.class);
        addEntityFacade(LookupCacheFacade.TYPE, LookupCacheFacade.class);
        addEntityFacade(LookupDataAdapterFacade.TYPE, LookupDataAdapterFacade.class);
        addEntityFacade(LookupTableFacade.TYPE, LookupTableFacade.class);
        addEntityFacade(OutputFacade.TYPE, OutputFacade.class);
        addEntityFacade(PipelineFacade.TYPE, PipelineFacade.class);
        addEntityFacade(PipelineRuleFacade.TYPE, PipelineRuleFacade.class);
        addEntityFacade(StreamFacade.TYPE, StreamFacade.class);

        addConstraintChecker(GraylogVersionConstraintChecker.class);
    }
}
