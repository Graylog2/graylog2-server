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
package org.graylog2.bootstrap;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.log4j.InstrumentedAppender;
import com.github.joschi.jadconfig.JadConfig;
import com.github.joschi.jadconfig.ParameterException;
import com.github.joschi.jadconfig.Repository;
import com.github.joschi.jadconfig.RepositoryException;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.guava.GuavaConverterFactory;
import com.github.joschi.jadconfig.guice.NamedConfigParametersModule;
import com.github.joschi.jadconfig.jodatime.JodaTimeConverterFactory;
import com.github.joschi.jadconfig.repositories.EnvironmentRepository;
import com.github.joschi.jadconfig.repositories.PropertiesRepository;
import com.github.joschi.jadconfig.repositories.SystemPropertiesRepository;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Binder;
import com.google.inject.CreationException;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.name.Names;
import com.google.inject.spi.Message;
import io.airlift.airline.Command;
import io.airlift.airline.Option;
import org.apache.log4j.Level;
import org.graylog2.UI;
import org.graylog2.plugin.BaseConfiguration;
import org.graylog2.plugin.Plugin;
import org.graylog2.plugin.PluginConfigBean;
import org.graylog2.plugin.PluginLoaderConfig;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.PluginModule;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.Version;
import org.graylog2.plugin.system.NodeIdPersistenceException;
import org.graylog2.shared.bindings.GuiceInjectorHolder;
import org.graylog2.shared.bindings.GuiceInstantiationService;
import org.graylog2.shared.bindings.InstantiationService;
import org.graylog2.shared.bindings.PluginBindings;
import org.graylog2.shared.plugins.PluginLoader;
import org.graylog2.shared.utilities.ExceptionUtils;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.logging.Slf4JLoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.nio.file.AccessDeniedException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Strings.nullToEmpty;

public abstract class CmdLineTool implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(CmdLineTool.class);

    protected static final String ENVIRONMENT_PREFIX = "GRAYLOG2_";
    protected static final String PROPERTIES_PREFIX = "graylog2.";
    protected static final Version version = Version.CURRENT_CLASSPATH;
    protected static final String FILE_SEPARATOR = System.getProperty("file.separator");
    protected static final String TMPDIR = System.getProperty("java.io.tmpdir", "/tmp");

    protected final JadConfig jadConfig;
    protected final BaseConfiguration configuration;

    @Option(name = "--dump-config", description = "Show the effective Graylog configuration and exit")
    protected boolean dumpConfig = false;

    @Option(name = "--dump-default-config", description = "Show the default configuration and exit")
    protected boolean dumpDefaultConfig = false;

    @Option(name = {"-d", "--debug"}, description = "Run Graylog in debug mode")
    private boolean debug = false;

    @Option(name = {"-f", "--configfile"}, description = "Configuration file for Graylog")
    private String configFile = "/etc/graylog/server/server.conf";

    protected String commandName = "command";

    protected Injector injector;

    protected CmdLineTool(BaseConfiguration configuration) {
        this(null, configuration);
    }

    protected CmdLineTool(String commandName, BaseConfiguration configuration) {
        jadConfig = new JadConfig();
        jadConfig.addConverterFactory(new GuavaConverterFactory());
        jadConfig.addConverterFactory(new JodaTimeConverterFactory());

        if (commandName == null) {
            if (this.getClass().isAnnotationPresent(Command.class)) {
                this.commandName = this.getClass().getAnnotation(Command.class).name();
            } else {
                this.commandName = "tool";
            }
        } else {
            this.commandName = commandName;
        }
        this.configuration = configuration;
    }


    protected abstract boolean validateConfiguration();

    public boolean isDumpConfig() {
        return dumpConfig;
    }

    public boolean isDumpDefaultConfig() {
        return dumpDefaultConfig;
    }

    public boolean isDebug() {
        return debug;
    }

    protected abstract List<Module> getCommandBindings();

    protected abstract List<Object> getCommandConfigurationBeans();


    @Override
    public void run() {
        setupLogger();

        final PluginBindings pluginBindings = installPluginConfigAndBindings(getPluginPath(configFile));

        if (isDumpDefaultConfig()) {
            dumpDefaultConfigAndExit();
        }

        final NamedConfigParametersModule configModule = readConfiguration(configFile);

        if (isDumpConfig()) {
            dumpCurrentConfigAndExit();
        }

        if (!validateConfiguration()) {
            LOG.error("Validating configuration file failed - exiting.");
            System.exit(1);
        }

        final List<String> arguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
        LOG.info("Running with JVM arguments: {}", Joiner.on(' ').join(arguments));

        injector = setupInjector(configModule, pluginBindings);

        if (injector == null) {
            LOG.error("Injector could not be created, exiting! (Please include the previous error messages in bug reports.)");
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

        startCommand();
    }

    protected abstract void startCommand();

    protected void setupLogger() {
        // Are we in debug mode?
        Level logLevel = Level.INFO;
        if (isDebug()) {
            LOG.info("Running in Debug mode");
            logLevel = Level.DEBUG;

            // Enable logging for Netty when running in debug mode.
            InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory());
        } else if (onlyLogErrors()) {
            logLevel = Level.ERROR;
        }
        org.apache.log4j.Logger.getRootLogger().setLevel(logLevel);
        org.apache.log4j.Logger.getLogger("org.graylog2").setLevel(logLevel);
    }

    protected boolean onlyLogErrors() {
        return false;
    }

    private void dumpCurrentConfigAndExit() {
        System.out.println(dumpConfiguration(jadConfig.dump()));
        System.exit(0);
    }

    private void dumpDefaultConfigAndExit() {
        for (Object bean : getCommandConfigurationBeans())
            jadConfig.addConfigurationBean(bean);
        dumpCurrentConfigAndExit();
    }

    private PluginBindings installPluginConfigAndBindings(String pluginPath) {
        final Set<Plugin> plugins = loadPlugins(pluginPath);
        final PluginBindings pluginBindings = new PluginBindings(plugins);
        for (final Plugin plugin : plugins) {
            for (final PluginModule pluginModule : plugin.modules()) {
                for (final PluginConfigBean configBean : pluginModule.getConfigBeans()) {
                    jadConfig.addConfigurationBean(configBean);
                }
            }

        }
        return pluginBindings;
    }

    private String getPluginPath(String configFile) {
        PluginLoaderConfig pluginLoaderConfig = new PluginLoaderConfig();
        JadConfig jadConfig = new JadConfig(getConfigRepositories(configFile), pluginLoaderConfig);

        try {
            jadConfig.process();
        } catch (RepositoryException e) {
            LOG.error("Couldn't load configuration: {}", e.getMessage());
            System.exit(1);
        } catch (ParameterException | ValidationException e) {
            LOG.error("Invalid configuration", e);
            System.exit(1);
        }

        return pluginLoaderConfig.getPluginDir();
    }

    protected Set<Plugin> loadPlugins(String pluginPath) {
        final File pluginDir = new File(pluginPath);
        final Set<Plugin> plugins = new HashSet<>();

        final PluginLoader pluginLoader = new PluginLoader(pluginDir);
        for (Plugin plugin : pluginLoader.loadPlugins()) {
            final PluginMetaData metadata = plugin.metadata();
            if (capabilities().containsAll(metadata.getRequiredCapabilities())) {
                if (version.sameOrHigher(metadata.getRequiredVersion())) {
                    plugins.add(plugin);
                } else {
                    LOG.error("Plugin \"" + metadata.getName() + "\" requires version " + metadata.getRequiredVersion() + " - not loading!");
                }
            } else {
                LOG.debug("Skipping plugin \"{}\" because some capabilities are missing ({}).",
                        metadata.getName(),
                        Sets.difference(plugin.metadata().getRequiredCapabilities(), capabilities()));
            }
        }

        LOG.info("Loaded plugins: " + plugins);
        return plugins;
    }

    protected Collection<Repository> getConfigRepositories(String configFile) {
        return Arrays.asList(
                new EnvironmentRepository(ENVIRONMENT_PREFIX),
                new SystemPropertiesRepository(PROPERTIES_PREFIX),
                new PropertiesRepository(configFile)
        );
    }

    private String dumpConfiguration(final Map<String, String> configMap) {
        final StringBuilder sb = new StringBuilder();
        sb.append("# Configuration of graylog2-").append(commandName).append(" ").append(version).append(System.lineSeparator());
        sb.append("# Generated on ").append(Tools.iso8601()).append(System.lineSeparator());

        for (Map.Entry<String, String> entry : configMap.entrySet()) {
            sb.append(entry.getKey()).append('=').append(nullToEmpty(entry.getValue())).append(System.lineSeparator());
        }

        return sb.toString();
    }

    protected NamedConfigParametersModule readConfiguration(final String configFile) {
        final List<Object> beans = getCommandConfigurationBeans();
        for (Object bean : beans) {
            jadConfig.addConfigurationBean(bean);
        }
        jadConfig.setRepositories(getConfigRepositories(configFile));

        LOG.debug("Loading configuration from config file: {}", configFile);
        try {
            jadConfig.process();
        } catch (RepositoryException e) {
            LOG.error("Couldn't load configuration: {}", e.getMessage());
            System.exit(1);
        } catch (ParameterException | ValidationException e) {
            LOG.error("Invalid configuration", e);
            System.exit(1);
        }

        if (configuration.getRestTransportUri() == null) {
            configuration.setRestTransportUri(configuration.getDefaultRestTransportUri());
            LOG.debug("No rest_transport_uri set. Using default [{}].", configuration.getRestTransportUri());
        }

        return new NamedConfigParametersModule(jadConfig.getConfigurationBeans());
    }

    protected List<Module> getSharedBindingsModules(InstantiationService instantiationService) {
        return Lists.newArrayList();
    }

    protected Injector setupInjector(NamedConfigParametersModule configModule, Module... otherModules) {
        try {
            final GuiceInstantiationService instantiationService = new GuiceInstantiationService();

            final ImmutableList.Builder<Module> modules = ImmutableList.builder();
            modules.add(configModule);
            modules.addAll(getSharedBindingsModules(instantiationService));
            modules.addAll(getCommandBindings());
            modules.addAll(Arrays.asList(otherModules));
            modules.add(new Module() {
                @Override
                public void configure(Binder binder) {
                    binder.bind(String.class).annotatedWith(Names.named("BootstrapCommand")).toInstance(commandName);
                }
            });

            final Injector injector = GuiceInjectorHolder.createInjector(modules.build());
            instantiationService.setInjector(injector);

            return injector;
        } catch (CreationException e) {
            annotateInjectorCreationException(e);
            return null;
        } catch (Exception e) {
            LOG.error("Injector creation failed!", e);
            return null;
        }
    }

    protected void annotateInjectorCreationException(CreationException e) {
        annotateInjectorExceptions(e.getErrorMessages());
        throw e;
    }

    protected void annotateInjectorExceptions(Collection<Message> messages) {
        for (Message message : messages) {
            //noinspection ThrowableResultOfMethodCallIgnored
            final Throwable rootCause = ExceptionUtils.getRootCause(message.getCause());
            if (rootCause instanceof NodeIdPersistenceException) {
                LOG.error(UI.wallString(
                        "Unable to read or persist your NodeId file. This means your node id file (" + configuration.getNodeIdFile() + ") is not readable or writable by the current user. The following exception might give more information: " + message));
                System.exit(-1);
            } else if (rootCause instanceof AccessDeniedException) {
                LOG.error(UI.wallString("Unable to access file " + rootCause.getMessage()));
                System.exit(-2);
            } else {
                // other guice error, still print the raw messages
                // TODO this could potentially print duplicate messages depending on what a subclass does...
                LOG.error("Guice error (more detail on log level debug): {}", message.getMessage());
                if (rootCause != null) {
                    LOG.debug("Stacktrace:", rootCause);
                }
            }
        }
    }

    protected Set<ServerStatus.Capability> capabilities() {
        return Collections.emptySet();
    }
}
