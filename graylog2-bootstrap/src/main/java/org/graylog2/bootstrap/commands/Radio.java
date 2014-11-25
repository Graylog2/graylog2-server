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

package org.graylog2.bootstrap.commands;

import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Injector;
import com.google.inject.Module;
import io.airlift.command.Command;
import io.airlift.command.Option;
import org.graylog2.bootstrap.Bootstrap;
import org.graylog2.bootstrap.Main;
import org.graylog2.radio.Configuration;
import org.graylog2.radio.bindings.PeriodicalBindings;
import org.graylog2.radio.bindings.RadioBindings;
import org.graylog2.radio.bindings.RadioInitializerBindings;
import org.graylog2.radio.cluster.Ping;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
@Command(name = "radio", description = "Start the Graylog2 radio")
public class Radio extends Bootstrap implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(Radio.class);

    private static final Configuration configuration = new Configuration();

    public Radio() {
        super("Radio", configuration);
    }

    @Override
    protected List<Module> getCommandBindings() {
        return Arrays.<Module>asList(new RadioBindings(configuration), new RadioInitializerBindings(), new PeriodicalBindings());
    }

    @Override
    protected List<Object> getCommandConfigurationBeans() {
        return Arrays.<Object>asList(configuration);
    }

    @Override
    protected void startNodeRegistration(Injector injector) {
        // register node by initiating first ping. if the node isn't registered, loading persisted inputs will fail silently, for example
        Ping.Pinger pinger = injector.getInstance(Ping.Pinger.class);
        pinger.ping();
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
}
