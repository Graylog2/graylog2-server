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
package org.graylog2.bootstrap.commands.journal;

import com.google.inject.Module;
import io.airlift.airline.Command;
import io.airlift.airline.Option;
import kafka.log.LogSegment;
import org.graylog2.Configuration;
import org.graylog2.bootstrap.CmdLineTool;
import org.graylog2.plugin.KafkaJournalConfiguration;
import org.graylog2.shared.bindings.SchedulerBindings;
import org.graylog2.shared.journal.KafkaJournal;
import org.graylog2.shared.journal.KafkaJournalModule;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("LocalCanBeFinal")
@Command(name = "show", description = "Shows information about the persisted message journal")
public class JournalShow extends CmdLineTool {
    private static final Logger log = LoggerFactory.getLogger(JournalShow.class);

    private static final Configuration configuration = new Configuration();
    private final KafkaJournalConfiguration kafkaJournalConfiguration = new KafkaJournalConfiguration();

    @Option(name = {"-s", "--show-segments"}, description = "Show detail information for all segments")
    private boolean showSegmentDetails = false;

    public JournalShow() {
        super(configuration);
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
        KafkaJournal journal = null;
        try {
            journal = injector.getInstance(KafkaJournal.class);

            long sizeInBytes = journal.size();
            int numSegments = journal.numberOfSegments();
            long committedReadOffset = journal.getCommittedReadOffset();
            final StringBuffer sb = new StringBuffer();

            sb.append("Graylog2 message journal in directory: ").append(new File(kafkaJournalConfiguration.getMessageJournalDir()).getAbsolutePath()).append(
                    "\n");
            sb.append("\t").append("Total size in bytes: ").append(sizeInBytes).append("\n");
            sb.append("\t").append("Number of segments: ").append(numSegments).append("\n");
            if (showSegmentDetails) {
                appendSegmentDetails(journal, sb);
            }
            sb.append("\t").append("Committed read offset: ");
            if (committedReadOffset == Long.MIN_VALUE) {
                sb.append("nothing committed");
            } else {
                sb.append(committedReadOffset);
            }
            sb.append("\n");
            sb.append("\n");

            System.out.print(sb);
            System.out.flush();
        } catch (Exception e) {
            System.err.println("Unable to read the message journal. Please make sure no other Graylog2 process is using the journal.");
        } finally {
            if (journal != null) journal.stopAsync().awaitTerminated();
        }
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
