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
package org.graylog2.periodical;

import com.google.common.annotations.VisibleForTesting;
import org.graylog2.alarmcallbacks.AlarmCallbackConfiguration;
import org.graylog2.alarmcallbacks.AlarmCallbackConfigurationService;
import org.graylog2.alarmcallbacks.EmailAlarmCallback;
import org.graylog2.alarmcallbacks.events.EmailAlarmCallbackMigrated;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.database.Persisted;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.database.users.User;
import org.graylog2.plugin.periodical.Periodical;
import org.graylog2.rest.models.alarmcallbacks.requests.CreateAlarmCallbackRequest;
import org.graylog2.shared.users.UserService;
import org.graylog2.streams.StreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class EmailAlarmCallbackMigrationPeriodical extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(EmailAlarmCallbackMigrationPeriodical.class);
    private final ClusterConfigService clusterConfigService;
    private final StreamService streamService;
    private final AlarmCallbackConfigurationService alarmCallbackService;
    private final EmailAlarmCallback emailAlarmCallback;
    private final User localAdminUser;

    @Inject
    public EmailAlarmCallbackMigrationPeriodical(ClusterConfigService clusterConfigService,
                                                 StreamService streamService,
                                                 AlarmCallbackConfigurationService alarmCallbackService,
                                                 EmailAlarmCallback emailAlarmCallback,
                                                 UserService userService) {
        this.clusterConfigService = clusterConfigService;
        this.streamService = streamService;
        this.alarmCallbackService = alarmCallbackService;
        this.emailAlarmCallback = emailAlarmCallback;
        this.localAdminUser = userService.getAdminUser();
    }

    @Override
    public boolean runsForever() {
        return true;
    }

    @Override
    public boolean stopOnGracefulShutdown() {
        return false;
    }

    @Override
    public boolean masterOnly() {
        return true;
    }

    @Override
    public boolean startOnThisNode() {
        return this.clusterConfigService.get(EmailAlarmCallbackMigrated.class) == null;
    }

    @Override
    public boolean isDaemon() {
        return false;
    }

    @Override
    public int getInitialDelaySeconds() {
        return 0;
    }

    @Override
    public int getPeriodSeconds() {
        return 0;
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Override
    public void doRun() {
        final Map<String, Optional<String>> streamMigrations = this.streamService.loadAll()
            .stream()
            .filter(stream -> !stream.getAlertReceivers().isEmpty()
                && !streamService.getAlertConditions(stream).isEmpty()
                && alarmCallbackService.getForStream(stream).isEmpty())
            .collect(Collectors.toMap(Persisted::getId, this::migrateStream));
        final boolean allSucceeded = streamMigrations.values()
            .stream()
            .allMatch(Optional::isPresent);

        final long count = streamMigrations.size();
        if (allSucceeded) {
            if (count > 0) {
                LOG.info("Successfully migrated " + count + " streams to include explicit email alarm callback.");
            } else {
                LOG.info("No streams needed to be migrated.");
            }
            this.clusterConfigService.write(EmailAlarmCallbackMigrated.create(streamMigrations));
        } else {
            final long errors = streamMigrations.values()
                .stream()
                .filter(callbackId -> !callbackId.isPresent())
                .count();
            LOG.error("Failed migrating " + errors + "/" + count + " streams to include explicit email alarm callback.");
        }
    }

    private Optional<String> migrateStream(org.graylog2.plugin.streams.Stream stream) {
        final Map<String, Object> defaultConfig = this.getDefaultEmailAlarmCallbackConfig();
        LOG.debug("Creating email alarm callback for stream <" + stream.getId() + ">");
        final AlarmCallbackConfiguration alarmCallbackConfiguration = alarmCallbackService.create(stream.getId(),
            CreateAlarmCallbackRequest.create(
                EmailAlarmCallback.class.getCanonicalName(),
                defaultConfig
            ),
            localAdminUser.getId()
        );
        try {
            final String callbackId = this.alarmCallbackService.save(alarmCallbackConfiguration);
            LOG.debug("Successfully created email alarm callback <" + callbackId + "> for stream <" + stream.getId() + ">.");
            return Optional.of(callbackId);
        } catch (ValidationException e) {
            LOG.error("Unable to create email alarm callback for stream <" + stream.getId() + ">: ", e);
        }
        return Optional.empty();
    }

    @VisibleForTesting
    Map<String, Object> getDefaultEmailAlarmCallbackConfig() {
        final ConfigurationRequest configurationRequest = this.emailAlarmCallback.getRequestedConfiguration();

        return configurationRequest.getFields().entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getDefaultValue()));
    }
}
