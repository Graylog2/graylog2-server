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
package org.graylog2.bootstrap.preflight;

import com.google.inject.assistedinject.FactoryModuleBuilder;
import okhttp3.OkHttpClient;
import org.graylog.security.certutil.CaService;
import org.graylog.security.certutil.CaServiceImpl;
import org.graylog.security.certutil.keystore.storage.KeystoreContentMover;
import org.graylog.security.certutil.keystore.storage.SinglePasswordKeystoreContentMover;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.audit.NullAuditEventSender;
import org.graylog2.plugin.inject.Graylog2Module;
import org.graylog2.plugin.system.FilePersistedNodeIdProvider;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.security.CustomCAX509TrustManager;
import org.graylog2.security.TrustManagerProvider;
import org.graylog2.shared.bindings.providers.OkHttpClientProvider;
import org.graylog2.storage.providers.ElasticsearchVersionProvider;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class ServerPreflightChecksModule extends Graylog2Module {

    @Override
    protected void configure() {
        bind(CaService.class).to(CaServiceImpl.class);

        install(new FactoryModuleBuilder()
                .implement(TrustManager.class, CustomCAX509TrustManager.class)
                .build(TrustManagerProvider.class));

        bind(X509TrustManager.class).to(CustomCAX509TrustManager.class).asEagerSingleton();

        bind(KeystoreContentMover.class).to(SinglePasswordKeystoreContentMover.class).asEagerSingleton();
        bind(OkHttpClient.class).toProvider(OkHttpClientProvider.class).asEagerSingleton();
        bind(ElasticsearchVersionProvider.class).asEagerSingleton();
        bind(NodeId.class).toProvider(FilePersistedNodeIdProvider.class).asEagerSingleton();
        bind(AuditEventSender.class).to(NullAuditEventSender.class);

        // The MongoDBPreflightCheck is not registered here, because it is called separately from ServerBootstrap
        addPreflightCheck(SearchDbPreflightCheck.class);
        addPreflightCheck(DiskJournalPreflightCheck.class);
    }
}
