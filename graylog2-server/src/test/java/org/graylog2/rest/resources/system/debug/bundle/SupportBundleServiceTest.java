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
package org.graylog2.rest.resources.system.debug.bundle;

import org.graylog2.cluster.NodeService;
import org.graylog2.rest.RemoteInterfaceProvider;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class SupportBundleServiceTest {

    @Mock
    private ExecutorService executorService;

    @Mock
    private NodeService nodeService;

    @Mock
    private RemoteInterfaceProvider remoteInterfaceProvider;

    @Mock
    private Path dataDir;

    @Mock
    private ObjectMapperProvider objectMapperProvider;

    @InjectMocks
    private SupportBundleService supportBundleService;


    @Test
    public void testLogSizeLimiterWithEnoughSpaceLeft() {
        final List<LogFile> fullLoglist = List.of(
                new LogFile("memory", "server.mem.log", 500, Instant.now()),
                new LogFile("0", "server.log", 500, Instant.now()),
                new LogFile("1", "server.log.1.gz", 500, Instant.now()),
                new LogFile("2", "server.log.2.gz", 500, Instant.now())
        );
        final List<LogFile> shrinkedList = supportBundleService.applyBundleSizeLogFileLimit(fullLoglist);

        assertThat(shrinkedList).containsExactlyInAnyOrderElementsOf(fullLoglist);
    }

    @Test
    public void testLogSizeLimiterWithLimitedSpace() {
        final List<LogFile> fullLoglist = List.of(
                new LogFile("memory", "server.mem.log", 500, Instant.now()),
                new LogFile("0", "server.log", 65 * 1024 * 1024, Instant.now()),
                new LogFile("1", "server.log.1.gz", 500, Instant.now().minus(1, ChronoUnit.DAYS)),
                new LogFile("2", "server.log.2.gz", 500, Instant.now().minus(2, ChronoUnit.DAYS))
        );
        final List<LogFile> shrinkedList = supportBundleService.applyBundleSizeLogFileLimit(fullLoglist);

        assertThat(shrinkedList).hasSize(2);
        assertThat(shrinkedList).extracting(LogFile::id).contains("memory", "0");
    }

    @Test
    public void testLogSizeLimiterWithSpaceForOneZippedFile() {
        final List<LogFile> fullLoglist = List.of(
                new LogFile("memory", "server.mem.log", 500, Instant.now()),
                new LogFile("0", "server.log", 50 * 1024 * 1024, Instant.now()),
                new LogFile("1", "server.log.1.gz", 20 * 1024 * 1024, Instant.now().minus(1, ChronoUnit.DAYS)),
                new LogFile("2", "server.log.2.gz", 500, Instant.now().minus(2, ChronoUnit.DAYS))
        );
        final List<LogFile> shrinkedList = supportBundleService.applyBundleSizeLogFileLimit(fullLoglist);

        assertThat(shrinkedList).hasSize(3);
        assertThat(shrinkedList).extracting(LogFile::id).contains("memory", "0", "1");
    }
}
