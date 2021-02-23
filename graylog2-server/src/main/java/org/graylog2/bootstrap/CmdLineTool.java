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

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jmx.JmxReporter;
import com.codahale.metrics.log4j2.InstrumentedAppender;
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
import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
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
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.graylog2.plugin.BaseConfiguration;
import org.graylog2.plugin.DocsHelper;
import org.graylog2.plugin.Plugin;
import org.graylog2.plugin.PluginConfigBean;
import org.graylog2.plugin.PluginLoaderConfig;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.PluginModule;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.Version;
import org.graylog2.plugin.system.NodeIdPersistenceException;
import org.graylog2.shared.UI;
import org.graylog2.shared.bindings.GuiceInjectorHolder;
import org.graylog2.shared.bindings.PluginBindings;
import org.graylog2.shared.plugins.ChainingClassLoader;
import org.graylog2.shared.plugins.PluginLoader;
import org.graylog2.shared.utilities.ExceptionUtils;
import org.graylog2.storage.UnsupportedElasticsearchException;
import org.graylog2.storage.versionprobe.ElasticsearchProbeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.nio.file.AccessDeniedException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Strings.nullToEmpty;

public abstract class CmdLineTool implements CliCommand {
    static {
        // Set up JDK Logging adapter, https://logging.apache.org/log4j/2.x/log4j-jul/index.html
        System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
    }

    private static final Logger LOG = LoggerFactory.getLogger(CmdLineTool.class);

    protected static final Version version = Version.CURRENT_CLASSPATH;
    protected static final String FILE_SEPARATOR = System.getProperty("file.separator");
    protected static final String TMPDIR = System.getProperty("java.io.tmpdir", "/tmp");

    protected final JadConfig jadConfig;
    protected final BaseConfiguration configuration;
    protected final ChainingClassLoader chainingClassLoader;

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
        this.chainingClassLoader = new ChainingClassLoader(this.getClass().getClassLoader());
    }


    /**
     * Validate the given configuration for this command.
     *
     * @return {@code true} if the configuration is valid, {@code false}.
     */
    protected boolean validateConfiguration() {
        return true;
    }

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

    /**
     * Things that have to run before the {@link #startCommand()} method is being called.
     */
    protected void beforeStart() {}

    @Override
    public void run() {
        final Level logLevel = setupLogger();

        final PluginBindings pluginBindings = installPluginConfigAndBindings(getPluginPath(configFile), chainingClassLoader);

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

        beforeStart();

        final List<String> arguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
        LOG.info("Running with JVM arguments: {}", Joiner.on(' ').join(arguments));

        injector = setupInjector(configModule, pluginBindings, binder -> binder.bind(ChainingClassLoader.class).toInstance(chainingClassLoader));

        if (injector == null) {
            LOG.error("Injector could not be created, exiting! (Please include the previous error messages in bug reports.)");
            System.exit(1);
        }

        // This is holding all our metrics.
        final MetricRegistry metrics = injector.getInstance(MetricRegistry.class);

        addInstrumentedAppender(metrics, logLevel);

        // Report metrics via JMX.
        final JmxReporter reporter = JmxReporter.forRegistry(metrics).build();
        reporter.start();

        startCommand();
    }

    protected abstract void startCommand();

    protected Level setupLogger() {
        final Level logLevel;
        if (isDebug()) {
            LOG.info("Running in Debug mode");
            logLevel = Level.DEBUG;

            // Enable logging for Netty when running in debug mode.
            InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE);
        } else if (onlyLogErrors()) {
            logLevel = Level.ERROR;
        } else {
            logLevel = Level.INFO;
        }

        initializeLogging(logLevel);

        return logLevel;
    }

    private void initializeLogging(final Level logLevel) {
        final LoggerContext context = (LoggerContext) LogManager.getContext(false);
        final org.apache.logging.log4j.core.config.Configuration config = context.getConfiguration();

        config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME).setLevel(logLevel);
        config.getLoggerConfig(Main.class.getPackage().getName()).setLevel(logLevel);

        context.updateLoggers(config);
    }

    private void addInstrumentedAppender(final MetricRegistry metrics, final Level level) {
        final InstrumentedAppender appender = new InstrumentedAppender(metrics, null, null, false);
        appender.start();

        final LoggerContext context = (LoggerContext) LogManager.getContext(false);
        final org.apache.logging.log4j.core.config.Configuration config = context.getConfiguration();
        config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME).addAppender(appender, level, null);
        context.updateLoggers(config);
    }

    protected boolean onlyLogErrors() {
        return false;
    }

    private void dumpCurrentConfigAndExit() {
        System.out.println(dumpConfiguration(jadConfig.dump()));
        System.exit(0);
    }

    private void dumpDefaultConfigAndExit() {
        for (Object bean : getCommandConfigurationBeans()) {
            jadConfig.addConfigurationBean(bean);
        }
        dumpCurrentConfigAndExit();
    }

    private PluginBindings installPluginConfigAndBindings(Path pluginPath, ChainingClassLoader classLoader) {
        final Set<Plugin> plugins = loadPlugins(pluginPath, classLoader);
        final PluginBindings pluginBindings = new PluginBindings(plugins, configuration);
        for (final Plugin plugin : plugins) {
            for (final PluginModule pluginModule : plugin.modules(configuration)) {
                for (final PluginConfigBean configBean : pluginModule.getConfigBeans()) {
                    jadConfig.addConfigurationBean(configBean);
                }
            }

        }
        return pluginBindings;
    }

    private Path getPluginPath(String configFile) {
        final PluginLoaderConfig pluginLoaderConfig = new PluginLoaderConfig();
        processConfiguration(new JadConfig(getConfigRepositories(configFile), pluginLoaderConfig));

        return pluginLoaderConfig.getPluginDir();
    }

    protected Set<Plugin> loadPlugins(Path pluginPath, ChainingClassLoader chainingClassLoader) {
        final Set<Plugin> plugins = new HashSet<>();

        final PluginLoader pluginLoader = new PluginLoader(pluginPath.toFile(), chainingClassLoader);
        for (Plugin plugin : pluginLoader.loadPlugins()) {
            final PluginMetaData metadata = plugin.metadata();
            if (capabilities().containsAll(metadata.getRequiredCapabilities())) {
                if (version.sameOrHigher(metadata.getRequiredVersion())) {
                    LOG.info("Loaded plugin: {}", plugin);
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

        return plugins;
    }

    protected Collection<Repository> getConfigRepositories(String configFile) {
        return Arrays.asList(
                new EnvironmentRepository("GRAYLOG_"),
                new SystemPropertiesRepository("graylog."),
                // Legacy prefixes
                new EnvironmentRepository("GRAYLOG2_"),
                new SystemPropertiesRepository("graylog2."),
                new PropertiesRepository(configFile)
        );
    }

    private String dumpConfiguration(final Map<String, String> configMap) {
        final StringBuilder sb = new StringBuilder();
        sb.append("# Configuration of graylog2-").append(commandName).append(" ").append(version).append(System.lineSeparator());
        sb.append("# Generated on ").append(Tools.nowUTC()).append(System.lineSeparator());

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
        processConfiguration(jadConfig);

        return new NamedConfigParametersModule(jadConfig.getConfigurationBeans());
    }

    private void processConfiguration(JadConfig jadConfig) {
        try {
            jadConfig.process();
        } catch (RepositoryException e) {
            LOG.error("Couldn't load configuration: {}", e.getMessage());
            System.exit(1);
        } catch (ParameterException | ValidationException e) {
            LOG.error("Invalid configuration", e);
            System.exit(1);
        }
    }

    protected List<Module> getSharedBindingsModules() {
        return Lists.newArrayList();
    }

    protected Injector setupInjector(NamedConfigParametersModule configModule, Module... otherModules) {
        try {
            final ImmutableList.Builder<Module> modules = ImmutableList.builder();
            modules.add(configModule);
            modules.addAll(getSharedBindingsModules());
            modules.addAll(getCommandBindings());
            modules.addAll(Arrays.asList(otherModules));
            modules.add(new Module() {
                @Override
                public void configure(Binder binder) {
                    binder.bind(String.class).annotatedWith(Names.named("BootstrapCommand")).toInstance(commandName);
                }
            });

            return GuiceInjectorHolder.createInjector(modules.build());
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
            } else if (rootCause instanceof UnsupportedElasticsearchException) {
                final Version elasticsearchVersion = ((UnsupportedElasticsearchException) rootCause).getElasticsearchMajorVersion();
                LOG.error(UI.wallString("Unsupported Elasticsearch version: " + elasticsearchVersion, DocsHelper.PAGE_ES_VERSIONS.toString()));
                System.exit(-3);
            } else if (rootCause instanceof ElasticsearchProbeException) {
                LOG.error(UI.wallString(rootCause.getMessage(), DocsHelper.PAGE_ES_CONFIGURATION.toString()));
                System.exit(-4);
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
