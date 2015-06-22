package org.graylog2.bootstrap;

import io.airlift.airline.Cli;

/**
 * This class provides the opportunity to add top level commands or command groups to the bootstrap processes.
 */
public interface CliCommandsProvider {
    void addTopLevelCommandsOrGroups(Cli.CliBuilder<CliCommand> builder);
}
