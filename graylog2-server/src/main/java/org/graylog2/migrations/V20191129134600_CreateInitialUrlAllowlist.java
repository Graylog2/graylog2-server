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
package org.graylog2.migrations;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import jakarta.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.graylog.events.legacy.LegacyAlarmCallbackEventNotificationConfig;
import org.graylog.events.notifications.DBNotificationService;
import org.graylog.events.notifications.EventNotificationConfig;
import org.graylog.events.notifications.NotificationDto;
import org.graylog.events.notifications.types.HTTPEventNotificationConfig;
import org.graylog2.lookup.adapters.DSVHTTPDataAdapter;
import org.graylog2.lookup.adapters.HTTPJSONPathDataAdapter;
import org.graylog2.lookup.db.DBDataAdapterService;
import org.graylog2.lookup.dto.DataAdapterDto;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.lookup.LookupDataAdapterConfiguration;
import org.graylog2.system.urlallowlist.AllowlistEntry;
import org.graylog2.system.urlallowlist.LiteralAllowlistEntry;
import org.graylog2.system.urlallowlist.RegexAllowlistEntry;
import org.graylog2.system.urlallowlist.RegexHelper;
import org.graylog2.system.urlallowlist.UrlAllowlist;
import org.graylog2.system.urlallowlist.UrlAllowlistService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Creates an initial URL allowlist. If a legacy whitelist entry exists, copy the entries from that.
 * If there are services configured which use URLs that are required to be allowlisted
 * in order to be reachable, these URLs are added to the allowlist to ensure that the service will still run properly.
 * <p>If no such services are configured yet, an empty allowlist is created.</p>
 *
 */
public class V20191129134600_CreateInitialUrlAllowlist extends Migration {
    private static final Logger log = LoggerFactory.getLogger(V20191129134600_CreateInitialUrlAllowlist.class);

    private static final String LEGACY_ALLOWLIST = "org.graylog2.system.urlwhitelist.UrlWhitelist";

    private final ClusterConfigService configService;
    private final UrlAllowlistService allowlistService;
    private final DBDataAdapterService dataAdapterService;
    private final DBNotificationService notificationService;
    private final RegexHelper regexHelper;

    @Inject
    public V20191129134600_CreateInitialUrlAllowlist(final ClusterConfigService clusterConfigService,
                                                     UrlAllowlistService allowlistService, DBDataAdapterService dataAdapterService,
                                                     DBNotificationService notificationService, RegexHelper regexHelper) {
        this.configService = clusterConfigService;
        this.allowlistService = allowlistService;
        this.dataAdapterService = dataAdapterService;
        this.notificationService = notificationService;
        this.regexHelper = regexHelper;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2019-11-29T13:46:00Z");
    }

    @Override
    public void upgrade() {
        final MigrationCompleted migrationCompleted = configService.get(MigrationCompleted.class);

        if (migrationCompleted != null) {
            log.debug("Migration already completed.");
            return;
        }

        UrlAllowlist allowlist = createAllowlist();
        UrlAllowlist legacyAllowList = configService.get(LEGACY_ALLOWLIST, UrlAllowlist.class);
        if (legacyAllowList != null) {
            allowlist.entries().addAll(legacyAllowList.entries());
            log.debug("Added legacy allowlist: {}", legacyAllowList);
        }

        allowlistService.saveAllowlist(allowlist);
        configService.write(MigrationCompleted.create(allowlist.toString()));
    }

    private UrlAllowlist createAllowlist() {
        final Set<AllowlistEntry> entries = new HashSet<>();

        try (Stream<DataAdapterDto> dataAdapterStream = dataAdapterService.streamAll()) {
            dataAdapterStream
                    .map(this::extractFromDataAdapter)
                    .forEach(e -> e.ifPresent(entries::add));
        }

        try (final Stream<NotificationDto> notificationsStream = notificationService.streamAll()) {
            notificationsStream.map(this::extractFromNotification)
                    .forEach(e -> e.ifPresent(entries::add));
        }

        log.info("Created {} allowlist entries from URLs configured in data adapters and event notifications.",
                entries.size());

        final UrlAllowlist allowlist = UrlAllowlist.createEnabled(new ArrayList<>(entries));
        log.debug("Resulting allowlist: {}.", allowlist);

        return allowlist;
    }

    private Optional<AllowlistEntry> extractFromNotification(NotificationDto notificationDto) {
        final EventNotificationConfig config = notificationDto.config();

        String url = "";
        if (config instanceof HTTPEventNotificationConfig) {
            url = ((HTTPEventNotificationConfig) config).url();
        } else if (config instanceof LegacyAlarmCallbackEventNotificationConfig) {
            url = Objects.toString(((LegacyAlarmCallbackEventNotificationConfig) config).configuration()
                    .get("url"), "");

        }

        if (StringUtils.isNotBlank(url)) {
            return defaultIfNotMatching(LiteralAllowlistEntry.create(UUID.randomUUID().toString(),
                    "\"" + notificationDto.title() + "\" alert notification", url), url);
        } else {
            return Optional.empty();
        }
    }

    private Optional<AllowlistEntry> extractFromDataAdapter(DataAdapterDto dataAdapterDto) {
        final LookupDataAdapterConfiguration config = dataAdapterDto.config();

        if (config instanceof DSVHTTPDataAdapter.Config) {
            final String url = ((DSVHTTPDataAdapter.Config) config).url();
            return defaultIfNotMatching(LiteralAllowlistEntry.create(UUID.randomUUID().toString(),
                    "\"" + dataAdapterDto.title() + "\" data adapter", url), url);
        } else if (config instanceof HTTPJSONPathDataAdapter.Config) {
            final String url = StringUtils.strip(((HTTPJSONPathDataAdapter.Config) config).url());
            // Quote all parts around the ${key} template parameter( and replace the ${key} template param with a
            // wildcard match
            String regex = regexHelper.createRegexForUrlTemplate(url, "${key}");
            return defaultIfNotMatching(RegexAllowlistEntry.create(UUID.randomUUID().toString(),
                    "\"" + dataAdapterDto.title() + "\" data adapter", regex), url);
        }

        return Optional.empty();
    }

    private Optional<AllowlistEntry> defaultIfNotMatching(AllowlistEntry entry, String url) {
        if (!entry.isAllowlisted(url)) {
            log.error("Unable to create matching URL allowlist entry for URL <{}>. Please configure your URL " +
                    "allowlist manually.", url);
            return Optional.empty();
        }
        return Optional.of(entry);
    }

    @JsonAutoDetect
    @AutoValue
    public static abstract class MigrationCompleted {
        @JsonProperty("created_allowlist")
        public abstract String createdAllowlist();

        @JsonCreator
        public static MigrationCompleted create(@JsonProperty("created_allowlist") String createdAllowlist) {
            return new AutoValue_V20191129134600_CreateInitialUrlAllowlist_MigrationCompleted(createdAllowlist);
        }
    }

}
