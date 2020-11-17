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
package org.graylog2.commands.journal;

import com.google.inject.Module;
import org.graylog2.Configuration;
import org.graylog2.audit.AuditBindings;
import org.graylog2.bindings.ConfigurationModule;
import org.graylog2.bootstrap.CmdLineTool;
import org.graylog2.plugin.KafkaJournalConfiguration;
import org.graylog2.plugin.Plugin;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.shared.bindings.SchedulerBindings;
import org.graylog2.shared.bindings.ServerStatusBindings;
import org.graylog2.shared.journal.KafkaJournal;
import org.graylog2.shared.journal.KafkaJournalModule;
import org.graylog2.shared.plugins.ChainingClassLoader;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public abstract class AbstractJournalCommand extends CmdLineTool {
    protected static final Configuration configuration = new Configuration();
    protected final KafkaJournalConfiguration kafkaJournalConfiguration = new KafkaJournalConfiguration();
    protected KafkaJournal journal;

    public AbstractJournalCommand() {
        this(null);
    }
    public AbstractJournalCommand(String commandName) {
        super(commandName, configuration);
    }

    @Override
    protected List<Module> getCommandBindings() {
        return Arrays.asList(new ConfigurationModule(configuration),
                             new ServerStatusBindings(capabilities()),
                             new SchedulerBindings(),
                             new KafkaJournalModule(),
                             new AuditBindings());
    }

    @Override
    protected Set<ServerStatus.Capability> capabilities() {
        return configuration.isMaster() ? Collections.singleton(ServerStatus.Capability.MASTER) : Collections.emptySet();
    }

    @Override
    protected List<Object> getCommandConfigurationBeans() {
        return Arrays.asList(configuration, kafkaJournalConfiguration);
    }

    @Override
    protected boolean onlyLogErrors() {
        // we don't want any non-error log output
        return true;
    }

    @Override
    protected Set<Plugin> loadPlugins(Path pluginPath, ChainingClassLoader chainingClassLoader) {
        // these commands do not need plugins, which could cause problems because of not loaded config beans
        return Collections.emptySet();
    }

    @Override
    protected void startCommand() {
        try {
            journal = injector.getInstance(KafkaJournal.class);
            runCommand();
        } catch (Exception e) {
            System.err.println(
                    "Unable to read the message journal. Please make sure no other Graylog process is using the journal.");
        } finally {
            if (journal != null) journal.stopAsync().awaitTerminated();
        }
    }

    protected abstract void runCommand();
}
