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
package org.graylog.datanode;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.CountingInputStream;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ArchiveDownloader {

    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    private final URL url;
    private final Path target;
    private Consumer<Float> onProgress;

    public ArchiveDownloader(URL url, Path target) {
        this.url = url;
        this.target = target;
    }

    public Path download() {
        final HttpURLConnection httpConnection = openConnection();
        long fileSize = httpConnection.getContentLength();
        CountingInputStream cis = getCountingInputStream(httpConnection);
        executor.execute(() -> {
            try {
                try (FileOutputStream fileOS = new FileOutputStream(target.toFile());) {
                    IOUtils.copyLarge(cis, fileOS);
                } finally {
                    httpConnection.disconnect();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        if (onProgress != null) {
            executor.execute(() -> {
                try {
                    while (cis.getByteCount() < fileSize) {
                        onProgress.accept((cis.getByteCount() / (float) fileSize) * 100);
                        Thread.sleep(1000);
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        try {
            executor.shutdown();
            final boolean finishedSuccessfully = executor.awaitTermination(10, TimeUnit.MINUTES);
            if (!finishedSuccessfully) {
                throw new RuntimeException("Failed to download opensearch distribution");
            }
            return target;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }


    }

    private static CountingInputStream getCountingInputStream(HttpURLConnection httpConnection) {
        try {
            return new CountingInputStream(httpConnection.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private HttpURLConnection openConnection() {
        try {
            return (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ArchiveDownloader onProgress(Consumer<Float> progressConsumer) {
        this.onProgress = progressConsumer;
        return this;
    }
}
