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
package org.graylog.datanode.opensearch.configuration.beans.impl;

import com.google.common.collect.ImmutableMap;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.apache.commons.io.FileUtils;
import org.graylog.datanode.Configuration;
import org.graylog.datanode.configuration.OpensearchConfigurationException;
import org.graylog.datanode.configuration.S3RepositoryConfiguration;
import org.graylog.datanode.opensearch.configuration.OpensearchConfigurationParams;
import org.graylog.datanode.opensearch.configuration.OpensearchUsableSpace;
import org.graylog.datanode.process.configuration.beans.DatanodeConfigurationBean;
import org.graylog.datanode.process.configuration.beans.DatanodeConfigurationPart;
import org.graylog2.bootstrap.preflight.PreflightCheckException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This opensearch configuration bean manages searchable snapshots and their S3 or local filesystem configuration.
 * It configures the search role for the node if snapshots are enabled and also validates the node search cache size.
 * If there is neither S3 nor local filesystem snapshot configuration, both search role and cache are disabled,
 * preventing unnecessary disk space consumption on the node.
 */
public class SearchableSnapshotsConfigurationBean implements DatanodeConfigurationBean<OpensearchConfigurationParams> {

    private static final Logger LOG = LoggerFactory.getLogger(SearchableSnapshotsConfigurationBean.class);

    static final String SEARCH_NODE_ROLE = "search";
    private final Configuration localConfiguration;
    private final S3RepositoryConfiguration s3RepositoryConfiguration;
    private final Provider<OpensearchUsableSpace> usableSpaceProvider;

    @Inject
    public SearchableSnapshotsConfigurationBean(Configuration localConfiguration, S3RepositoryConfiguration s3RepositoryConfiguration, Provider<OpensearchUsableSpace> usableSpaceProvider) {
        this.localConfiguration = localConfiguration;
        this.s3RepositoryConfiguration = s3RepositoryConfiguration;
        this.usableSpaceProvider = usableSpaceProvider;
    }

    @Override
    public DatanodeConfigurationPart buildConfigurationPart(OpensearchConfigurationParams trustedCertificates) {
        if (snapshotsAreEnabled()) {
            validateUsableSpace();
            return DatanodeConfigurationPart.builder()
                    .properties(properties())
                    .keystoreItems(keystoreItems())
                    .addNodeRole(SEARCH_NODE_ROLE)
                    .build();
        } else {
            LOG.info("Opensearch snapshots not configured, skipping search role and cache configuration.");
            return DatanodeConfigurationPart.builder().build();
        }
    }

    private void validateUsableSpace() throws OpensearchConfigurationException {
        final OpensearchUsableSpace usableSpace = usableSpaceProvider.get();
        final String configuredCacheSize = this.localConfiguration.getNodeSearchCacheSize();
        final long cacheSize = toBytes(configuredCacheSize);
        final String usableHumanReadable = toHumanReadableSize(usableSpace.usableSpace());
        if (cacheSize >= usableSpace.usableSpace()) {
            throw new OpensearchConfigurationException("""
                    There is not enough usable space for the node search cache. Your system has only %s available.
                    Either decrease node_search_cache_size configuration or make sure that datanode has enough free disk space.
                    Data directory: %s, current node_search_cache_size: %s"""
                    .formatted(usableHumanReadable, usableSpace.dataDir().toAbsolutePath(), configuredCacheSize));
        } else if (percentageUsage(usableSpace.usableSpace(), cacheSize) > 80.0) {
            LOG.warn("Your system is running out of disk space. Current node_search_cache_size is configured to {} " +
                    "and your disk has only {} available.", configuredCacheSize, usableHumanReadable);
        }
    }

    private double percentageUsage(long usableSpace, long cacheSize) {
        return 100.0 / usableSpace * cacheSize;
    }

    @Nonnull
    private static String toHumanReadableSize(long usableSpace) {
        return FileUtils.byteCountToDisplaySize(usableSpace).replaceFirst("\\s", "").toLowerCase(Locale.ROOT);
    }


    public static long toBytes(String cacheSize) {
        long returnValue = -1;
        Pattern patt = Pattern.compile("([\\d.]+)([GMK]B)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = patt.matcher(cacheSize);
        Map<String, Integer> powerMap = new HashMap<>();
        powerMap.put("GB", 3);
        powerMap.put("MB", 2);
        powerMap.put("KB", 1);
        if (matcher.find()) {
            String number = matcher.group(1);
            int pow = powerMap.get(matcher.group(2).toUpperCase(Locale.ROOT));
            BigDecimal bytes = new BigDecimal(number);
            bytes = bytes.multiply(BigDecimal.valueOf(1024).pow(pow));
            returnValue = bytes.longValue();
        }

        if (returnValue == -1) {
            throw new PreflightCheckException(String.format(Locale.ROOT, "Unexpected value %s of node_search_cache_size", cacheSize));
        }

        return returnValue;
    }

    private Map<String, String> properties() {
        final ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        builder.put("node.search.cache.size", localConfiguration.getNodeSearchCacheSize());

        if (isSharedFileSystemRepo()) {
            // https://opensearch.org/docs/latest/tuning-your-cluster/availability-and-recovery/snapshots/snapshot-restore/#shared-file-system
            if (localConfiguration.getPathRepo() != null && !localConfiguration.getPathRepo().isEmpty()) {
                builder.put("path.repo", String.join(",", localConfiguration.getPathRepo()));
            }
        }

        if (s3RepositoryConfiguration.isRepositoryEnabled()) {
            builder.putAll(s3RepositoryConfiguration.toOpensearchProperties());
        }
        return builder.build();
    }

    private Map<String, String> keystoreItems() {
        final ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        if (s3RepositoryConfiguration.isRepositoryEnabled()) {
            builder.put("s3.client.default.access_key", s3RepositoryConfiguration.getS3ClientDefaultAccessKey());
            builder.put("s3.client.default.secret_key", s3RepositoryConfiguration.getS3ClientDefaultSecretKey());
        }
        return builder.build();
    }

    private boolean snapshotsAreEnabled() {
        return s3RepositoryConfiguration.isRepositoryEnabled() || isSharedFileSystemRepo();
    }

    private boolean isSharedFileSystemRepo() {
        return localConfiguration.getPathRepo() != null && !localConfiguration.getPathRepo().isEmpty();
    }
}
