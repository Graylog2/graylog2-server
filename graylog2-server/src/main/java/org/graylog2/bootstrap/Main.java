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
package org.graylog2.bootstrap;

import com.google.common.collect.ImmutableSet;
import io.airlift.airline.Cli;
import io.airlift.airline.Cli.CliBuilder;
import org.graylog2.bootstrap.commands.Help;
import org.graylog2.bootstrap.commands.ShowVersion;

import java.util.ServiceLoader;

public class Main {
    public static void main(String[] args) {
        final CliBuilder<CliCommand> builder = Cli.<CliCommand>builder("graylog")
                .withDescription("Open source, centralized log management")
                .withDefaultCommand(Help.class)
                .withCommands(ImmutableSet.of(
                        ShowVersion.class,
                        Help.class));

        // add rest from classpath
        final ServiceLoader<CliCommandsProvider> commandsProviders = ServiceLoader.load(CliCommandsProvider.class);
        for (CliCommandsProvider provider : commandsProviders) {
            provider.addTopLevelCommandsOrGroups(builder);
        }

        final Cli<CliCommand> cli = builder.build();
        final Runnable command = cli.parse(args);
        command.run();
    }


}
