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
package org.graylog2.migrations;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.apache.commons.lang3.StringUtils;
import org.graylog.autovalue.WithBeanGetter;
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
import org.graylog2.system.urlwhitelist.RegexWhitelistEntry;
import org.graylog2.system.urlwhitelist.UrlWhitelist;
import org.graylog2.system.urlwhitelist.UrlWhitelistService;
import org.graylog2.system.urlwhitelist.WhitelistEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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

    @Inject
    public V20191129134600_CreateInitialUrlWhitelist(final ClusterConfigService clusterConfigService,
            UrlWhitelistService whitelistService, DBDataAdapterService dataAdapterService,
            DBNotificationService notificationService) {
        this.configService = clusterConfigService;
        this.whitelistService = whitelistService;
        this.dataAdapterService = dataAdapterService;
        this.notificationService = notificationService;
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
        whitelistService.save(whitelist);
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

        final UrlWhitelist whitelist = UrlWhitelist.create(new ArrayList<>(entries));
        log.debug("Resulting whitelist: {}.", whitelist);

        return whitelist;
    }

    private Optional<WhitelistEntry> extractFromNotification(NotificationDto notificationDto) {
        final EventNotificationConfig config = notificationDto.config();

        if (!(config instanceof HTTPEventNotificationConfig)) {
            return Optional.empty();
        }
        final String url = ((HTTPEventNotificationConfig) config).url();
        return defaultIfNotMatching(new LiteralWhitelistEntry(url), url);
    }

    private Optional<WhitelistEntry> extractFromDataAdapter(DataAdapterDto dataAdapterDto) {
        final LookupDataAdapterConfiguration config = dataAdapterDto.config();

        if (config instanceof DSVHTTPDataAdapter.Config) {
            final String url = ((DSVHTTPDataAdapter.Config) config).url();
            return defaultIfNotMatching(new LiteralWhitelistEntry(url), url);
        } else if (config instanceof HTTPJSONPathDataAdapter.Config) {
            final String url = StringUtils.strip(((HTTPJSONPathDataAdapter.Config) config).url());
            // Quote all parts around the ${key} template parameter( and replace the ${key} template param with a
            // wildcard match
            String transformedUrl = Arrays.stream(StringUtils.splitByWholeSeparator(url, "${key}"))
                    .map(part -> StringUtils.isBlank(part) ? part : Pattern.quote(part))
                    .collect(Collectors.joining(".*?"));
            return defaultIfNotMatching(new RegexWhitelistEntry("^" + transformedUrl + "$"), url);
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
