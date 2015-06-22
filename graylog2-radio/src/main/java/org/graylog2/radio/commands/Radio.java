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
package org.graylog2.radio.commands;

import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Injector;
import com.google.inject.Module;
import io.airlift.airline.Command;
import org.graylog2.bootstrap.Main;
import org.graylog2.bootstrap.ServerBootstrap;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.radio.Configuration;
import org.graylog2.radio.bindings.PeriodicalBindings;
import org.graylog2.radio.bindings.RadioBindings;
import org.graylog2.radio.bindings.RadioInitializerBindings;
import org.graylog2.radio.cluster.Ping;
import org.graylog2.shared.bindings.ObjectMapperModule;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Command(name = "radio", description = "Start the Graylog radio")
public class Radio extends ServerBootstrap {
    private static final Logger LOG = LoggerFactory.getLogger(Radio.class);
    private static final Configuration configuration = new Configuration();

    static {
        // If this is not done, the default from BaseConfiguration will enable journaling on radio,
        // but the actual Journal implementation there is the NoopJournal. This lead to discarding all incoming messages.
        // see https://github.com/Graylog2/graylog2-server/issues/927
        configuration.setMessageJournalEnabled(false);
    }

    public Radio() {
        super("radio", configuration);
    }

    @Override
    protected List<Module> getCommandBindings() {
        return Arrays.<Module>asList(new RadioBindings(configuration, capabilities()),
                new RadioInitializerBindings(),
                new PeriodicalBindings(),
                new ObjectMapperModule());
    }

    @Override
    protected List<Object> getCommandConfigurationBeans() {
        return Arrays.<Object>asList(configuration);
    }

    @Override
    protected void startNodeRegistration(Injector injector) {
        // register node by initiating first ping. if the node isn't registered, loading persisted inputs will fail silently, for example
        Ping pinger = injector.getInstance(Ping.class);
        pinger.run();
    }

    @Override
    protected boolean validateConfiguration() {
        return true;
    }

    private static class ShutdownHook implements Runnable {
        private final ActivityWriter activityWriter;
        private final ServiceManager serviceManager;

        @Inject
        public ShutdownHook(ActivityWriter activityWriter, ServiceManager serviceManager) {
            this.activityWriter = activityWriter;
            this.serviceManager = serviceManager;
        }

        @Override
        public void run() {
            String msg = "SIGNAL received. Shutting down.";
            LOG.info(msg);
            activityWriter.write(new Activity(msg, Main.class));

            serviceManager.stopAsync().awaitStopped();
        }
    }

    @Override
    protected Class<? extends Runnable> shutdownHook() {
        return ShutdownHook.class;
    }

    @Override
    protected Set<ServerStatus.Capability> capabilities() {
        return EnumSet.of(ServerStatus.Capability.RADIO);
    }
}
