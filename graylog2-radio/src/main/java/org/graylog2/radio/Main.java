/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.radio;

import com.beust.jcommander.JCommander;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.log4j.InstrumentedAppender;
import com.github.joschi.jadconfig.JadConfig;
import com.github.joschi.jadconfig.ParameterException;
import com.github.joschi.jadconfig.RepositoryException;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.jodatime.JodaTimeConverterFactory;
import com.github.joschi.jadconfig.repositories.EnvironmentRepository;
import com.github.joschi.jadconfig.repositories.PropertiesRepository;
import com.github.joschi.jadconfig.repositories.SystemPropertiesRepository;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Injector;
import com.google.inject.Module;
import org.apache.log4j.Level;
import org.graylog2.plugin.Plugin;
import org.graylog2.plugin.PluginModule;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Tools;
import org.graylog2.radio.bindings.RadioBindings;
import org.graylog2.radio.bindings.RadioInitializerBindings;
import org.graylog2.radio.cluster.Ping;
import org.graylog2.shared.NodeRunner;
import org.graylog2.shared.bindings.GuiceInjectorHolder;
import org.graylog2.shared.bindings.GuiceInstantiationService;
import org.graylog2.shared.initializers.ServiceManagerListener;
import org.graylog2.shared.plugins.PluginLoader;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Strings.nullToEmpty;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class Main extends NodeRunner {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);
    private static final String ENVIRONMENT_PREFIX = "GRAYLOG2_";
    private static final String PROPERTIES_PREFIX = "graylog2.";

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        final CommandLineArguments commandLineArguments = new CommandLineArguments();
        final JCommander jCommander = new JCommander(commandLineArguments, args);
        jCommander.setProgramName("graylog2-radio");

        if (commandLineArguments.isShowHelp()) {
            jCommander.usage();
            System.exit(0);
        }

        if (commandLineArguments.isShowVersion()) {
            System.out.println("Graylog2 Radio " + RadioVersion.VERSION);
            System.out.println("JRE: " + Tools.getSystemInformation());
            System.exit(0);
        }

        if(commandLineArguments.isDumpDefaultConfig()) {
            final JadConfig jadConfig = new JadConfig();
            jadConfig.addConverterFactory(new JodaTimeConverterFactory());
            jadConfig.addConfigurationBean(new Configuration());
            System.out.println(dumpConfiguration(jadConfig.dump()));
            System.exit(0);
        }

        final JadConfig jadConfig = new JadConfig();
        jadConfig.addConverterFactory(new JodaTimeConverterFactory());
        final Configuration configuration = readConfiguration(jadConfig, commandLineArguments.getConfigFile());

        if (commandLineArguments.isDumpConfig()) {
            System.out.println(dumpConfiguration(jadConfig.dump()));
            System.exit(0);
        }

        // Are we in debug mode?
        Level logLevel = Level.INFO;
        if (commandLineArguments.isDebug()) {
            LOG.info("Running in Debug mode");
            logLevel = Level.DEBUG;
        }

        PluginLoader pluginLoader = new PluginLoader(new File(configuration.getPluginDir()));
        List<PluginModule> pluginModules = Lists.newArrayList();
        for (Plugin plugin : pluginLoader.loadPlugins())
            pluginModules.addAll(plugin.modules());

        LOG.debug("Loaded modules: " + pluginModules);

        GuiceInstantiationService instantiationService = new GuiceInstantiationService();
        List<Module> bindingsModules = getBindingsModules(instantiationService,
                new RadioBindings(configuration),
                new RadioInitializerBindings());
        LOG.debug("Adding plugin modules: " + pluginModules);
        bindingsModules.addAll(pluginModules);
        final Injector injector = GuiceInjectorHolder.createInjector(bindingsModules);
        instantiationService.setInjector(injector);

        // This is holding all our metrics.
        final MetricRegistry metrics = injector.getInstance(MetricRegistry.class);

        // Report metrics via JMX.
        final JmxReporter reporter = JmxReporter.forRegistry(metrics).build();
        reporter.start();

        InstrumentedAppender logMetrics = new InstrumentedAppender(metrics);
        logMetrics.activateOptions();
        org.apache.log4j.Logger.getRootLogger().setLevel(logLevel);
        org.apache.log4j.Logger.getLogger(Main.class.getPackage().getName()).setLevel(logLevel);
        org.apache.log4j.Logger.getRootLogger().addAppender(logMetrics);

        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        LOG.info("Graylog2 Radio {} starting up. (JRE: {})", RadioVersion.VERSION, Tools.getSystemInformation());

        // Do not use a PID file if the user requested not to
        if (!commandLineArguments.isNoPidFile()) {
            savePidFile(commandLineArguments.getPidFile());
        }

        final ServerStatus serverStatus = injector.getInstance(ServerStatus.class);
        serverStatus.initialize();

        // register node by initiating first ping. if the node isn't registered, loading persisted inputs will fail silently, for example
        Ping.Pinger pinger = injector.getInstance(Ping.Pinger.class);
        pinger.ping();

        final ServiceManager serviceManager = injector.getInstance(ServiceManager.class);
        final ServiceManagerListener serviceManagerListener = injector.getInstance(ServiceManagerListener.class);
        serviceManager.addListener(serviceManagerListener, MoreExecutors.sameThreadExecutor());
        serviceManager.startAsync().awaitHealthy();

        LOG.info("Graylog2 Radio up and running.");

        while (true) {
            try { Thread.sleep(1000); } catch (InterruptedException e) { /* lol, i don't care */ }
        }
    }

    private static Configuration readConfiguration(final JadConfig jadConfig, final String configFile) {
        final Configuration configuration = new Configuration();

        jadConfig.addConfigurationBean(configuration);
        jadConfig.setRepositories(Arrays.asList(
                new EnvironmentRepository(ENVIRONMENT_PREFIX),
                new SystemPropertiesRepository(PROPERTIES_PREFIX),
                new PropertiesRepository(configFile)
        ));

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

        return configuration;
    }

    private static String dumpConfiguration(final Map<String, String> configMap) {
        final StringBuilder sb = new StringBuilder();
        sb.append("# Configuration of graylog2-radio ").append(RadioVersion.VERSION).append(System.lineSeparator());
        sb.append("# Generated on ").append(Tools.iso8601()).append(System.lineSeparator());

        for (Map.Entry<String, String> entry : configMap.entrySet()) {
            sb.append(entry.getKey()).append('=').append(nullToEmpty(entry.getValue())).append(System.lineSeparator());
        }

        return sb.toString();
    }
}
