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

import com.google.inject.Scopes;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.elasticsearch.client.Client;
import org.elasticsearch.node.Node;
import org.graylog2.Configuration;
import org.graylog2.alerts.AlertSender;
import org.graylog2.alerts.FormattedEmailAlertSender;
import org.graylog2.alerts.types.FieldContentValueAlertCondition;
import org.graylog2.alerts.types.FieldValueAlertCondition;
import org.graylog2.alerts.types.MessageCountAlertCondition;
import org.graylog2.auditlog.NullAuditLogger;
import org.graylog2.bindings.providers.BundleExporterProvider;
import org.graylog2.bindings.providers.BundleImporterProvider;
import org.graylog2.bindings.providers.ClusterEventBusProvider;
import org.graylog2.bindings.providers.DefaultSecurityManagerProvider;
import org.graylog2.bindings.providers.EsClientProvider;
import org.graylog2.bindings.providers.EsNodeProvider;
import org.graylog2.bindings.providers.MongoConnectionProvider;
import org.graylog2.bindings.providers.RulesEngineProvider;
import org.graylog2.bindings.providers.SystemJobFactoryProvider;
import org.graylog2.bindings.providers.SystemJobManagerProvider;
import org.graylog2.buffers.processors.ServerProcessBufferProcessor;
import org.graylog2.bundles.BundleService;
import org.graylog2.cluster.ClusterConfigServiceImpl;
import org.graylog2.dashboards.widgets.WidgetCacheTime;
import org.graylog2.database.MongoConnection;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.filters.FilterService;
import org.graylog2.filters.FilterServiceImpl;
import org.graylog2.grok.GrokPatternRegistry;
import org.graylog2.indexer.SetIndexReadOnlyJob;
import org.graylog2.indexer.healing.FixDeflectorByDeleteJob;
import org.graylog2.indexer.healing.FixDeflectorByMoveJob;
import org.graylog2.indexer.indices.jobs.OptimizeIndexJob;
import org.graylog2.indexer.indices.jobs.SetIndexReadOnlyAndCalculateRangeJob;
import org.graylog2.indexer.ranges.CreateNewSingleIndexRangeJob;
import org.graylog2.indexer.ranges.RebuildIndexRangesJob;
import org.graylog2.inputs.InputEventListener;
import org.graylog2.inputs.InputStateListener;
import org.graylog2.inputs.PersistedInputsImpl;
import org.graylog2.plugin.RulesEngine;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.decorators.SearchResponseDecorator;
import org.graylog2.plugin.inject.Graylog2Module;
import org.graylog2.rest.NotFoundExceptionMapper;
import org.graylog2.rest.ScrollChunkWriter;
import org.graylog2.rest.ValidationExceptionMapper;
import org.graylog2.security.ldap.LdapConnector;
import org.graylog2.security.ldap.LdapSettingsImpl;
import org.graylog2.security.realm.AuthenticatingRealmModule;
import org.graylog2.security.realm.LdapUserAuthenticator;
import org.graylog2.shared.buffers.processors.ProcessBufferProcessor;
import org.graylog2.shared.inputs.PersistedInputs;
import org.graylog2.shared.journal.JournalReaderModule;
import org.graylog2.shared.journal.KafkaJournalModule;
import org.graylog2.shared.journal.NoopJournalModule;
import org.graylog2.shared.metrics.jersey2.MetricsDynamicBinding;
import org.graylog2.shared.security.RestrictToMasterFeature;
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
import org.graylog2.users.RoleService;
import org.graylog2.users.RoleServiceImpl;
import org.graylog2.users.UserImpl;

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.ext.ExceptionMapper;

import static com.google.inject.name.Names.named;

public class ServerBindings extends Graylog2Module {
    private final Configuration configuration;

    public ServerBindings(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    protected void configure() {
        bindInterfaces();
        bindSingletons();
        bindProviders();
        bindFactoryModules();
        bindDynamicFeatures();
        bindExceptionMappers();
        bindAdditionalJerseyComponents();
        bindEventBusListeners();
        install(new AuthenticatingRealmModule());
        bindSearchResponseDecorators();

        // make sure there is a default binding
        auditLoggerBinder().setDefault().to(NullAuditLogger.class);
    }

    private void bindProviders() {
        bind(ClusterEventBus.class).toProvider(ClusterEventBusProvider.class).asEagerSingleton();
    }

    private void bindFactoryModules() {
        // System Jobs
        install(new FactoryModuleBuilder().build(RebuildIndexRangesJob.Factory.class));
        install(new FactoryModuleBuilder().build(OptimizeIndexJob.Factory.class));
        install(new FactoryModuleBuilder().build(SetIndexReadOnlyJob.Factory.class));
        install(new FactoryModuleBuilder().build(CreateNewSingleIndexRangeJob.Factory.class));
        install(new FactoryModuleBuilder().build(FixDeflectorByDeleteJob.Factory.class));
        install(new FactoryModuleBuilder().build(FixDeflectorByMoveJob.Factory.class));
        install(new FactoryModuleBuilder().build(SetIndexReadOnlyAndCalculateRangeJob.Factory.class));

        install(new FactoryModuleBuilder().build(LdapSettingsImpl.Factory.class));
        install(new FactoryModuleBuilder().build(FieldValueAlertCondition.Factory.class));
        install(new FactoryModuleBuilder().build(MessageCountAlertCondition.Factory.class));
        install(new FactoryModuleBuilder().build(FieldContentValueAlertCondition.Factory.class));
        install(new FactoryModuleBuilder().build(WidgetCacheTime.Factory.class));
        install(new FactoryModuleBuilder().build(UserImpl.Factory.class));
    }

    private void bindSingletons() {
        bind(MongoConnection.class).toProvider(MongoConnectionProvider.class);

        if (configuration.isMessageJournalEnabled()) {
            install(new KafkaJournalModule());
            install(new JournalReaderModule());
        } else {
            install(new NoopJournalModule());
        }
        bind(Node.class).toProvider(EsNodeProvider.class).asEagerSingleton();
        bind(Client.class).toProvider(EsClientProvider.class).asEagerSingleton();
        bind(SystemJobManager.class).toProvider(SystemJobManagerProvider.class);
        bind(RulesEngine.class).toProvider(RulesEngineProvider.class);
        bind(LdapConnector.class).in(Scopes.SINGLETON);
        bind(LdapUserAuthenticator.class).in(Scopes.SINGLETON);
        bind(DefaultSecurityManager.class).toProvider(DefaultSecurityManagerProvider.class).asEagerSingleton();
        bind(SystemJobFactory.class).toProvider(SystemJobFactoryProvider.class);
        bind(GracefulShutdown.class).in(Scopes.SINGLETON);
        bind(BundleService.class).in(Scopes.SINGLETON);
        bind(BundleImporterProvider.class).in(Scopes.SINGLETON);
        bind(BundleExporterProvider.class).in(Scopes.SINGLETON);
        bind(ClusterStatsModule.class).asEagerSingleton();
        bind(ClusterConfigService.class).to(ClusterConfigServiceImpl.class).asEagerSingleton();
        bind(GrokPatternRegistry.class).in(Scopes.SINGLETON);

        bind(String[].class).annotatedWith(named("RestControllerPackages")).toInstance(new String[]{
                "org.graylog2.rest.resources",
                "org.graylog2.shared.rest.resources"
        });
    }

    private void bindInterfaces() {
        bind(AlertSender.class).to(FormattedEmailAlertSender.class);
        bind(StreamRouter.class);
        install(new FactoryModuleBuilder().implement(StreamRouterEngine.class, StreamRouterEngine.class).build(
                StreamRouterEngine.Factory.class));
        bind(FilterService.class).to(FilterServiceImpl.class).in(Scopes.SINGLETON);
        bind(ActivityWriter.class).to(SystemMessageActivityWriter.class);
        bind(PersistedInputs.class).to(PersistedInputsImpl.class);

        bind(ProcessBufferProcessor.class).to(ServerProcessBufferProcessor.class);
        bind(RoleService.class).to(RoleServiceImpl.class).in(Scopes.SINGLETON);
    }

    private void bindDynamicFeatures() {
        final Multibinder<Class<? extends DynamicFeature>> dynamicFeatures = jerseyDynamicFeatureBinder();
        dynamicFeatures.addBinding().toInstance(MetricsDynamicBinding.class);
        dynamicFeatures.addBinding().toInstance(RestrictToMasterFeature.class);
    }

    private void bindExceptionMappers() {
        final Multibinder<Class<? extends ExceptionMapper>> exceptionMappers = jerseyExceptionMapperBinder();
        exceptionMappers.addBinding().toInstance(NotFoundExceptionMapper.class);
        exceptionMappers.addBinding().toInstance(ValidationExceptionMapper.class);
    }

    private void bindAdditionalJerseyComponents() {
        jerseyAdditionalComponentsBinder().addBinding().toInstance(ScrollChunkWriter.class);
    }

    private void bindEventBusListeners() {
        bind(InputStateListener.class).asEagerSingleton();
        bind(InputEventListener.class).asEagerSingleton();
        bind(LocalDebugEventListener.class).asEagerSingleton();
        bind(ClusterDebugEventListener.class).asEagerSingleton();
    }

    private void bindSearchResponseDecorators() {
        // only triggering an initialize to make sure that the binding exists
        final MapBinder<String, SearchResponseDecorator.Factory> searchResponseDecoratorBinder = searchResponseDecoratorBinder();
    }
}
