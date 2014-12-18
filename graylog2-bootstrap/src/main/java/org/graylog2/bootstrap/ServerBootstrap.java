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
package org.graylog2.bootstrap;

import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.ProvisionException;
import io.airlift.airline.Option;
import org.graylog2.plugin.BaseConfiguration;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.inject.Graylog2Module;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.shared.bindings.GenericBindings;
import org.graylog2.shared.bindings.InstantiationService;
import org.graylog2.shared.bindings.PluginBindings;
import org.graylog2.shared.initializers.ServiceManagerListener;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public abstract class ServerBootstrap extends CmdLineTool {
    private static final Logger LOG = LoggerFactory.getLogger(ServerBootstrap.class);

    public ServerBootstrap(String commandName, BaseConfiguration configuration) {
        super(commandName, configuration);
        this.commandName = commandName;
    }

    @Option(name = {"-p", "--pidfile"}, description = "File containing the PID of Graylog2")
    private String pidFile = TMPDIR + FILE_SEPARATOR + "graylog2.pid";

    @Option(name = {"-np", "--no-pid-file"}, description = "Do not write a PID file (overrides -p/--pidfile)")
    private boolean noPidFile = false;

    protected abstract void startNodeRegistration(Injector injector);

    public String getPidFile() {
        return pidFile;
    }

    public boolean isNoPidFile() {
        return noPidFile;
    }

    @Override
    protected void startCommand() {
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
            annotateProvisionException(e);
            System.exit(-1);
            return;
        } catch (Exception e) {
            LOG.error("Unexpected exception", e);
            System.exit(-1);
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(injector.getInstance(shutdownHook())));

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
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            return;
        }
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

    @Override
    protected List<Module> getSharedBindingsModules(InstantiationService instantiationService) {
        final List<Module> result = super.getSharedBindingsModules(instantiationService);
        result.add(new GenericBindings(instantiationService));
        Reflections reflections = new Reflections("org.graylog2.shared.bindings");
        final Set<Class<? extends AbstractModule>> generic = reflections.getSubTypesOf(AbstractModule.class);
        final Set<Class<? extends Graylog2Module>> gl2Modules = reflections.getSubTypesOf(Graylog2Module.class);
        for (Class<? extends Module> type : Iterables.concat(generic, gl2Modules)) {
            // skip the some modules, because we have already instantiated it above, avoids a bogus log message
            if (type.equals(GenericBindings.class) || type.equals(PluginBindings.class)) {
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
        return result;
    }

    protected void annotateProvisionException(ProvisionException e) {
        annotateInjectorExceptions(e.getErrorMessages());
        throw e;
    }

    protected abstract Class<? extends Runnable> shutdownHook();
}
