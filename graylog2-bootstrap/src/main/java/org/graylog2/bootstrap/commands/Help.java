package org.graylog2.bootstrap.commands;

import org.graylog2.bootstrap.CliCommand;

/* shallow subclass to make it implement CliCommand */
public class Help extends io.airlift.airline.Help implements CliCommand {
}
