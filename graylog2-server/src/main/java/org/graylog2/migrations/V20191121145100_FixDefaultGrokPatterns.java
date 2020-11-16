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
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.grok.GrokPattern;
import org.graylog2.grok.GrokPatternService;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.database.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Fixes errors in the default grok patterns that have been installed by the
 * {@link V20180924111644_AddDefaultGrokPatterns} migration.
 * <p>
 * At the moment this is only the COMMONAPACHELOG pattern, but if we find more issues with the default patterns we
 * <em>should</em> be able to adjust this migration by adding more patterns instead of having to write additional
 * migrations.
 *
 */
public class V20191121145100_FixDefaultGrokPatterns extends Migration {
    private static final Logger log = LoggerFactory.getLogger(V20191121145100_FixDefaultGrokPatterns.class);

    private final ClusterConfigService configService;
    private final GrokPatternService grokPatternService;

    @VisibleForTesting
    static final List<PatternToMigrate> patternsToMigrate = ImmutableList.of(
        PatternToMigrate.builder()
                      .name("COMMONAPACHELOG")
                      .migrateFrom("%{IPORHOST:clientip} %{HTTPDUSER:ident} %{USER:auth} \\[%{HTTPDATE:timestamp}\\] \"(?:%{WORD:verb} %{NOTSPACE:request}(?: HTTP/%{NUMBER:httpversion})?|%{DATA:rawrequest})\" %{NUMBER:response} (?:%{NUMBER:bytes}|-)")
                      .migrateTo("%{IPORHOST:clientip} %{HTTPDUSER:ident} %{USER:auth} \\[%{HTTPDATE:timestamp;date;dd/MMM/yyyy:HH:mm:ss Z}\\] \"(?:%{WORD:verb} %{NOTSPACE:request}(?: HTTP/%{NUMBER:httpversion})?|%{DATA:rawrequest})\" %{NUMBER:response} (?:%{NUMBER:bytes}|-)")
                      .build()
    );

    @Inject
    public V20191121145100_FixDefaultGrokPatterns(final ClusterConfigService clusterConfigService,
                                                  GrokPatternService grokPatternService) {
        this.grokPatternService = grokPatternService;
        this.configService = clusterConfigService;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2019-11-21T14:51:00Z");
    }

    @Override
    public void upgrade() {
        final MigrationCompleted migrationCompleted = configService.get(MigrationCompleted.class);

        final Set<String> patternNames = patternsToMigrate.stream()
                .map(PatternToMigrate::name)
                .collect(Collectors.toSet());

        if (migrationCompleted != null && migrationCompleted.patterns().containsAll(patternNames)) {
            log.debug("Migration already completed.");
            return;
        }

        try {
            for (PatternToMigrate patternToMigrate : patternsToMigrate) {
                migratePattern(patternToMigrate);
            }
            configService.write(MigrationCompleted.create(patternNames));
        } catch (ValidationException e) {
            log.error("Unable to migrate Grok Pattern.", e);
        }
    }

    private void migratePattern(PatternToMigrate patternToMigrate) throws ValidationException {
        final Optional<GrokPattern> currentPattern = grokPatternService.loadByName(patternToMigrate.name());
        if (!currentPattern.isPresent()) {
            log.debug("Couldn't find default pattern '{}'. Skipping migration.", patternToMigrate.name());
            return;
        }

        final GrokPattern pattern = currentPattern.get();

        if (!patternToMigrate.migrateFrom().equals(pattern.pattern())) {
            log.info("Skipping migration of modified default Grok Pattern '{}'.", pattern.name());
        } else {
            log.info("Migrating default Grok Pattern '{}'.", pattern.name());
            final GrokPattern migratedPattern = pattern.toBuilder()
                    .pattern(patternToMigrate.migrateTo())
                    .build();
            grokPatternService.update(migratedPattern);
        }
    }

    @JsonAutoDetect
    @AutoValue
    @WithBeanGetter
    public static abstract class MigrationCompleted {
        @JsonProperty("patterns")
        public abstract Set<String> patterns();

        @JsonCreator
        public static MigrationCompleted create(@JsonProperty("patterns") Set<String> patterns) {
            return new AutoValue_V20191121145100_FixDefaultGrokPatterns_MigrationCompleted(patterns);
        }
    }

    @AutoValue
    public static abstract class PatternToMigrate {
        public abstract String name();
        public abstract String migrateFrom();
        public abstract String migrateTo();

        public static Builder builder() {
            return new AutoValue_V20191121145100_FixDefaultGrokPatterns_PatternToMigrate.Builder();
        }

        @AutoValue.Builder
        public abstract static class Builder {
            public abstract Builder name(String name);
            public abstract Builder migrateFrom(String migrateFrom);
            public abstract Builder migrateTo(String migrateTo);
            public abstract PatternToMigrate build();
        }
    }
}
