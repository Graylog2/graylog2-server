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
package org.graylog2.rest.resources.system;

import com.codahale.metrics.annotation.Timed;
import com.github.joschi.jadconfig.util.Size;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import kafka.log.LogSegment;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.Configuration;
import org.graylog2.plugin.KafkaJournalConfiguration;
import org.graylog2.plugin.ThrottleState;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.rest.resources.system.responses.JournalSummaryResponse;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.shared.journal.Journal;
import org.graylog2.shared.journal.KafkaJournal;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@RequiresAuthentication
@Api(value = "System/Journal", description = "Message journal information of this node.")
@Produces(MediaType.APPLICATION_JSON)
@Path("/system/journal")
public class JournalResource extends RestResource {
    private static final Logger log = LoggerFactory.getLogger(JournalResource.class);
    private final boolean journalEnabled;
    private final Journal journal;
    private final KafkaJournalConfiguration kafkaJournalConfiguration;

    @Inject
    public JournalResource(Configuration configuration, KafkaJournalConfiguration kafkaJournalConfiguration, Journal journal) {
        this.kafkaJournalConfiguration = kafkaJournalConfiguration;
        this.journalEnabled = configuration.isMessageJournalEnabled();
        this.journal = journal;
    }

    @GET
    @Timed
    @ApiOperation(value = "Get current state of the journal on this node.")
    @RequiresPermissions(RestPermissions.JOURNAL_READ)
    public JournalSummaryResponse show() {
        if (!journalEnabled) {
            return JournalSummaryResponse.createDisabled();
        }

        if (journal instanceof KafkaJournal) {
            final KafkaJournal kafkaJournal = (KafkaJournal) journal;
            final ThrottleState throttleState = kafkaJournal.getThrottleState();
            kafkaJournal.numberOfSegments();

            long oldestSegment = Long.MAX_VALUE;
            for (final LogSegment segment : kafkaJournal.getSegments()) {
                oldestSegment = Math.min(oldestSegment, segment.created());
            }

            return JournalSummaryResponse.createEnabled(throttleState.appendEventsPerSec,
                                                        throttleState.readEventsPerSec,
                                                        throttleState.uncommittedJournalEntries,
                                                        Size.bytes(throttleState.journalSize),
                                                        Size.bytes(throttleState.journalSizeLimit),
                                                        kafkaJournal.numberOfSegments(),
                                                        new DateTime(oldestSegment, DateTimeZone.UTC),
                                                        kafkaJournalConfiguration
            );

        }

        log.warn("Unknown Journal implementation {} in use, cannot get information about it. Pretending journal is disabled.",
                journal.getClass());
        return JournalSummaryResponse.createDisabled();

    }

}
