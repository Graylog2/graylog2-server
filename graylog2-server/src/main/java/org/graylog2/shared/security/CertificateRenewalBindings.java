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
package org.graylog2.shared.security;

import com.google.common.util.concurrent.Service;
import com.google.inject.multibindings.Multibinder;
import org.graylog.security.certutil.CaService;
import org.graylog.security.certutil.CaServiceImpl;
import org.graylog.security.rest.CertificateRenewalResource;
import org.graylog2.cluster.certificates.CertificateExchange;
import org.graylog2.cluster.certificates.CertificateExchangeImpl;
import org.graylog2.plugin.PluginModule;
import org.graylog2.shared.initializers.PeriodicalsService;

public class CertificateRenewalBindings extends PluginModule {
    @Override
    protected void configure() {
        bind(CaService.class).to(CaServiceImpl.class);
        //bind(CertRenewalService.class).to(CertRenewalServiceImpl.class).asEagerSingleton();
        addRestResource(CertificateRenewalResource.class);
        bind(CertificateExchange.class).to(CertificateExchangeImpl.class);

        Multibinder<Service> serviceBinder = Multibinder.newSetBinder(binder(), Service.class);
        serviceBinder.addBinding().to(PeriodicalsService.class).asEagerSingleton();
    }
}
