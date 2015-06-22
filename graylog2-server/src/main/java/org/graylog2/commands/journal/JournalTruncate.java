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
package org.graylog2.commands.journal;

import io.airlift.airline.Command;
import io.airlift.airline.Option;

@Command(name = "truncate", description = "Truncates the journal to a given offset")
public class JournalTruncate extends AbstractJournalCommand {

    @Option(name = {"-o", "--offset"}, description = "Truncate journal up to this offset, no remaining offset will be larger than the given offset.", required = true)
    private long offset = Long.MIN_VALUE;

    public JournalTruncate() {
        super("truncate-journal");
    }

    @Override
    protected void runCommand() {
        try {
            final long logEndOffset = journal.getLogEndOffset();
            if (offset > logEndOffset) {
                System.err.println("Truncating journal to " + offset + " has no effect as the largest offset in the log is " + (logEndOffset - 1) + ".");
            } else {
                journal.truncateTo(offset);
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Illegal offset value " + offset);
        }
    }
}
