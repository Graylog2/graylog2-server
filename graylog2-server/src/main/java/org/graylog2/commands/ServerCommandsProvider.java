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
package org.graylog2.commands;

import com.github.rvesse.airline.builder.CliBuilder;
import com.google.common.collect.ImmutableSet;
import org.graylog2.bootstrap.CliCommand;
import org.graylog2.bootstrap.CliCommandsProvider;
import org.graylog2.commands.journal.JournalDecode;
import org.graylog2.commands.journal.JournalShow;
import org.graylog2.commands.journal.JournalTruncate;

public class ServerCommandsProvider implements CliCommandsProvider {
    @Override
    public void addTopLevelCommandsOrGroups(CliBuilder<CliCommand> builder) {

        builder.withCommand(Server.class);

        builder.withGroup("journal")
                .withDescription("Manage the persisted message journal")
                .withDefaultCommand(JournalShow.class)
                .withCommands(
                        ImmutableSet.of(
                                JournalShow.class,
                                JournalTruncate.class,
                                JournalDecode.class
                        ));

    }
}
