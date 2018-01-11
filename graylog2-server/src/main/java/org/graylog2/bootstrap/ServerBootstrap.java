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

import com.github.rvesse.airline.annotations.Option;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.ProvisionException;
import org.graylog2.audit.AuditActor;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.plugin.BaseConfiguration;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.shared.bindings.GenericBindings;
import org.graylog2.shared.bindings.GenericInitializerBindings;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.graylog2.audit.AuditEventTypes.NODE_STARTUP_COMPLETE;
import static org.graylog2.audit.AuditEventTypes.NODE_STARTUP_INITIATE;

public abstract class ServerBootstrap extends CmdLineTool {
    private static final Logger LOG = LoggerFactory.getLogger(ServerBootstrap.class);

    public ServerBootstrap(String commandName, BaseConfiguration configuration) {
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

    @Override
    protected void beforeStart() {
        super.beforeStart();

        // Do not use a PID file if the user requested not to
        if (!isNoPidFile()) {
            savePidFile(getPidFile());
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
        serviceManager.addListener(serviceManagerListener);
        try {
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
        LOG.info("Graylog " + commandName + " up and running.");
        auditEventSender.success(AuditActor.system(nodeId), NODE_STARTUP_COMPLETE, auditEventContext);

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

        result.add(new GenericBindings());
        result.add(new SecurityBindings());
        result.add(new ServerStatusBindings(capabilities()));
        result.add(new ValidatorModule());
        result.add(new SharedPeriodicalBindings());
        result.add(new SchedulerBindings());
        result.add(new GenericInitializerBindings());
        result.add(new SystemStatsModule(configuration.isDisableSigar()));

        return result;
    }

    protected void annotateProvisionException(ProvisionException e) {
        annotateInjectorExceptions(e.getErrorMessages());
        throw e;
    }

    protected abstract Class<? extends Runnable> shutdownHook();
}
