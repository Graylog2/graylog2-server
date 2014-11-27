/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.bootstrap;

import com.google.common.collect.ImmutableSet;
import io.airlift.command.Cli;
import io.airlift.command.Cli.CliBuilder;
import io.airlift.command.Help;
import org.graylog2.bootstrap.commands.Radio;
import org.graylog2.bootstrap.commands.Server;
import org.graylog2.bootstrap.commands.ShowVersion;

import java.util.Set;

public class Main {
    public static void main(String[] args) {
        final Set<Class<? extends Runnable>> commands = ImmutableSet.of(
                Server.class,
                Radio.class,
                ShowVersion.class,
                Help.class);

        final CliBuilder<Runnable> builder = Cli.<Runnable>builder("graylog2")
                .withDescription("Open source, centralized log management")
                .withDefaultCommand(Help.class)
                .withCommands(commands);

        final Cli<Runnable> cli = builder.build();
        final Runnable command = cli.parse(args);
        command.run();
    }
}
