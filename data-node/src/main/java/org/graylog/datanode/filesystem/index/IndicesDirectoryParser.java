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
package org.graylog.datanode.filesystem.index;

import jakarta.annotation.Nullable;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog.datanode.filesystem.index.dto.IndexInformation;
import org.graylog.datanode.filesystem.index.dto.IndexerDirectoryInformation;
import org.graylog.datanode.filesystem.index.dto.NodeInformation;
import org.graylog.datanode.filesystem.index.dto.ShardInformation;
import org.graylog.datanode.filesystem.index.indexreader.ShardStats;
import org.graylog.datanode.filesystem.index.indexreader.ShardStatsParser;
import org.graylog.datanode.filesystem.index.statefile.StateFile;
import org.graylog.datanode.filesystem.index.statefile.StateFileParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
public class IndicesDirectoryParser {

    private static final Logger LOG = LoggerFactory.getLogger(IndicesDirectoryParser.class);
    private static final Pattern NUMERIC_DIR = Pattern.compile("\\d+");

    public static final String STATE_DIR_NAME = "_state";
    public static final String STATE_FILE_EXTENSION = ".st";

    private final StateFileParser stateFileParser;
    private final ShardStatsParser shardReader;
    private final ExecutorService executor;

    @Inject
    public IndicesDirectoryParser(StateFileParser stateFileParser, ShardStatsParser shardReader) {
        this.stateFileParser = stateFileParser;
        this.shardReader = shardReader;
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    @PreDestroy
    public void close() {
        executor.shutdown();
    }

    public IndexerDirectoryInformation parse(Path path) {
        if (!Files.exists(path)) {
            throw new IndexerInformationParserException("Path " + path + " does not exist.");
        }

        if (!Files.isDirectory(path)) {
            throw new IndexerInformationParserException("Path " + path + " is not a directory");
        }

        if (!Files.isReadable(path)) {
            throw new IndexerInformationParserException("Path " + path + " is not readable");
        }

        final Path nodesPath = path.resolve("nodes");

        if (!Files.exists(nodesPath)) {
            return IndexerDirectoryInformation.empty(path);
        }

        try (final Stream<Path> nodes = Files.list(nodesPath)) {
            final List<NodeInformation> nodeInformation = nodes.filter(Files::isDirectory)
                    .filter(p -> NUMERIC_DIR.matcher(p.getFileName().toString()).matches())
                    .map(this::parseNode)
                    .filter(node -> !node.isEmpty())
                    .toList();
            return new IndexerDirectoryInformation(path, nodeInformation);
        } catch (IOException e) {
            throw new IndexerInformationParserException("Failed to list nodes", e);
        }
    }

    private NodeInformation parseNode(Path nodePath) {
        final Path indicesDir = nodePath.resolve("indices");
        if (!Files.exists(indicesDir)) {
            return NodeInformation.empty(nodePath);
        }
        try (Stream<Path> indicesDirs = Files.list(indicesDir)) {
            final StateFile state = getState(nodePath, "node");
            // Collect paths eagerly: the underlying DirectoryStream is not thread-safe
            final List<CompletableFuture<IndexInformation>> futures = indicesDirs.toList().stream()
                    .map(p -> CompletableFuture.supplyAsync(() -> parseIndex(p), executor))
                    .toList();
            final List<IndexInformation> indices = awaitAll(futures).stream()
                    .sorted(Comparator.comparing(IndexInformation::indexName))
                    .collect(Collectors.toList());
            return new NodeInformation(nodePath, indices, state);
        } catch (IOException e) {
            throw new IndexerInformationParserException("Failed to list indices directories", e);
        }
    }

    @Nullable
    private StateFile getState(Path path, String stateFilePrefix) {
        final Optional<StateFile> stateFile = findStateFile(path, stateFilePrefix)
                .map(stateFileParser::parse);
        if (stateFile.isPresent()) {
            return stateFile.get();
        } else {
            LOG.warn("Couldn't find state file in directory " + path + ". This is unexpected but indexers can usually recover from this.");
            return null;
        }

    }

    private IndexInformation parseIndex(Path path) {
        final String indexID = path.getFileName().toString();
        final StateFile state = getState(path, "state");
        try (Stream<Path> shardDirs = Files.list(path)) {
            // Collect paths eagerly: the underlying DirectoryStream is not thread-safe
            final List<CompletableFuture<ShardInformation>> futures = shardDirs
                    .filter(Files::isDirectory)
                    .filter(p -> NUMERIC_DIR.matcher(p.getFileName().toString()).matches())
                    .filter(p -> Files.exists(p.resolve("index")))
                    .toList()
                    .stream()
                    .map(p -> CompletableFuture.supplyAsync(() -> getShardInformation(p), executor))
                    .toList();
            final List<ShardInformation> shards = awaitAll(futures).stream()
                    .sorted(Comparator.comparing(ShardInformation::name))
                    .collect(Collectors.toList());
            return new IndexInformation(path, indexID, state, shards);
        } catch (IOException e) {
            throw new IndexerInformationParserException("Failed to parse shard information", e);
        }
    }

    private ShardInformation getShardInformation(Path path) {
        // shardReader and getState are independent I/O operations — run them concurrently
        final CompletableFuture<ShardStats> statsFuture =
                CompletableFuture.supplyAsync(() -> shardReader.read(path), executor);
        final CompletableFuture<StateFile> stateFuture =
                CompletableFuture.supplyAsync(() -> getState(path, "state"), executor);
        awaitAllOf(statsFuture, stateFuture);
        final ShardStats shardStats = statsFuture.join();
        final StateFile state = stateFuture.join();
        return new ShardInformation(path, shardStats.documentsCount(), state, shardStats.minSegmentLuceneVersion());
    }

    private Optional<Path> findStateFile(Path stateDir, String stateFilePrefix) {
        try (Stream<Path> stateFiles = Files.list(stateDir.resolve(STATE_DIR_NAME))) {
            return stateFiles
                    .filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().startsWith(stateFilePrefix))
                    .filter(file -> file.getFileName().toString().endsWith(STATE_FILE_EXTENSION))
                    .findFirst();
        } catch (IOException e) {
            throw new IndexerInformationParserException("Failed to list state file of index" + stateDir, e);
        }
    }

    private <T> List<T> awaitAll(List<CompletableFuture<T>> futures) {
        awaitAllOf(futures.toArray(CompletableFuture[]::new));
        return futures.stream().map(CompletableFuture::join).toList();
    }

    private void awaitAllOf(CompletableFuture<?>... futures) {
        try {
            CompletableFuture.allOf(futures).join();
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            throw e;
        }
    }
}
