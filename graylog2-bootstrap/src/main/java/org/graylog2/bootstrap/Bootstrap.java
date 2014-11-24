/*
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.graylog2.bootstrap;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.log4j.InstrumentedAppender;
import com.github.joschi.jadconfig.JadConfig;
import com.github.joschi.jadconfig.guice.NamedConfigParametersModule;
import com.github.joschi.jadconfig.jodatime.JodaTimeConverterFactory;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.ProvisionException;
import org.apache.log4j.Level;
import org.graylog2.plugin.BaseConfiguration;
import org.graylog2.plugin.Plugin;
import org.graylog2.plugin.PluginModule;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.Version;
import org.graylog2.plugin.inject.Graylog2Module;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.radio.cluster.Ping;
import org.graylog2.shared.bindings.GenericBindings;
import org.graylog2.shared.bindings.InstantiationService;
import org.graylog2.shared.initializers.ServiceManagerListener;
import org.graylog2.shared.plugins.PluginLoader;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public abstract class Bootstrap implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(Bootstrap.class);
    protected static final String ENVIRONMENT_PREFIX = "GRAYLOG2_";
    protected static final String PROPERTIES_PREFIX = "graylog2.";
    protected static final Version version = Version.CURRENT_CLASSPATH;
    protected static final String FILE_SEPARATOR = System.getProperty("file.separator");
    protected static final String TMPDIR = System.getProperty("java.io.tmpdir", "/tmp");

    private final String commandName;
    private final BaseConfiguration configuration;

    public Bootstrap(String commandName, BaseConfiguration configuration) {
        this.commandName = commandName;
        this.configuration = configuration;
    }

    protected abstract Injector setupInjector(NamedConfigParametersModule configModule, List<PluginModule> pluginModules);
    protected abstract NamedConfigParametersModule readConfiguration(final JadConfig jadConfig, final String configFile);
    protected abstract void startNodeRegistration(Injector injector);

    protected abstract String getConfigFile();
    protected abstract String getPidFile();
    protected abstract boolean isNoPidFile();
    protected abstract boolean isDebug();

    @Override
    public void run() {
        final JadConfig jadConfig = new JadConfig();
        jadConfig.addConverterFactory(new JodaTimeConverterFactory());
        final NamedConfigParametersModule configModule = readConfiguration(jadConfig, getConfigFile());

        // Are we in debug mode?
        Level logLevel = Level.INFO;
        if (isDebug()) {
            LOG.info("Running in Debug mode");
            logLevel = Level.DEBUG;
        }
        org.apache.log4j.Logger.getRootLogger().setLevel(logLevel);
        org.apache.log4j.Logger.getLogger(Main.class.getPackage().getName()).setLevel(logLevel);

        PluginLoader pluginLoader = new PluginLoader(new File(configuration.getPluginDir()));
        List<PluginModule> pluginModules = Lists.newArrayList();
        for (Plugin plugin : pluginLoader.loadPlugins())
            pluginModules.addAll(plugin.modules());

        LOG.debug("Loaded modules: " + pluginModules);

        final Injector injector = setupInjector(configModule, pluginModules);

        if (injector == null) {
            LOG.error("Injector could not be created, exiting! (Please include the previous stacktraces in bug reports.)");
            System.exit(1);
        }

        // This is holding all our metrics.
        final MetricRegistry metrics = injector.getInstance(MetricRegistry.class);

        // Report metrics via JMX.
        final JmxReporter reporter = JmxReporter.forRegistry(metrics).build();
        reporter.start();

        InstrumentedAppender logMetrics = new InstrumentedAppender(metrics);
        logMetrics.activateOptions();
        org.apache.log4j.Logger.getRootLogger().addAppender(logMetrics);

        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        LOG.info("Graylog2 " + commandName + " {} starting up. (JRE: {})", version, Tools.getSystemInformation());

        // Do not use a PID file if the user requested not to
        if (!isNoPidFile()) {
            savePidFile(getPidFile());
        }

        final ServerStatus serverStatus = injector.getInstance(ServerStatus.class);
        serverStatus.initialize();

        startNodeRegistration(injector);

        final ActivityWriter activityWriter;
        final ServiceManager serviceManager;
        try {
            activityWriter = injector.getInstance(ActivityWriter.class);
            serviceManager = injector.getInstance(ServiceManager.class);
        } catch (ProvisionException e) {
            LOG.error("Guice error", e);
            System.exit(-1);
            return;
        } catch (Exception e) {
            LOG.error("Unexpected exception", e);
            System.exit(-1);
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                String msg = "SIGNAL received. Shutting down.";
                LOG.info(msg);
                activityWriter.write(new Activity(msg, Main.class));

                serviceManager.stopAsync().awaitStopped();
            }
        });

        // propagate default size to input plugins
        MessageInput.setDefaultRecvBufferSize(configuration.getUdpRecvBufferSizes());

        // Start services.
        final ServiceManagerListener serviceManagerListener = injector.getInstance(ServiceManagerListener.class);
        serviceManager.addListener(serviceManagerListener);
        try {
            serviceManager.startAsync().awaitHealthy();
        } catch (Exception e) {
            try {
                serviceManager.stopAsync().awaitStopped(configuration.getShutdownTimeout(), TimeUnit.MILLISECONDS);
            } catch (TimeoutException timeoutException) {
                LOG.error("Unable to shutdown properly on time. {}", serviceManager.servicesByState());
            }
            LOG.error("Graylog2 startup failed. Exiting. Exception was:", e);
            System.exit(-1);
        }
        LOG.info("Services started, startup times in ms: {}", serviceManager.startupTimes());

        activityWriter.write(new Activity("Started up.", Main.class));
        LOG.info("Graylog2 " + commandName + " up and running.");

        // Block forever.
        try {
            while (true) {
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            return;
        }
    }

    protected List<Module> getBindingsModules(InstantiationService instantiationService, Module... specificModules) {
        List<Module> result = Lists.newArrayList();
        result.add(new GenericBindings(instantiationService));
        Reflections reflections = new Reflections("org.graylog2.shared.bindings");
        final Set<Class<? extends AbstractModule>> generic = reflections.getSubTypesOf(AbstractModule.class);
        final Set<Class<? extends Graylog2Module>> gl2Modules = reflections.getSubTypesOf(Graylog2Module.class);
        for (Class<? extends Module> type : Iterables.concat(generic, gl2Modules)) {
            // skip the GenericBindings module, because we have already instantiated it above, avoids a bogus log message
            if (type.equals(GenericBindings.class)) {
                continue;
            }
            try {
                Constructor<? extends Module> constructor = type.getConstructor();
                Module module = constructor.newInstance();
                result.add(module);
            } catch (InstantiationException | InvocationTargetException | IllegalAccessException e) {
                LOG.error("Unable to instantiate Module {}: {}", type, e);
            } catch (NoSuchMethodException e) {
                LOG.info("No constructor found for guice module {}", type);
            }
        }

        result.addAll(Arrays.asList(specificModules));
        return result;
    }

    protected void savePidFile(final String pidFile) {
        final String pid = Tools.getPID();
        final Path pidFilePath = Paths.get(pidFile);
        pidFilePath.toFile().deleteOnExit();

        try {
            if (pid == null || pid.isEmpty() || pid.equals("unknown")) {
                throw new Exception("Could not determine PID.");
            }

            Files.write(pidFilePath, pid.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            LOG.error("Could not write PID file: " + e.getMessage(), e);
            System.exit(1);
        }
    }
}
