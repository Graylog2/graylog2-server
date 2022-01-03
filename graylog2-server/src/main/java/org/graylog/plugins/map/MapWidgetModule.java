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
package org.graylog.plugins.map;

import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Names;
import org.graylog.plugins.map.geoip.GeoIpResolver;
import org.graylog.plugins.map.geoip.GeoIpResolverFactory;
import org.graylog.plugins.map.geoip.IpInfoIpAsnResolver;
import org.graylog.plugins.map.geoip.IpInfoLocationResolver;
import org.graylog.plugins.map.geoip.MaxMindIpAsnResolver;
import org.graylog.plugins.map.geoip.MaxMindIpLocationResolver;
import org.graylog.plugins.map.geoip.MaxmindDataAdapter;
import org.graylog.plugins.map.geoip.processor.GeoIpProcessor;
import org.graylog2.plugin.PluginModule;

public class MapWidgetModule extends PluginModule {
    @Override
    protected void configure() {
        addMessageProcessor(GeoIpProcessor.class, GeoIpProcessor.Descriptor.class);
        installLookupDataAdapter(MaxmindDataAdapter.NAME,
                MaxmindDataAdapter.class,
                MaxmindDataAdapter.Factory.class,
                MaxmindDataAdapter.Config.class);

        install(new FactoryModuleBuilder()
                .implement(GeoIpResolver.class, Names.named("MAXMIND_CITY"), MaxMindIpLocationResolver.class)
                .implement(GeoIpResolver.class, Names.named("MAXMIND_ASN"), MaxMindIpAsnResolver.class)
                .implement(GeoIpResolver.class, Names.named("IPINFO_CITY"), IpInfoLocationResolver.class)
                .implement(GeoIpResolver.class, Names.named("IPINFO_ASN"), IpInfoIpAsnResolver.class)
                .build(GeoIpResolverFactory.class));
    }
}
