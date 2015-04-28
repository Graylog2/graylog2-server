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
package org.graylog2.bindings;

import com.google.common.eventbus.EventBus;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.Multibinder;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.elasticsearch.node.Node;
import org.graylog2.Configuration;
import org.graylog2.alerts.AlertSender;
import org.graylog2.alerts.FormattedEmailAlertSender;
import org.graylog2.alerts.types.FieldValueAlertCondition;
import org.graylog2.alerts.types.MessageCountAlertCondition;
import org.graylog2.bindings.providers.BundleExporterProvider;
import org.graylog2.bindings.providers.BundleImporterProvider;
import org.graylog2.bindings.providers.ClusterEventBusProvider;
import org.graylog2.bindings.providers.DefaultSecurityManagerProvider;
import org.graylog2.bindings.providers.EsNodeProvider;
import org.graylog2.bindings.providers.LdapConnectorProvider;
import org.graylog2.bindings.providers.LdapUserAuthenticatorProvider;
import org.graylog2.bindings.providers.MongoConnectionProvider;
import org.graylog2.bindings.providers.RotationStrategyProvider;
import org.graylog2.bindings.providers.RulesEngineProvider;
import org.graylog2.bindings.providers.SystemJobFactoryProvider;
import org.graylog2.bindings.providers.SystemJobManagerProvider;
import org.graylog2.buffers.processors.ServerProcessBufferProcessor;
import org.graylog2.bundles.BundleService;
import org.graylog2.cluster.ClusterConfigServiceImpl;
import org.graylog2.database.MongoConnection;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.filters.FilterService;
import org.graylog2.filters.FilterServiceImpl;
import org.graylog2.indexer.SetIndexReadOnlyJob;
import org.graylog2.indexer.healing.FixDeflectorByDeleteJob;
import org.graylog2.indexer.healing.FixDeflectorByMoveJob;
import org.graylog2.indexer.indices.jobs.OptimizeIndexJob;
import org.graylog2.indexer.ranges.CreateNewSingleIndexRangeJob;
import org.graylog2.indexer.ranges.RebuildIndexRangesJob;
import org.graylog2.inputs.InputStateListener;
import org.graylog2.inputs.PersistedInputsImpl;
import org.graylog2.jersey.container.netty.SecurityContextFactory;
import org.graylog2.plugin.BaseConfiguration;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.RulesEngine;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.indexer.rotation.RotationStrategy;
import org.graylog2.rest.NotFoundExceptionMapper;
import org.graylog2.rest.RestAccessLogFilter;
import org.graylog2.rest.ScrollChunkWriter;
import org.graylog2.rest.ValidationExceptionMapper;
import org.graylog2.security.ShiroSecurityContextFactory;
import org.graylog2.security.ldap.LdapConnector;
import org.graylog2.security.ldap.LdapSettingsImpl;
import org.graylog2.security.realm.LdapUserAuthenticator;
import org.graylog2.shared.buffers.processors.ProcessBufferProcessor;
import org.graylog2.shared.inputs.PersistedInputs;
import org.graylog2.shared.journal.JournalReaderModule;
import org.graylog2.shared.journal.KafkaJournalModule;
import org.graylog2.shared.journal.NoopJournalModule;
import org.graylog2.shared.metrics.jersey2.MetricsDynamicBinding;
import org.graylog2.shared.security.RestrictToMasterFeature;
import org.graylog2.shared.security.ShiroSecurityBinding;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.graylog2.streams.StreamRouter;
import org.graylog2.streams.StreamRouterEngine;
import org.graylog2.system.activities.SystemMessageActivityWriter;
import org.graylog2.system.debug.ClusterDebugEventListener;
import org.graylog2.system.debug.LocalDebugEventListener;
import org.graylog2.system.jobs.SystemJobFactory;
import org.graylog2.system.jobs.SystemJobManager;
import org.graylog2.system.shutdown.GracefulShutdown;
import org.graylog2.system.stats.ClusterStatsModule;

import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.ext.ExceptionMapper;
import java.util.Set;

import static com.google.inject.name.Names.named;

public class ServerBindings extends AbstractModule {
    private final Configuration configuration;
    private final Set<ServerStatus.Capability> capabilities;

    public ServerBindings(Configuration configuration, Set<ServerStatus.Capability> capabilities) {
        this.configuration = configuration;
        this.capabilities = capabilities;
    }

    @Override
    protected void configure() {
        bindInterfaces();
        bindSingletons();
        bindProviders();
        bindFactoryModules();
        bindDynamicFeatures();
        bindContainerResponseFilters();
        bindExceptionMappers();
        bindAdditionalJerseyComponents();
        bindPluginMetaData();
        bindEventBusListeners();
    }

    private void bindProviders() {
        bind(RotationStrategy.class).toProvider(RotationStrategyProvider.class);
        bind(EventBus.class).annotatedWith(ClusterEventBus.class).toProvider(ClusterEventBusProvider.class).asEagerSingleton();
    }

    private void bindFactoryModules() {
        install(new FactoryModuleBuilder().build(RebuildIndexRangesJob.Factory.class));
        install(new FactoryModuleBuilder().build(OptimizeIndexJob.Factory.class));
        install(new FactoryModuleBuilder().build(SetIndexReadOnlyJob.Factory.class));
        install(new FactoryModuleBuilder().build(CreateNewSingleIndexRangeJob.Factory.class));
        install(new FactoryModuleBuilder().build(FixDeflectorByDeleteJob.Factory.class));
        install(new FactoryModuleBuilder().build(FixDeflectorByMoveJob.Factory.class));
        install(new FactoryModuleBuilder().build(LdapSettingsImpl.Factory.class));
        install(new FactoryModuleBuilder().build(FieldValueAlertCondition.Factory.class));
        install(new FactoryModuleBuilder().build(MessageCountAlertCondition.Factory.class));
    }

    private void bindSingletons() {
        bind(Configuration.class).toInstance(configuration);
        bind(BaseConfiguration.class).toInstance(configuration);

        bind(MongoConnection.class).toProvider(MongoConnectionProvider.class);

        Multibinder<ServerStatus.Capability> capabilityBinder = Multibinder.newSetBinder(binder(), ServerStatus.Capability.class);
        for (ServerStatus.Capability capability : capabilities) {
            capabilityBinder.addBinding().toInstance(capability);
        }

        bind(ServerStatus.class).in(Scopes.SINGLETON);

        if (configuration.isMessageJournalEnabled()) {
            install(new KafkaJournalModule());
            install(new JournalReaderModule());
        } else {
            install(new NoopJournalModule());
        }
        bind(Node.class).toProvider(EsNodeProvider.class).in(Scopes.SINGLETON);
        bind(SystemJobManager.class).toProvider(SystemJobManagerProvider.class);
        bind(RulesEngine.class).toProvider(RulesEngineProvider.class);
        bind(LdapConnector.class).toProvider(LdapConnectorProvider.class);
        bind(LdapUserAuthenticator.class).toProvider(LdapUserAuthenticatorProvider.class);
        bind(DefaultSecurityManager.class).toProvider(DefaultSecurityManagerProvider.class);
        bind(SystemJobFactory.class).toProvider(SystemJobFactoryProvider.class);
        bind(GracefulShutdown.class).in(Scopes.SINGLETON);
        bind(BundleService.class).in(Scopes.SINGLETON);
        bind(BundleImporterProvider.class).in(Scopes.SINGLETON);
        bind(BundleExporterProvider.class).in(Scopes.SINGLETON);
        bind(ClusterStatsModule.class).asEagerSingleton();
        bind(ClusterConfigService.class).to(ClusterConfigServiceImpl.class).asEagerSingleton();

        bind(String[].class).annotatedWith(named("RestControllerPackages")).toInstance(new String[]{
                "org.graylog2.rest.resources",
                "org.graylog2.shared.rest.resources"
        });
    }

    private void bindInterfaces() {
        bind(SecurityContextFactory.class).to(ShiroSecurityContextFactory.class);
        bind(AlertSender.class).to(FormattedEmailAlertSender.class);
        bind(StreamRouter.class);
        install(new FactoryModuleBuilder().implement(StreamRouterEngine.class, StreamRouterEngine.class).build(StreamRouterEngine.Factory.class));
        bind(FilterService.class).to(FilterServiceImpl.class).in(Scopes.SINGLETON);
        bind(ActivityWriter.class).to(SystemMessageActivityWriter.class);
        bind(PersistedInputs.class).to(PersistedInputsImpl.class);

        bind(ProcessBufferProcessor.class).to(ServerProcessBufferProcessor.class);
    }

    private void bindDynamicFeatures() {
        TypeLiteral<Class<? extends DynamicFeature>> type = new TypeLiteral<Class<? extends DynamicFeature>>() {
        };
        Multibinder<Class<? extends DynamicFeature>> setBinder = Multibinder.newSetBinder(binder(), type);
        setBinder.addBinding().toInstance(ShiroSecurityBinding.class);
        setBinder.addBinding().toInstance(MetricsDynamicBinding.class);
        setBinder.addBinding().toInstance(RestrictToMasterFeature.class);
    }

    private void bindContainerResponseFilters() {
        TypeLiteral<Class<? extends ContainerResponseFilter>> type = new TypeLiteral<Class<? extends ContainerResponseFilter>>() {
        };
        Multibinder<Class<? extends ContainerResponseFilter>> setBinder = Multibinder.newSetBinder(binder(), type);
        setBinder.addBinding().toInstance(RestAccessLogFilter.class);
    }

    private void bindExceptionMappers() {
        TypeLiteral<Class<? extends ExceptionMapper>> type = new TypeLiteral<Class<? extends ExceptionMapper>>() {
        };
        Multibinder<Class<? extends ExceptionMapper>> setBinder = Multibinder.newSetBinder(binder(), type);
        setBinder.addBinding().toInstance(NotFoundExceptionMapper.class);
        setBinder.addBinding().toInstance(ValidationExceptionMapper.class);
    }

    private void bindAdditionalJerseyComponents() {
        Multibinder<Class> componentBinder = Multibinder.newSetBinder(binder(), Class.class, named("additionalJerseyComponents"));
        componentBinder.addBinding().toInstance(ScrollChunkWriter.class);
    }

    private void bindPluginMetaData() {
        Multibinder<PluginMetaData> setBinder = Multibinder.newSetBinder(binder(), PluginMetaData.class);
    }

    private void bindEventBusListeners() {
        bind(InputStateListener.class).asEagerSingleton();
        bind(LocalDebugEventListener.class).asEagerSingleton();
        bind(ClusterDebugEventListener.class).asEagerSingleton();
    }
}
