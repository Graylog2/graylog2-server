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
package org.graylog.plugins.threatintel;

import com.google.inject.Binder;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import org.graylog.plugins.pipelineprocessor.ast.functions.Function;
import org.graylog.plugins.threatintel.adapters.abusech.AbuseChRansomAdapter;
import org.graylog.plugins.threatintel.adapters.otx.OTXDataAdapter;
import org.graylog.plugins.threatintel.adapters.spamhaus.SpamhausEDROPDataAdapter;
import org.graylog.plugins.threatintel.adapters.tor.TorExitNodeDataAdapter;
import org.graylog.plugins.threatintel.functions.DomainFunctions;
import org.graylog.plugins.threatintel.functions.GenericLookupResult;
import org.graylog.plugins.threatintel.functions.IPFunctions;
import org.graylog.plugins.threatintel.functions.abusech.AbuseChRansomDomainLookupFunction;
import org.graylog.plugins.threatintel.functions.abusech.AbuseChRansomIpLookupFunction;
import org.graylog.plugins.threatintel.functions.global.GlobalDomainLookupFunction;
import org.graylog.plugins.threatintel.functions.global.GlobalIpLookupFunction;
import org.graylog.plugins.threatintel.functions.misc.LookupTableFunction;
import org.graylog.plugins.threatintel.functions.misc.PrivateNetLookupFunction;
import org.graylog.plugins.threatintel.functions.otx.OTXDomainLookupFunction;
import org.graylog.plugins.threatintel.functions.otx.OTXIPLookupFunction;
import org.graylog.plugins.threatintel.functions.spamhaus.SpamhausIpLookupFunction;
import org.graylog.plugins.threatintel.functions.tor.TorExitNodeLookupFunction;
import org.graylog.plugins.threatintel.migrations.V20170821100300_MigrateOTXAPIToken;
import org.graylog.plugins.threatintel.migrations.V20180906112716_RecreateThreatintelLookupTables;
import org.graylog.plugins.threatintel.migrations.V20240531101100_RemoveAbusechContentPack;
import org.graylog.plugins.threatintel.whois.ip.WhoisDataAdapter;
import org.graylog.plugins.threatintel.whois.ip.WhoisLookupIpFunction;
import org.graylog2.plugin.PluginConfigBean;
import org.graylog2.plugin.PluginModule;

import java.util.Collections;
import java.util.Set;

public class ThreatIntelPluginModule extends PluginModule {

    @Override
    public Set<? extends PluginConfigBean> getConfigBeans() {
        return Collections.emptySet();
    }

    @Override
    protected void configure() {

        bind(PluginConfigService.class).in(Scopes.SINGLETON);

        // AlienVault OTX threat intel lookup.
        addMessageProcessorFunction(OTXDomainLookupFunction.NAME, OTXDomainLookupFunction.class);
        addMessageProcessorFunction(OTXIPLookupFunction.NAME, OTXIPLookupFunction.class);

        // Tor exit node lookup.
        addMessageProcessorFunction(TorExitNodeLookupFunction.NAME, TorExitNodeLookupFunction.class);

        // Spamhaus DROP and EDROP lookup.
        addMessageProcessorFunction(SpamhausIpLookupFunction.NAME, SpamhausIpLookupFunction.class);

        // abuse.ch Ransomware
        addMessageProcessorFunction(AbuseChRansomDomainLookupFunction.NAME, AbuseChRansomDomainLookupFunction.class);
        addMessageProcessorFunction(AbuseChRansomIpLookupFunction.NAME, AbuseChRansomIpLookupFunction.class);

        // Global/combined lookup
        addMessageProcessorFunction(GlobalIpLookupFunction.NAME, GlobalIpLookupFunction.class);
        addMessageProcessorFunction(GlobalDomainLookupFunction.NAME, GlobalDomainLookupFunction.class);

        // WHOIS IP lookup.
        addMessageProcessorFunction(WhoisLookupIpFunction.NAME, WhoisLookupIpFunction.class);

        // Private network lookup.
        addMessageProcessorFunction(PrivateNetLookupFunction.NAME, PrivateNetLookupFunction.class);

        installLookupDataAdapter(AbuseChRansomAdapter.NAME, AbuseChRansomAdapter.class, AbuseChRansomAdapter.Factory.class, AbuseChRansomAdapter.Config.class);
        installLookupDataAdapter(SpamhausEDROPDataAdapter.NAME, SpamhausEDROPDataAdapter.class, SpamhausEDROPDataAdapter.Factory.class, SpamhausEDROPDataAdapter.Config.class);
        installLookupDataAdapter(TorExitNodeDataAdapter.NAME, TorExitNodeDataAdapter.class, TorExitNodeDataAdapter.Factory.class, TorExitNodeDataAdapter.Config.class);
        installLookupDataAdapter(WhoisDataAdapter.NAME, WhoisDataAdapter.class, WhoisDataAdapter.Factory.class, WhoisDataAdapter.Config.class);
        installLookupDataAdapter(OTXDataAdapter.NAME, OTXDataAdapter.class, OTXDataAdapter.Factory.class, OTXDataAdapter.Config.class);

        addMigration(V20180906112716_RecreateThreatintelLookupTables.class);
        addMigration(V20170821100300_MigrateOTXAPIToken.class);
        addMigration(V20240531101100_RemoveAbusechContentPack.class);

        addDomainFunction("abusech_ransomware", AbuseChRansomDomainLookupFunction.class);
        addIPFunction("abusech_ransomware", AbuseChRansomIpLookupFunction.class);
        addIPFunction("spamhaus", SpamhausIpLookupFunction.class);
        addIPFunction("tor", TorExitNodeLookupFunction.class);
    }

    private void addMessageProcessorFunction(String name, Class<? extends Function<?>> functionClass) {
        addMessageProcessorFunction(binder(), name, functionClass);
    }

    private MapBinder<String, Function<?>> processorFunctionBinder(Binder binder) {
        return MapBinder.newMapBinder(binder, TypeLiteral.get(String.class), new TypeLiteral<Function<?>>() {});
    }

    private void addMessageProcessorFunction(Binder binder, String name, Class<? extends Function<?>> functionClass) {
        processorFunctionBinder(binder).addBinding(name).to(functionClass);

    }

    private MapBinder<String, LookupTableFunction<? extends GenericLookupResult>> domainFunctionBinder() {
        return MapBinder.newMapBinder(binder(), TypeLiteral.get(String.class), new TypeLiteral<LookupTableFunction<? extends GenericLookupResult>>() {}, DomainFunctions.class);
    }

    private MapBinder<String, LookupTableFunction<? extends GenericLookupResult>> ipFunctionBinder() {
        return MapBinder.newMapBinder(binder(), TypeLiteral.get(String.class), new TypeLiteral<LookupTableFunction<? extends GenericLookupResult>>() {}, IPFunctions.class);
    }

    private void addDomainFunction(String id, Class<? extends LookupTableFunction<? extends GenericLookupResult>> functionClass) {
        domainFunctionBinder().addBinding(id).to(functionClass);
    }

    private void addIPFunction(String id, Class<? extends LookupTableFunction<? extends GenericLookupResult>> functionClass) {
        ipFunctionBinder().addBinding(id).to(functionClass);
    }
}
