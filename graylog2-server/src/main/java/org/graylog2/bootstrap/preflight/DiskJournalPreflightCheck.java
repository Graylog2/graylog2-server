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
package org.graylog2.bootstrap.preflight;

import com.github.joschi.jadconfig.util.Size;
import org.apache.commons.io.FileUtils;
import org.graylog.shaded.kafka09.utils.FileLock;
import org.graylog2.Configuration;
import org.graylog2.shared.messageq.MessageQueueModule;
import org.graylog2.shared.system.stats.fs.FsProbe;
import org.graylog2.shared.system.stats.fs.FsStats;
import org.graylog2.shared.utilities.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class DiskJournalPreflightCheck implements PreflightCheck {
    private static final Logger LOG = LoggerFactory.getLogger(DiskJournalPreflightCheck.class);

    private final Configuration configuration;
    private final FsProbe fsProbe;
    private final Path journalDirectory;
    private final Size journalMaxSize;

    @Inject
    public DiskJournalPreflightCheck(Configuration configuration,
                                     FsProbe fsProbe,
                                     @Named("message_journal_dir") Path journalDirectory,
                                     @Named("message_journal_max_size") Size journalMaxSize) {
        this.configuration = configuration;
        this.fsProbe = fsProbe;
        this.journalDirectory = journalDirectory;
        this.journalMaxSize = journalMaxSize;
    }

    @Override
    public void runCheck() throws PreflightCheckException {
        if (!configuration.isMessageJournalEnabled() || (!configuration.getMessageJournalMode().equals(MessageQueueModule.DISK_JOURNAL_MODE))) {
            return;
        }
        checkWritableJournalDir();
        checkJournalDirSizeAndType();
        checkJournalUnlocked();
    }

    private void checkJournalDirSizeAndType() {
        final Map<String, FsStats.Filesystem> filesystems = fsProbe.fsStats().filesystems();
        final FsStats.Filesystem journalFs = filesystems.get(journalDirectory.toAbsolutePath().toString());
        if (journalFs != null) {
            final long availableOnFS = journalFs.available();
            if (availableOnFS > 0) {
                final long usedByJournal = FileUtils.sizeOfDirectory(journalDirectory.toFile());
                if (availableOnFS + usedByJournal < journalMaxSize.toBytes()) {
                       throw new PreflightCheckException(StringUtils.f(
                            "Journal directory <%s> has not enough free space (%d MB) available. You need to provide additional %d MB to contain 'message_journal_max_size = %d MB' ",
                            journalDirectory.toAbsolutePath(),
                            Size.bytes(availableOnFS).toMegabytes(),
                            Size.bytes(journalMaxSize.toBytes() - usedByJournal - availableOnFS).toMegabytes(),
                            journalMaxSize.toMegabytes()
                    ));
                }
            }
            if (journalFs.typeName() != null && journalFs.typeName().equals("Network Disk")) {
                final String message = StringUtils.f(
                        "Journal directory <%s> should not be on a network file system (%s)!",
                        journalDirectory.toAbsolutePath(),
                        journalFs.sysTypeName()
                );
                LOG.warn(message);
            }
        } else {
            LOG.warn("Could not perform size check on journal directory <{}>", journalDirectory.toAbsolutePath());
        }
    }

    private void checkWritableJournalDir() {
        if (!Files.exists(journalDirectory)) {
            try {
                Files.createDirectories(journalDirectory);
            } catch (IOException e) {
                throw new PreflightCheckException(StringUtils.f("Cannot create journal directory at <%s>", journalDirectory.toAbsolutePath()), e);
            }
        }
        if (!Files.isWritable(journalDirectory)) {
            throw new PreflightCheckException(StringUtils.f("Journal directory <%s> is not writable!", journalDirectory.toAbsolutePath()));
        }
    }

    private void checkJournalUnlocked() {
        final File file = new File(journalDirectory.toFile(), ".lock");
        FileLock fileLock = null;
        try {
            fileLock = new FileLock(file);
            if (!fileLock.tryLock()) {
                throw new PreflightCheckException(
                        StringUtils.f("The journal is already locked by another process. Try running fuser \"%s\" to find the PID.",
                        file.getAbsolutePath()));
            }
        } finally {
            if (fileLock != null) {
                fileLock.unlock();
            }
        }
    }
}
