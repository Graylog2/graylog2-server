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
package org.graylog2.lookup;

import com.google.inject.Scopes;
import com.google.inject.multibindings.MapBinder;
import org.graylog2.Configuration;
import org.graylog2.lookup.adapters.CSVFileDataAdapter;
import org.graylog2.lookup.adapters.DSVHTTPDataAdapter;
import org.graylog2.lookup.adapters.DnsLookupDataAdapter;
import org.graylog2.lookup.adapters.HTTPJSONPathDataAdapter;
import org.graylog2.lookup.caches.CaffeineLookupCache;
import org.graylog2.lookup.caches.NullCache;
import org.graylog2.lookup.db.DBLookupTableConfigService;
import org.graylog2.plugin.inject.Graylog2Module;
import org.graylog2.plugin.lookup.LookupDataAdapter;
import org.graylog2.system.SystemEntity;
import org.graylog2.system.urlwhitelist.UrlWhitelistNotificationService;
import org.graylog2.system.urlwhitelist.UrlWhitelistService;

public class LookupModule extends Graylog2Module {
    private final Configuration configuration;

    public LookupModule(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    protected void configure() {
        serviceBinder().addBinding().to(UrlWhitelistService.class).in(Scopes.SINGLETON);
        binder().bind(UrlWhitelistNotificationService.class).in(Scopes.SINGLETON);
        binder().bind(AllowedAuxiliaryPathChecker.class).in(Scopes.SINGLETON);

        bind(LookupTableConfigService.class).to(DBLookupTableConfigService.class).asEagerSingleton();
        // Triggering map binder once, so it does not break injection when no instance is bound.
        MapBinder.newMapBinder(binder(), String.class, LookupDataAdapter.Factory2.class, SystemEntity.class);
        serviceBinder().addBinding().to(LookupTableService.class).asEagerSingleton();

        installLookupCache(NullCache.NAME,
                NullCache.class,
                NullCache.Factory.class,
                NullCache.Config.class);

        installLookupCache(CaffeineLookupCache.NAME,
                CaffeineLookupCache.class,
                CaffeineLookupCache.Factory.class,
                CaffeineLookupCache.Config.class);

        installLookupDataAdapter(CSVFileDataAdapter.NAME,
                CSVFileDataAdapter.class,
                CSVFileDataAdapter.Factory.class,
                CSVFileDataAdapter.Config.class);

        installLookupDataAdapter2(DnsLookupDataAdapter.NAME,
                DnsLookupDataAdapter.class,
                DnsLookupDataAdapter.Factory.class,
                DnsLookupDataAdapter.Config.class);

        installLookupDataAdapter2(HTTPJSONPathDataAdapter.NAME,
                HTTPJSONPathDataAdapter.class,
                HTTPJSONPathDataAdapter.Factory.class,
                HTTPJSONPathDataAdapter.Config.class);

        installLookupDataAdapter(DSVHTTPDataAdapter.NAME,
                DSVHTTPDataAdapter.class,
                DSVHTTPDataAdapter.Factory.class,
                DSVHTTPDataAdapter.Config.class);
    }

}
