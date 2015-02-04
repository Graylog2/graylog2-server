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
package org.graylog2.bootstrap.commands.journal;

import com.google.inject.Module;
import org.graylog2.Configuration;
import org.graylog2.bootstrap.CmdLineTool;
import org.graylog2.plugin.KafkaJournalConfiguration;
import org.graylog2.shared.bindings.SchedulerBindings;
import org.graylog2.shared.journal.KafkaJournal;
import org.graylog2.shared.journal.KafkaJournalModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

public abstract class AbstractJournalCommand extends CmdLineTool {
    protected static final Logger log = LoggerFactory.getLogger(AbstractJournalCommand.class);

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
    protected boolean validateConfiguration() {
        if (kafkaJournalConfiguration.getMessageJournalDir() == null) {
            log.error("No message journal path set. Please define message_journal_dir in your graylog2.conf.");
            return false;
        }
        return true;
    }

    @Override
    protected List<Module> getCommandBindings() {
        return Arrays.<Module>asList(new SchedulerBindings(),
                                     new KafkaJournalModule());
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
