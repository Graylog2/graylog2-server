/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.periodical;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.RatioGauge;
import com.github.joschi.jadconfig.util.Size;
import com.google.common.eventbus.EventBus;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.GlobalMetricNames;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.ThrottleState;
import org.graylog2.plugin.periodical.Periodical;
import org.graylog2.shared.buffers.ProcessBuffer;
import org.graylog2.shared.journal.Journal;
import org.graylog2.shared.journal.LocalKafkaJournal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;

import static org.graylog2.shared.metrics.MetricUtils.safelyRegister;

/**
 * The ThrottleStateUpdater publishes the current state buffer state of the journal to other interested parties,
 * chiefly the ThrottleableTransports.
 * <p/>
 * <p>
 * It only includes the necessary information to make a decision about whether to throttle parts of the system,
 * but does not send "throttle" commands. This allows for a flexible approach in picking a throttling strategy.
 * </p>
 * <p>
 * The implementation expects to be called once per second to have a rough estimate about the events per second,
 * over the last second.
 * </p>
 */
public class ThrottleStateUpdaterThread extends Periodical {
    private static final Logger log = LoggerFactory.getLogger(ThrottleStateUpdaterThread.class);
    private final LocalKafkaJournal journal;
    private final ProcessBuffer processBuffer;
    private final EventBus eventBus;
    private final Size retentionSize;
    private final NotificationService notificationService;
    private final ServerStatus serverStatus;

    private boolean firstRun = true;
    private long logEndOffset;
    private long currentReadOffset;
    private long currentTs;
    private ThrottleState throttleState;

    @Inject
    public ThrottleStateUpdaterThread(final Journal journal,
                                      ProcessBuffer processBuffer,
                                      EventBus eventBus,
                                      NotificationService notificationService,
                                      ServerStatus serverStatus,
                                      MetricRegistry metricRegistry,
                                      @Named("message_journal_max_size") Size retentionSize) {
        this.processBuffer = processBuffer;
        this.eventBus = eventBus;
        this.retentionSize = retentionSize;
        this.notificationService = notificationService;
        this.serverStatus = serverStatus;
        // leave this.journal null, we'll say "don't start" in that case, see startOnThisNode() below.
        if (journal instanceof LocalKafkaJournal) {
            this.journal = (LocalKafkaJournal) journal;
        } else {
            this.journal = null;
        }
        throttleState = new ThrottleState();

        safelyRegister(metricRegistry,
                GlobalMetricNames.JOURNAL_APPEND_RATE,
                new Gauge<Long>() {
                    @Override
                    public Long getValue() {
                               return throttleState.appendEventsPerSec;
                           }
                       });
        safelyRegister(metricRegistry,
                       GlobalMetricNames.JOURNAL_READ_RATE,
                       new Gauge<Long>() {
                           @Override
                           public Long getValue() {
                               return throttleState.readEventsPerSec;
                           }
                       });
        safelyRegister(metricRegistry,
                       GlobalMetricNames.JOURNAL_SEGMENTS,
                       new Gauge<Integer>() {
                           @Override
                           public Integer getValue() {
                               if (ThrottleStateUpdaterThread.this.journal == null) {
                                   return 0;
                               }
                               return ThrottleStateUpdaterThread.this.journal.numberOfSegments();
                           }
                       });
        safelyRegister(metricRegistry,
                       GlobalMetricNames.JOURNAL_UNCOMMITTED_ENTRIES,
                       new Gauge<Long>() {
                           @Override
                           public Long getValue() {
                               return throttleState.uncommittedJournalEntries;
                           }
                       });
        final Gauge<Long> sizeGauge = safelyRegister(metricRegistry,
                                   GlobalMetricNames.JOURNAL_SIZE,
                                   new Gauge<Long>() {
                                       @Override
                                       public Long getValue() {
                                           return throttleState.journalSize;
                                       }
                                   });
        final Gauge<Long> sizeLimitGauge = safelyRegister(metricRegistry,
                                        GlobalMetricNames.JOURNAL_SIZE_LIMIT,
                                        new Gauge<Long>() {
                                            @Override
                                            public Long getValue() {
                                                return throttleState.journalSizeLimit;
                                            }
                                        });
        safelyRegister(metricRegistry,
                       GlobalMetricNames.JOURNAL_UTILIZATION_RATIO,
                       new RatioGauge() {
                           @Override
                           protected Ratio getRatio() {
                               return Ratio.of(sizeGauge.getValue(),
                                               sizeLimitGauge.getValue());
                           }
                       });
    }

    @Override
    public boolean runsForever() {
        return false;
    }

    @Override
    public boolean stopOnGracefulShutdown() {
        return true;
    }

    @Override
    public boolean masterOnly() {
        return false;
    }

    @Override
    public boolean startOnThisNode() {
        // don't start if we don't have the KafkaJournal
        return journal != null;
    }

    @Override
    public boolean isDaemon() {
        return true;
    }

    @Override
    public int getInitialDelaySeconds() {
        return 1;
    }

    @Override
    public int getPeriodSeconds() {
        return 1;
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

    @Override
    public void doRun() {
        throttleState = new ThrottleState(throttleState);
        final long committedOffset = journal.getCommittedOffset();

        // TODO there's a lot of duplication around this class. Probably should be refactored a bit.
        // also update metrics for each of the values, so clients can get to it cheaply

        long prevTs = currentTs;
        currentTs = System.nanoTime();

        long previousLogEndOffset = logEndOffset;
        long previousReadOffset = currentReadOffset;
        long logStartOffset = journal.getLogStartOffset();
        logEndOffset = journal.getLogEndOffset() - 1; // -1 because getLogEndOffset is the next offset that gets assigned
        currentReadOffset = journal.getNextReadOffset() - 1; // just to make it clear which field we read

        // for the first run, don't send an update, there's no previous data available to calc rates
        if (firstRun) {
            firstRun = false;
            return;
        }

        throttleState.appendEventsPerSec = (long) Math.floor((logEndOffset - previousLogEndOffset) / ((currentTs - prevTs) / 1.0E09));
        throttleState.readEventsPerSec = (long) Math.floor((currentReadOffset - previousReadOffset) / ((currentTs - prevTs) / 1.0E09));

        throttleState.journalSize = journal.size();
        throttleState.journalSizeLimit = retentionSize.toBytes();

        throttleState.processBufferCapacity = processBuffer.getRemainingCapacity();

        if (committedOffset == LocalKafkaJournal.DEFAULT_COMMITTED_OFFSET) {
            // nothing committed at all, the entire log is uncommitted, or completely empty.
            throttleState.uncommittedJournalEntries = journal.size() == 0 ? 0 : logEndOffset - logStartOffset;
        } else {
            throttleState.uncommittedJournalEntries = logEndOffset - committedOffset;
        }
        log.debug("ThrottleState: {}", throttleState);

        // the journal needs this to provide information to rest clients
        journal.setThrottleState(throttleState);

        // publish to interested parties
        eventBus.post(throttleState);

        // Abusing the current thread to send notifications from KafkaJournal in the graylog2-shared module
        final double journalUtilizationPercentage = throttleState.journalSizeLimit > 0 ? (throttleState.journalSize * 100) / throttleState.journalSizeLimit : 0.0;

        if (journalUtilizationPercentage > LocalKafkaJournal.NOTIFY_ON_UTILIZATION_PERCENTAGE) {
            Notification notification = notificationService.buildNow()
                    .addNode(serverStatus.getNodeId().toString())
                    .addType(Notification.Type.JOURNAL_UTILIZATION_TOO_HIGH)
                    .addSeverity(Notification.Severity.URGENT)
                    .addDetail("journal_utilization_percentage", journalUtilizationPercentage);
            notificationService.publishIfFirst(notification);
        }

        if (journal.getPurgedSegmentsInLastRetention() > 0) {
            Notification notification = notificationService.buildNow()
                    .addNode(serverStatus.getNodeId().toString())
                    .addType(Notification.Type.JOURNAL_UNCOMMITTED_MESSAGES_DELETED)
                    .addSeverity(Notification.Severity.URGENT);
            notificationService.publishIfFirst(notification);
        }
    }
}
