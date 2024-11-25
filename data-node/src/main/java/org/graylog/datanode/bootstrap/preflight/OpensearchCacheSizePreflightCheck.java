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
package org.graylog.datanode.bootstrap.preflight;

import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.apache.commons.io.FileUtils;
import org.graylog.datanode.Configuration;
import org.graylog2.bootstrap.preflight.PreflightCheck;
import org.graylog2.bootstrap.preflight.PreflightCheckException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OpensearchCacheSizePreflightCheck implements PreflightCheck {

    private final String configuredNodeSearchCacheSize;
    private final Path opensearchDataLocation;
    private final Function<Path, Long> usableSpaceProvider;

    private static final Logger LOG = LoggerFactory.getLogger(OpensearchCacheSizePreflightCheck.class);

    @Inject
    public OpensearchCacheSizePreflightCheck(Configuration datanodeConfiguration) {
        this(datanodeConfiguration.getNodeSearchCacheSize(), datanodeConfiguration.getOpensearchDataLocation(), OpensearchCacheSizePreflightCheck::getUsableSpace);
    }

    public OpensearchCacheSizePreflightCheck(String cacheSize, Path opensearchDataLocation, Function<Path, Long> usableSpaceProvider) {
        this.configuredNodeSearchCacheSize = cacheSize;
        this.opensearchDataLocation = opensearchDataLocation;
        this.usableSpaceProvider = usableSpaceProvider;
    }

    @Override
    public void runCheck() throws PreflightCheckException {
        final long usableSpace = usableSpaceProvider.apply(opensearchDataLocation);
        final long cacheSize = toBytes(this.configuredNodeSearchCacheSize);
        final String usableHumanReadable = toHumanReadableSize(usableSpace);
        if (cacheSize >= usableSpace) {
            throw new PreflightCheckException("""
                    There is not enough usable space for the node search cache. Your system has only %s available.
                    Either decrease node_search_cache_size configuration or make sure that datanode has enough free disk space.
                    Current node_search_cache_size=%s"""
                    .formatted(usableHumanReadable, this.configuredNodeSearchCacheSize));
        } else if (percentageUsage(usableSpace, cacheSize) > 80.0) {
            LOG.warn("Your system is running out of disk space. Current node_search_cache_size is configured to {} " +
                    "and your disk has only {} available.", this.configuredNodeSearchCacheSize, usableHumanReadable);
        }
    }

    private double percentageUsage(long usableSpace, long cacheSize) {
        return 100.0 / usableSpace * cacheSize;
    }

    @Nonnull
    private static String toHumanReadableSize(long usableSpace) {
        return FileUtils.byteCountToDisplaySize(usableSpace).replaceFirst("\\s", "").toLowerCase(Locale.ROOT);
    }

    private static long getUsableSpace(Path opensearchDataLocation) {
        final FileStore fileStore;
        try {
            fileStore = Files.getFileStore(opensearchDataLocation);
            return fileStore.getUsableSpace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

}
