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
import jakarta.annotation.Nonnull;
import org.graylog2.Configuration;
import org.graylog2.commands.AbstractNodeCommand;
import org.graylog2.featureflag.FeatureFlags;
import org.graylog2.plugin.KafkaJournalConfiguration;
import org.graylog2.plugin.Plugin;
import org.graylog2.shared.journal.LocalKafkaJournal;
import org.graylog2.shared.journal.LocalKafkaJournalModule;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public abstract class AbstractJournalCommand extends AbstractNodeCommand {
    protected final KafkaJournalConfiguration kafkaJournalConfiguration = new KafkaJournalConfiguration();
    protected LocalKafkaJournal journal;

    public AbstractJournalCommand(String commandName) {
        super(commandName, new JournalCommandConfiguration());
    }

    @Override
    protected @Nonnull List<Module> getNodeCommandBindings(FeatureFlags featureFlags) {
        return List.of(
                new LocalKafkaJournalModule()
        );
    }

    @Override
    protected @Nonnull List<Object> getNodeCommandConfigurationBeans() {
        return List.of(kafkaJournalConfiguration);
    }

    @Override
    protected boolean onlyLogErrors() {
        // we don't want any non-error log output
        return true;
    }

    @Override
    protected Set<Plugin> loadPlugins() {
        // these commands do not need plugins, which could cause problems because of not loaded config beans
        return Collections.emptySet();
    }

    @Override
    protected void startCommand() {
        try {
            journal = injector.getInstance(LocalKafkaJournal.class);
            runCommand();
        } catch (Exception e) {
            System.err.println(
                    "Unable to read the message journal. Please make sure no other Graylog process is using the journal.");
        } finally {
            if (journal != null) {
                journal.stopAsync().awaitTerminated();
            }
        }
    }

    protected abstract void runCommand();

    static class JournalCommandConfiguration extends Configuration {
        @Override
        public boolean withNodeIdFile() {
            return false;
        }

        @Override
        public boolean withScheduler() {
            return true;
        }

        @Override
        public boolean withEventBus() {
            return false;
        }

        @Override
        public boolean withPlugins() {
            return false;
        }

        @Override
        public boolean withMongoDb() {
            return false;
        }
    }
}
