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
import kafka.log.LogSegment;
import org.graylog2.shared.journal.KafkaJournal;
import org.joda.time.DateTime;

@SuppressWarnings("LocalCanBeFinal")
@Command(name = "show", description = "Shows information about the persisted message journal")
public class JournalShow extends AbstractJournalCommand {

    @Option(name = {"-s", "--show-segments"}, description = "Show detail information for all segments")
    private boolean showSegmentDetails = false;

    public JournalShow() {
        super("show-journal");
    }

    @Override
    protected void runCommand() {
        long sizeInBytes = journal.size();
        int numSegments = journal.numberOfSegments();
        long committedReadOffset = journal.getCommittedReadOffset();
        final StringBuffer sb = new StringBuffer();

        final long startOffset = journal.getLogStartOffset();
        final long lastOffset = journal.getLogEndOffset() - 1;

        sb.append("Graylog message journal in directory: ").append(kafkaJournalConfiguration.getMessageJournalDir().getAbsolutePath()).append(
                "\n");
        sb.append("\t").append("Total size in bytes: ").append(sizeInBytes).append("\n");
        sb.append("\t").append("Number of segments: ").append(numSegments).append("\n");
        sb.append("\t").append("Log start offset: ").append(startOffset).append("\n");
        sb.append("\t").append("Log end offset: ").append(lastOffset).append("\n");
        sb.append("\t").append("Number of messages: ").append(lastOffset - startOffset + 1).append("\n");
        sb.append("\t").append("Committed read offset: ");
        if (committedReadOffset == Long.MIN_VALUE) {
            sb.append("nothing committed");
        } else {
            sb.append(committedReadOffset);
        }
        sb.append("\n");

        if (showSegmentDetails) {
            appendSegmentDetails(journal, sb);
        }
        sb.append("\n");

        System.out.print(sb);
        System.out.flush();
    }

    private void appendSegmentDetails(KafkaJournal journal, StringBuffer sb) {
        final Iterable<LogSegment> segments = journal.getSegments();
        int i = 1;
        for (LogSegment segment : segments) {
            sb.append("\t\t").append("Segment ").append(i++).append("\n");
            sb.append("\t\t\t").append("Base offset: ").append(segment.baseOffset()).append("\n");
            sb.append("\t\t\t").append("Size in bytes: ").append(segment.size()).append("\n");
            sb.append("\t\t\t").append("Created at: ").append(new DateTime(segment.created())).append("\n");
            sb.append("\t\t\t").append("Last modified: ").append(new DateTime(segment.lastModified())).append("\n");
        }
    }

}
