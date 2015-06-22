package org.graylog2.radio.commands;

import io.airlift.airline.Cli;
import org.graylog2.bootstrap.CliCommand;
import org.graylog2.bootstrap.CliCommandsProvider;

public class RadioCommandsProvider implements CliCommandsProvider {
    @Override
    public void addTopLevelCommandsOrGroups(Cli.CliBuilder<CliCommand> builder) {
        builder.withCommand(Radio.class);
    }
}
