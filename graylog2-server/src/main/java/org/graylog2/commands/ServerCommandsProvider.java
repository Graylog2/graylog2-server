package org.graylog2.commands;

import com.google.common.collect.ImmutableSet;
import io.airlift.airline.Cli;
import org.graylog2.bootstrap.CliCommand;
import org.graylog2.bootstrap.CliCommandsProvider;
import org.graylog2.commands.journal.JournalDecode;
import org.graylog2.commands.journal.JournalShow;
import org.graylog2.commands.journal.JournalTruncate;

public class ServerCommandsProvider implements CliCommandsProvider {
    @Override
    public void addTopLevelCommandsOrGroups(Cli.CliBuilder<CliCommand> builder) {

        builder.withCommand(Server.class);

        builder.withGroup("journal")
                .withDescription("Manage the persisted message journal")
                .withDefaultCommand(JournalShow.class)
                .withCommands(
                        ImmutableSet.<Class<? extends CliCommand>>of(
                                JournalShow.class,
                                JournalTruncate.class,
                                JournalDecode.class
                        ));

    }
}
