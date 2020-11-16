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
import org.apache.commons.lang3.StringUtils;
import org.graylog.autovalue.WithBeanGetter;
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
import org.graylog2.system.urlwhitelist.LiteralWhitelistEntry;
import org.graylog2.system.urlwhitelist.RegexHelper;
import org.graylog2.system.urlwhitelist.RegexWhitelistEntry;
import org.graylog2.system.urlwhitelist.UrlWhitelist;
import org.graylog2.system.urlwhitelist.UrlWhitelistService;
import org.graylog2.system.urlwhitelist.WhitelistEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Creates an initial URL whitelist. If there are services configured which use URLs that are required to be whitelisted
 * in order to be reachable, these URLs are added to the whitelist to ensure that the service will still run properly.
 * <p>If no such services are configured yet, an empty whitelist is created.</p>
 */
public class V20191129134600_CreateInitialUrlWhitelist extends Migration {
    private static final Logger log = LoggerFactory.getLogger(V20191129134600_CreateInitialUrlWhitelist.class);

    private final ClusterConfigService configService;
    private final UrlWhitelistService whitelistService;
    private final DBDataAdapterService dataAdapterService;
    private final DBNotificationService notificationService;
    private final RegexHelper regexHelper;

    @Inject
    public V20191129134600_CreateInitialUrlWhitelist(final ClusterConfigService clusterConfigService,
            UrlWhitelistService whitelistService, DBDataAdapterService dataAdapterService,
            DBNotificationService notificationService, RegexHelper regexHelper) {
        this.configService = clusterConfigService;
        this.whitelistService = whitelistService;
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

        UrlWhitelist whitelist = createWhitelist();
        whitelistService.saveWhitelist(whitelist);
        configService.write(MigrationCompleted.create(whitelist.toString()));
    }

    private UrlWhitelist createWhitelist() {
        final Set<WhitelistEntry> entries = new HashSet<>();

        dataAdapterService.findAll()
                .stream()
                .map(this::extractFromDataAdapter)
                .forEach(e -> e.ifPresent(entries::add));

        try (final Stream<NotificationDto> notificationsStream = notificationService.streamAll()) {
            notificationsStream.map(this::extractFromNotification)
                    .forEach(e -> e.ifPresent(entries::add));
        }

        log.info("Created {} whitelist entries from URLs configured in data adapters and event notifications.",
                entries.size());

        final UrlWhitelist whitelist = UrlWhitelist.createEnabled(new ArrayList<>(entries));
        log.debug("Resulting whitelist: {}.", whitelist);

        return whitelist;
    }

    private Optional<WhitelistEntry> extractFromNotification(NotificationDto notificationDto) {
        final EventNotificationConfig config = notificationDto.config();

        String url = "";
        if (config instanceof HTTPEventNotificationConfig) {
            url = ((HTTPEventNotificationConfig) config).url();
        } else if (config instanceof LegacyAlarmCallbackEventNotificationConfig) {
            url = Objects.toString(((LegacyAlarmCallbackEventNotificationConfig) config).configuration()
                    .get("url"), "");

        }

        if (StringUtils.isNotBlank(url)) {
            return defaultIfNotMatching(LiteralWhitelistEntry.create(UUID.randomUUID().toString(),
                    "\"" + notificationDto.title() + "\" alert notification", url), url);
        } else {
            return Optional.empty();
        }
    }

    private Optional<WhitelistEntry> extractFromDataAdapter(DataAdapterDto dataAdapterDto) {
        final LookupDataAdapterConfiguration config = dataAdapterDto.config();

        if (config instanceof DSVHTTPDataAdapter.Config) {
            final String url = ((DSVHTTPDataAdapter.Config) config).url();
            return defaultIfNotMatching(LiteralWhitelistEntry.create(UUID.randomUUID().toString(),
                    "\"" + dataAdapterDto.title() + "\" data adapter", url), url);
        } else if (config instanceof HTTPJSONPathDataAdapter.Config) {
            final String url = StringUtils.strip(((HTTPJSONPathDataAdapter.Config) config).url());
            // Quote all parts around the ${key} template parameter( and replace the ${key} template param with a
            // wildcard match
            String regex = regexHelper.createRegexForUrlTemplate(url, "${key}");
            return defaultIfNotMatching(RegexWhitelistEntry.create(UUID.randomUUID().toString(),
                    "\"" + dataAdapterDto.title() + "\" data adapter", regex), url);
        }

        return Optional.empty();
    }

    private Optional<WhitelistEntry> defaultIfNotMatching(WhitelistEntry entry, String url) {
        if (!entry.isWhitelisted(url)) {
            log.error("Unable to create matching URL whitelist entry for URL <{}>. Please configure your URL " +
                    "whitelist manually.", url);
            return Optional.empty();
        }
        return Optional.of(entry);
    }

    @JsonAutoDetect
    @AutoValue
    @WithBeanGetter
    public static abstract class MigrationCompleted {
        @JsonProperty("created_whitelist")
        public abstract String createdWhitelist();

        @JsonCreator
        public static MigrationCompleted create(@JsonProperty("created_whitelist") String createdWhitelist) {
            return new AutoValue_V20191129134600_CreateInitialUrlWhitelist_MigrationCompleted(createdWhitelist);
        }
    }

}
