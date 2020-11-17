/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.contentpacks;

import org.graylog2.contentpacks.constraints.GraylogVersionConstraintChecker;
import org.graylog2.contentpacks.constraints.PluginVersionConstraintChecker;
import org.graylog2.contentpacks.facades.DashboardFacade;
import org.graylog2.contentpacks.facades.GrokPatternFacade;
import org.graylog2.contentpacks.facades.InputFacade;
import org.graylog2.contentpacks.facades.LookupCacheFacade;
import org.graylog2.contentpacks.facades.LookupDataAdapterFacade;
import org.graylog2.contentpacks.facades.LookupTableFacade;
import org.graylog2.contentpacks.facades.OutputFacade;
import org.graylog2.contentpacks.facades.PipelineFacade;
import org.graylog2.contentpacks.facades.PipelineRuleFacade;
import org.graylog2.contentpacks.facades.RootEntityFacade;
import org.graylog2.contentpacks.facades.SearchFacade;
import org.graylog2.contentpacks.facades.SidecarCollectorConfigurationFacade;
import org.graylog2.contentpacks.facades.SidecarCollectorFacade;
import org.graylog2.contentpacks.facades.StreamFacade;
import org.graylog2.contentpacks.facades.UrlWhitelistFacade;
import org.graylog2.contentpacks.facades.dashboardV1.DashboardV1Facade;
import org.graylog2.contentpacks.jersey.ModelIdParamConverter;
import org.graylog2.contentpacks.model.entities.EventListEntity;
import org.graylog2.contentpacks.model.entities.MessageListEntity;
import org.graylog2.contentpacks.model.entities.PivotEntity;
import org.graylog2.plugin.PluginModule;

public class ContentPacksModule extends PluginModule {

    @Override
    protected void configure() {
        bind(ContentPackPersistenceService.class).asEagerSingleton();
        bind(ContentPackService.class).asEagerSingleton();

        jerseyAdditionalComponentsBinder().addBinding().toInstance(ModelIdParamConverter.Provider.class);

        addEntityFacade(SidecarCollectorConfigurationFacade.TYPE_V1, SidecarCollectorConfigurationFacade.class);
        addEntityFacade(SidecarCollectorFacade.TYPE_V1, SidecarCollectorFacade.class);
        addEntityFacade(GrokPatternFacade.TYPE_V1, GrokPatternFacade.class);
        addEntityFacade(InputFacade.TYPE_V1, InputFacade.class);
        addEntityFacade(LookupCacheFacade.TYPE_V1, LookupCacheFacade.class);
        addEntityFacade(LookupDataAdapterFacade.TYPE_V1, LookupDataAdapterFacade.class);
        addEntityFacade(LookupTableFacade.TYPE_V1, LookupTableFacade.class);
        addEntityFacade(OutputFacade.TYPE_V1, OutputFacade.class);
        addEntityFacade(PipelineFacade.TYPE_V1, PipelineFacade.class);
        addEntityFacade(PipelineRuleFacade.TYPE_V1, PipelineRuleFacade.class);
        addEntityFacade(RootEntityFacade.TYPE, RootEntityFacade.class);
        addEntityFacade(StreamFacade.TYPE_V1, StreamFacade.class);
        addEntityFacade(DashboardFacade.TYPE_V2, DashboardFacade.class);
        addEntityFacade(DashboardV1Facade.TYPE_V1, DashboardV1Facade.class);
        addEntityFacade(SearchFacade.TYPE_V1, SearchFacade.class);
        addEntityFacade(UrlWhitelistFacade.TYPE_V1, UrlWhitelistFacade.class);

        addConstraintChecker(GraylogVersionConstraintChecker.class);
        addConstraintChecker(PluginVersionConstraintChecker.class);

        registerJacksonSubtype(MessageListEntity.class);
        registerJacksonSubtype(PivotEntity.class);
        registerJacksonSubtype(EventListEntity.class);
    }
}
