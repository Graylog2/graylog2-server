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
package org.graylog2.radio.bindings;

import com.google.inject.name.Names;
import com.google.inject.util.Providers;
import org.graylog2.jersey.container.netty.SecurityContextFactory;
import org.graylog2.plugin.BaseConfiguration;
import org.graylog2.plugin.inject.Graylog2Module;
import org.graylog2.radio.Configuration;
import org.graylog2.radio.bindings.providers.RadioTransportProvider;
import org.graylog2.radio.buffers.processors.RadioProcessBufferProcessor;
import org.graylog2.radio.inputs.PersistedInputsImpl;
import org.graylog2.radio.security.RadioSecurityContextFactory;
import org.graylog2.radio.system.activities.NullActivityWriter;
import org.graylog2.radio.transports.RadioTransport;
import org.graylog2.radio.users.NullUserServiceImpl;
import org.graylog2.shared.buffers.processors.ProcessBufferProcessor;
import org.graylog2.shared.inputs.PersistedInputs;
import org.graylog2.shared.journal.NoopJournalModule;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.graylog2.shared.users.UserService;

import java.io.File;

public class RadioBindings extends Graylog2Module {
    private final Configuration configuration;

    public RadioBindings(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    protected void configure() {
        bindSingletons();
        bindTransport();
        bind(ProcessBufferProcessor.class).to(RadioProcessBufferProcessor.class);
        bindInterfaces();
    }

    private void bindInterfaces() {
        bind(ActivityWriter.class).to(NullActivityWriter.class);
        bind(PersistedInputs.class).to(PersistedInputsImpl.class);
        bind(UserService.class).to(NullUserServiceImpl.class);
        bind(SecurityContextFactory.class).to(RadioSecurityContextFactory.class);
    }

    private void bindSingletons() {
        bind(Configuration.class).toInstance(configuration);
        bind(BaseConfiguration.class).toInstance(configuration);

        install(new NoopJournalModule());
        // make this null for radio for now, because we don't support journalling here yet.
        bind(File.class).annotatedWith(Names.named("message_journal_dir")).toProvider(Providers.<File>of(null));
        
        bind(String[].class).annotatedWith(Names.named("RestControllerPackages")).toInstance(new String[]{
                "org.graylog2.radio.rest.resources",
                "org.graylog2.shared.rest.resources"
        });
    }

    private void bindTransport() {
        bind(RadioTransport.class).toProvider(RadioTransportProvider.class);
    }

}
