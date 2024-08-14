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
package org.graylog.datanode.bootstrap;

import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.ProvisionException;
import org.graylog.datanode.Configuration;
import org.graylog.datanode.bindings.ConfigurationModule;
import org.graylog.datanode.bindings.DatanodeConfigurationBindings;
import org.graylog.datanode.bindings.GenericBindings;
import org.graylog.datanode.bindings.GenericInitializerBindings;
import org.graylog.datanode.bindings.OpensearchProcessBindings;
import org.graylog.datanode.bindings.PreflightChecksBindings;
import org.graylog.datanode.bindings.SchedulerBindings;
import org.graylog.datanode.bootstrap.preflight.PreflightClusterConfigurationModule;
import org.graylog2.bindings.NamedConfigParametersOverrideModule;
import org.graylog2.bootstrap.preflight.PreflightCheckService;
import org.graylog2.plugin.Plugin;
import org.graylog2.plugin.Tools;
import org.graylog2.shared.bindings.IsDevelopmentBindings;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
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
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;

public abstract class ServerBootstrap extends DatanodeCmdLineTool {
    private static final Logger LOG = LoggerFactory.getLogger(ServerBootstrap.class);
    protected Configuration configuration;

    protected ServerBootstrap(String commandName, Configuration configuration) {
        super(commandName);
        this.commandName = commandName;
        this.configuration = configuration;
    }

    protected abstract void startNodeRegistration(Injector injector);

    @Override
    protected void beforeInjectorCreation(Set<Plugin> plugins) {
        runPreFlightChecks(plugins);
    }

    private void runPreFlightChecks(Set<Plugin> plugins) {
        if (configuration.getSkipPreflightChecks()) {
            LOG.info("Skipping preflight checks");
            return;
        }

        final List<Module> preflightCheckModules = plugins.stream().map(Plugin::preflightCheckModules)
                .flatMap(Collection::stream).collect(Collectors.toList());

        getPreflightInjector(preflightCheckModules).getInstance(PreflightCheckService.class).runChecks();
    }

    private Injector getPreflightInjector(List<Module> preflightCheckModules) {
        return Guice.createInjector(
                new IsDevelopmentBindings(),
                new PreflightClusterConfigurationModule(chainingClassLoader),
                new NamedConfigParametersOverrideModule(jadConfig.getConfigurationBeans()),
                new ConfigurationModule(configuration),
                new PreflightChecksBindings(),
                new DatanodeConfigurationBindings(),
        new Module() {
                    @Override
                    public void configure(Binder binder) {
                        preflightCheckModules.forEach(binder::install);
                    }
                });
    }

    private void setNettyNativeDefaults(Configuration configuration) {
        // Give netty a better spot than /tmp to unpack its tcnative libraries
        if (System.getProperty("io.netty.native.workdir") == null) {
            System.setProperty("io.netty.native.workdir", configuration.getNativeLibDir().toAbsolutePath().toString());
        }
        // Don't delete the native lib after unpacking, as this confuses needrestart(1) on some distributions
        if (System.getProperty("io.netty.native.deleteLibAfterLoading") == null) {
            System.setProperty("io.netty.native.deleteLibAfterLoading", "false");
        }
    }

    @Override
    protected void startCommand() {
        final String systemInformation = Tools.getSystemInformation();

        final OS os = OS.getOs();

        LOG.info("Graylog {} {} starting up", commandName, version);
        LOG.info("JRE: {}", systemInformation);
        LOG.info("Deployment: {}", configuration.getInstallationSource());
        LOG.info("OS: {}", os.getPlatformName());
        LOG.info("Arch: {}", os.getArch());

        startNodeRegistration(injector);

        final ActivityWriter activityWriter;
        final ServiceManager serviceManager;
        try {
            activityWriter = injector.getInstance(ActivityWriter.class);
            serviceManager = injector.getInstance(ServiceManager.class);
        } catch (ProvisionException e) {
            LOG.error("Guice error", e);
            annotateProvisionException(e);
            System.exit(-1);
            return;
        } catch (Exception e) {
            LOG.error("Unexpected exception", e);
            System.exit(-1);
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(injector.getInstance(shutdownHook())));

        // Start services.
        try {
            serviceManager.startAsync().awaitHealthy();
        } catch (Exception e) {
            try {
                serviceManager.stopAsync().awaitStopped(configuration.getShutdownTimeout(), TimeUnit.MILLISECONDS);
            } catch (TimeoutException timeoutException) {
                LOG.error("Unable to shutdown properly on time. {}", serviceManager.servicesByState());
            }
            LOG.error("Graylog DataNode startup failed. Exiting. Exception was:", e);
            System.exit(-1);
        }
        LOG.info("Services started, startup times in ms: {}", serviceManager.startupTimes());

        activityWriter.write(new Activity("Started up.", Main.class));
        LOG.info("Graylog DataNode {} up and running.", commandName);

        // Block forever.
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
        }
    }

    public void runMigrations() {
        LOG.info("Running {} migrations...", 0);
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
        result.add(new GenericBindings(isMigrationCommand()));
        result.add(new SchedulerBindings());
        result.add(new GenericInitializerBindings());
        result.add(new OpensearchProcessBindings());
        result.add(new DatanodeConfigurationBindings());

        return result;
    }

    protected void annotateProvisionException(ProvisionException e) {
        annotateInjectorExceptions(e.getErrorMessages());
        throw e;
    }

    protected abstract Class<? extends Runnable> shutdownHook();
}
