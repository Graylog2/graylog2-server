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
package org.graylog2.bootstrap;

import com.github.joschi.jadconfig.guice.NamedConfigParametersModule;
import com.github.rvesse.airline.annotations.Option;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.ProvisionException;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.google.inject.util.Types;
import org.graylog2.Configuration;
import org.graylog2.audit.AuditActor;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.bindings.ConfigurationModule;
import org.graylog2.bootstrap.preflight.MongoDBPreflightCheck;
import org.graylog2.bootstrap.preflight.PreflightCheckException;
import org.graylog2.bootstrap.preflight.PreflightCheckService;
import org.graylog2.bootstrap.preflight.ServerPreflightChecksModule;
import org.graylog2.configuration.PathConfiguration;
import org.graylog2.configuration.TLSProtocolsConfiguration;
import org.graylog2.migrations.Migration;
import org.graylog2.plugin.Plugin;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.shared.bindings.FreshInstallDetectionModule;
import org.graylog2.shared.bindings.GenericBindings;
import org.graylog2.shared.bindings.GenericInitializerBindings;
import org.graylog2.shared.bindings.IsDevelopmentBindings;
import org.graylog2.shared.bindings.SchedulerBindings;
import org.graylog2.shared.bindings.ServerStatusBindings;
import org.graylog2.shared.bindings.SharedPeriodicalBindings;
import org.graylog2.shared.bindings.ValidatorModule;
import org.graylog2.shared.initializers.ServiceManagerListener;
import org.graylog2.shared.security.SecurityBindings;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.graylog2.shared.system.stats.SystemStatsModule;
import org.jsoftbiz.utils.OS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.graylog2.audit.AuditEventTypes.NODE_STARTUP_COMPLETE;
import static org.graylog2.audit.AuditEventTypes.NODE_STARTUP_INITIATE;

public abstract class ServerBootstrap extends CmdLineTool {
    private static final Logger LOG = LoggerFactory.getLogger(ServerBootstrap.class);
    private boolean isFreshInstallation;

    protected ServerBootstrap(String commandName, Configuration configuration) {
        super(commandName, configuration);
        this.commandName = commandName;
    }

    @Option(name = {"-p", "--pidfile"}, description = "File containing the PID of Graylog")
    private String pidFile = TMPDIR + FILE_SEPARATOR + "graylog.pid";

    @Option(name = {"-np", "--no-pid-file"}, description = "Do not write a PID file (overrides -p/--pidfile)")
    private boolean noPidFile = false;

    protected abstract void startNodeRegistration(Injector injector);

    public String getPidFile() {
        return pidFile;
    }

    public boolean isNoPidFile() {
        return noPidFile;
    }

    private boolean isFreshInstallation() {
        return isFreshInstallation;
    }

    private void registerFreshInstallation() {
        this.isFreshInstallation = true;
    }

    @Override
    protected void beforeStart(TLSProtocolsConfiguration tlsProtocolsConfiguration, PathConfiguration pathConfiguration) {
        super.beforeStart(tlsProtocolsConfiguration, pathConfiguration);

        // Do not use a PID file if the user requested not to
        if (!isNoPidFile()) {
            savePidFile(getPidFile());
        }
        // This needs to run before the first SSLContext is instantiated,
        // because it sets up the default SSLAlgorithmConstraints
        applySecuritySettings(tlsProtocolsConfiguration);

        // Set these early in the startup because netty's NativeLibraryUtil uses a static initializer
        setNettyNativeDefaults(pathConfiguration);

    }

    @Override
    protected void beforeInjectorCreation(Set<Plugin> plugins) {
        runPreFlightChecks(plugins);
    }

    private void runPreFlightChecks(Set<Plugin> plugins) {
        if (configuration.getSkipPreflightChecks()) {
            LOG.info("Skipping preflight checks");
            return;
        }

        runMongoPreflightCheck();

        final List<Module> preflightCheckModules = plugins.stream().map(Plugin::preflightCheckModules)
                .flatMap(Collection::stream).collect(Collectors.toList());
        preflightCheckModules.add(new FreshInstallDetectionModule(isFreshInstallation()));

        getPreflightInjector(preflightCheckModules).getInstance(PreflightCheckService.class).runChecks();
    }

    private void runMongoPreflightCheck() {
        // The MongoDBPreflightCheck is not run via the PreflightCheckService,
        // because it also detects whether we are running on a fresh Graylog installation
        final Injector injector = getMongoPreFlightInjector();
        final MongoDBPreflightCheck mongoDBPreflightCheck = injector.getInstance(MongoDBPreflightCheck.class);
        try {
            mongoDBPreflightCheck.runCheck();
        } catch (PreflightCheckException e) {
            LOG.error("Preflight check failed with error: {}", e.getLocalizedMessage());
            throw e;
        }

        if (mongoDBPreflightCheck.dbIsEmpty()) {
            registerFreshInstallation();
        }
    }

    private Injector getMongoPreFlightInjector() {
        return Guice.createInjector(
                new IsDevelopmentBindings(),
                new NamedConfigParametersModule(jadConfig.getConfigurationBeans()),
                new ConfigurationModule(configuration)
        );
    }

    private Injector getPreflightInjector(List<Module> preflightCheckModules) {
        final Injector injector = Guice.createInjector(
                new IsDevelopmentBindings(),
                new NamedConfigParametersModule(jadConfig.getConfigurationBeans()),
                new ServerStatusBindings(capabilities()),
                new ConfigurationModule(configuration),
                new SystemStatsModule(configuration.isDisableNativeSystemStatsCollector()),
                new ServerPreflightChecksModule(),
                new Module() {
                    @Override
                    public void configure(Binder binder) {
                        preflightCheckModules.forEach(binder::install);
                    }
                });
        return injector;
    }

    private void setNettyNativeDefaults(PathConfiguration pathConfiguration) {
        // Give netty a better spot than /tmp to unpack its tcnative libraries
        if (System.getProperty("io.netty.native.workdir") == null) {
            System.setProperty("io.netty.native.workdir", pathConfiguration.getNativeLibDir().toAbsolutePath().toString());
        }
        // Don't delete the native lib after unpacking, as this confuses needrestart(1) on some distributions
        if (System.getProperty("io.netty.native.deleteLibAfterLoading") == null) {
            System.setProperty("io.netty.native.deleteLibAfterLoading", "false");
        }
    }

    @Override
    protected void startCommand() {
        final AuditEventSender auditEventSender = injector.getInstance(AuditEventSender.class);
        final NodeId nodeId = injector.getInstance(NodeId.class);
        final String systemInformation = Tools.getSystemInformation();
        final Map<String, Object> auditEventContext = ImmutableMap.of(
                "version", version.toString(),
                "java", systemInformation,
                "node_id", nodeId.toString()
        );
        auditEventSender.success(AuditActor.system(nodeId), NODE_STARTUP_INITIATE, auditEventContext);

        final OS os = OS.getOs();

        LOG.info("Graylog {} {} starting up", commandName, version);
        LOG.info("JRE: {}", systemInformation);
        LOG.info("Deployment: {}", configuration.getInstallationSource());
        LOG.info("OS: {}", os.getPlatformName());
        LOG.info("Arch: {}", os.getArch());

        try {
            if (configuration.isLeader() && configuration.runMigrations()) {
                runMigrations();
            }
        } catch (Exception e) {
            LOG.warn("Exception while running migrations", e);
            System.exit(1);
        }

        final ServerStatus serverStatus = injector.getInstance(ServerStatus.class);
        serverStatus.initialize();

        startNodeRegistration(injector);

        final ActivityWriter activityWriter;
        final ServiceManager serviceManager;
        final Service leaderElectionService;
        try {
            activityWriter = injector.getInstance(ActivityWriter.class);
            serviceManager = injector.getInstance(ServiceManager.class);
            leaderElectionService = injector.getInstance(Key.get(Service.class, Names.named("LeaderElectionService")));
        } catch (ProvisionException e) {
            LOG.error("Guice error", e);
            annotateProvisionException(e);
            auditEventSender.failure(AuditActor.system(nodeId), NODE_STARTUP_INITIATE, auditEventContext);
            System.exit(-1);
            return;
        } catch (Exception e) {
            LOG.error("Unexpected exception", e);
            auditEventSender.failure(AuditActor.system(nodeId), NODE_STARTUP_INITIATE, auditEventContext);
            System.exit(-1);
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(injector.getInstance(shutdownHook())));

        // propagate default size to input plugins
        MessageInput.setDefaultRecvBufferSize(configuration.getUdpRecvBufferSizes());

        // Start services.
        final ServiceManagerListener serviceManagerListener = injector.getInstance(ServiceManagerListener.class);
        serviceManager.addListener(serviceManagerListener, MoreExecutors.directExecutor());
        try {
            leaderElectionService.startAsync().awaitRunning();
            serviceManager.startAsync().awaitHealthy();
        } catch (Exception e) {
            try {
                serviceManager.stopAsync().awaitStopped(configuration.getShutdownTimeout(), TimeUnit.MILLISECONDS);
            } catch (TimeoutException timeoutException) {
                LOG.error("Unable to shutdown properly on time. {}", serviceManager.servicesByState());
            }
            LOG.error("Graylog startup failed. Exiting. Exception was:", e);
            auditEventSender.failure(AuditActor.system(nodeId), NODE_STARTUP_INITIATE, auditEventContext);
            System.exit(-1);
        }
        LOG.info("Services started, startup times in ms: {}", serviceManager.startupTimes());

        activityWriter.write(new Activity("Started up.", Main.class));
        LOG.info("Graylog {} up and running.", commandName);
        auditEventSender.success(AuditActor.system(nodeId), NODE_STARTUP_COMPLETE, auditEventContext);

        // Block forever.
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            return;
        }
    }

    public void runMigrations() {
        //noinspection unchecked
        final TypeLiteral<Set<Migration>> typeLiteral = (TypeLiteral<Set<Migration>>) TypeLiteral.get(Types.setOf(Migration.class));
        Set<Migration> migrations = injector.getInstance(Key.get(typeLiteral));

        LOG.info("Running {} migrations...", migrations.size());

        ImmutableSortedSet.copyOf(migrations).forEach(m -> {
            LOG.debug("Running migration <{}>", m.getClass().getCanonicalName());
            try {
                m.upgrade();
            } catch (Exception e) {
                if (configuration.ignoreMigrationFailures()) {
                    LOG.warn("  Ignoring migration failure: {}", e.getMessage());
                } else {
                    throw e;
                }
            }
        });
    }

    protected void savePidFile(final String pidFile) {
        final String pid = Tools.getPID();
        final Path pidFilePath = Paths.get(pidFile);
        pidFilePath.toFile().deleteOnExit();

        try {
            if (isNullOrEmpty(pid) || "unknown".equals(pid)) {
                throw new Exception("Could not determine PID.");
            }

            Files.write(pidFilePath, pid.getBytes(StandardCharsets.UTF_8), StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW, LinkOption.NOFOLLOW_LINKS);
        } catch (Exception e) {
            LOG.error("Could not write PID file: " + e.getMessage(), e);
            System.exit(1);
        }
    }

    @Override
    protected List<Module> getSharedBindingsModules() {
        final List<Module> result = super.getSharedBindingsModules();

        result.add(new FreshInstallDetectionModule(isFreshInstallation()));
        result.add(new GenericBindings(isMigrationCommand()));
        result.add(new SecurityBindings());
        result.add(new ServerStatusBindings(capabilities()));
        result.add(new ValidatorModule());
        result.add(new SharedPeriodicalBindings());
        result.add(new SchedulerBindings());
        result.add(new GenericInitializerBindings());
        result.add(new SystemStatsModule(configuration.isDisableNativeSystemStatsCollector()));

        return result;
    }

    protected void annotateProvisionException(ProvisionException e) {
        annotateInjectorExceptions(e.getErrorMessages());
        throw e;
    }

    protected abstract Class<? extends Runnable> shutdownHook();
}
