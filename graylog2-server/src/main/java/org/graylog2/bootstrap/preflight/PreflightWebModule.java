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

import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import org.graylog.security.certutil.CaService;
import org.graylog.security.certutil.CaServiceImpl;
import org.graylog.security.certutil.keystore.storage.KeystoreContentMover;
import org.graylog.security.certutil.keystore.storage.SinglePasswordKeystoreContentMover;
import org.graylog2.Configuration;
import org.graylog2.bindings.providers.ClusterEventBusProvider;
import org.graylog2.bindings.providers.MongoConnectionProvider;
import org.graylog2.bootstrap.preflight.web.PreflightBoot;
import org.graylog2.bootstrap.preflight.web.resources.CertificateRenewalPolicyResource;
import org.graylog2.bootstrap.preflight.web.resources.PreflightAssetsResource;
import org.graylog2.bootstrap.preflight.web.resources.PreflightResource;
import org.graylog2.bootstrap.preflight.web.resources.PreflightStatusResource;
import org.graylog2.cluster.ClusterConfigServiceImpl;
import org.graylog2.cluster.leader.LeaderElectionModule;
import org.graylog2.cluster.lock.LockServiceModule;
import org.graylog2.cluster.nodes.DataNodeClusterService;
import org.graylog2.cluster.nodes.DataNodeDto;
import org.graylog2.cluster.nodes.NodeService;
import org.graylog2.cluster.nodes.ServerNodeClusterService;
import org.graylog2.cluster.nodes.ServerNodeDto;
import org.graylog2.database.MongoConnection;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.events.ClusterEventCleanupPeriodical;
import org.graylog2.events.ClusterEventPeriodical;
import org.graylog2.migrations.V20230929142900_CreateInitialPreflightPassword;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.inject.Graylog2Module;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.periodical.Periodical;
import org.graylog2.shared.bindings.providers.EventBusProvider;
import org.graylog2.shared.bindings.providers.ServiceManagerProvider;
import org.graylog2.shared.initializers.PeriodicalsService;

import static java.util.Objects.requireNonNull;

public class PreflightWebModule extends Graylog2Module {

    public static final String FEATURE_FLAG_PREFLIGHT_WEB_ENABLED = "preflight_web";

    private final Configuration configuration;

    public PreflightWebModule(Configuration configuration) {
        this.configuration = requireNonNull(configuration);
    }

    @Override
    protected void configure() {

        bind(ServiceManager.class).toProvider(ServiceManagerProvider.class).asEagerSingleton();
        bind(MongoConnection.class).toProvider(MongoConnectionProvider.class);
        bind(new TypeLiteral<NodeService<ServerNodeDto>>() {}).to(ServerNodeClusterService.class);
        bind(new TypeLiteral<NodeService<DataNodeDto>>() {}).to(DataNodeClusterService.class);
        bind(KeystoreContentMover.class).to(SinglePasswordKeystoreContentMover.class).asEagerSingleton();
        bind(ClusterConfigService.class).to(ClusterConfigServiceImpl.class);
        bind(CaService.class).to(CaServiceImpl.class);

        bind(PreflightConfigService.class).to(PreflightConfigServiceImpl.class);
        bind(PreflightBoot.class).asEagerSingleton();
        bind(NotificationService.class).to(NullNotificationService.class);

        addPreflightRestResource(PreflightResource.class);
        addPreflightRestResource(CertificateRenewalPolicyResource.class);
        addPreflightRestResource(PreflightStatusResource.class);
        addPreflightRestResource(PreflightAssetsResource.class);

        Multibinder<Periodical> periodicalBinder = Multibinder.newSetBinder(binder(), Periodical.class);
        periodicalBinder.addBinding().to(GraylogCertificateProvisioningPeriodical.class);
        periodicalBinder.addBinding().to(ClusterEventPeriodical.class);
        periodicalBinder.addBinding().to(ClusterEventCleanupPeriodical.class);

        Multibinder<Service> serviceBinder = Multibinder.newSetBinder(binder(), Service.class);
        serviceBinder.addBinding().to(PreflightJerseyService.class);
        serviceBinder.addBinding().to(PeriodicalsService.class);

        install(new LockServiceModule());
        install(new LeaderElectionModule(configuration));

        bind(ClusterEventBus.class).toProvider(ClusterEventBusProvider.class).asEagerSingleton();
        bind(EventBus.class).toProvider(EventBusProvider.class).asEagerSingleton();

        migrationsBinder().addBinding().to(V20230929142900_CreateInitialPreflightPassword.class);

        // needed for the ObjectMapperModule
        MapBinder.newMapBinder(binder(),
                TypeLiteral.get(String.class),
                new TypeLiteral<MessageInput.Factory<? extends MessageInput>>() {
                });


    }

    protected void addPreflightRestResource(Class<?> restResourceClass) {
        preflightRestResourceBinder().addBinding().toInstance(restResourceClass);
    }

    private Multibinder<Class<?>> preflightRestResourceBinder() {
        return Multibinder.newSetBinder(
                binder(),
                new TypeLiteral<Class<?>>() {},
                PreflightRestResourcesBinding.class
        );
    }
}
