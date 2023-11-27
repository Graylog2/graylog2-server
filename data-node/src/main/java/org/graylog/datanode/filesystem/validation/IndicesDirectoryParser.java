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
package org.graylog.datanode.filesystem.validation;

import org.graylog.datanode.filesystem.validation.dto.IndexInformation;
import org.graylog.datanode.filesystem.validation.dto.IndexerInformation;
import org.graylog.datanode.filesystem.validation.dto.NodeInformation;
import org.graylog.datanode.filesystem.validation.dto.ShardInformation;
import org.graylog.datanode.filesystem.validation.indexreader.ShardStats;
import org.graylog.datanode.filesystem.validation.indexreader.ShardStatsParser;
import org.graylog.datanode.filesystem.validation.statefile.StateFile;
import org.graylog.datanode.filesystem.validation.statefile.StateFileParser;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IndicesDirectoryParser {

    public static final String STATE_DIR_NAME = "_state";
    public static final String STATE_FILE_EXTENSION = ".st";

    private final StateFileParser stateFileParser;
    private final ShardStatsParser shardReader;

    @Inject
    public IndicesDirectoryParser(StateFileParser stateFileParser, ShardStatsParser shardReader) {
        this.stateFileParser = stateFileParser;
        this.shardReader = shardReader;
    }

    public IndexerInformation parse(Path path) {
        if (!Files.exists(path)) {
            throw new IndexerInformationParserException("Path " + path + " is not a directory");
        }

        if (!Files.isReadable(path)) {
            throw new IndexerInformationParserException("Path " + path + " is not readable");
        }

        try (final Stream<Path> nodes = Files.list(path.resolve("nodes"))) {
            final List<NodeInformation> nodeInformation = nodes.filter(Files::isDirectory)
                    .filter(p -> p.getFileName().toString().matches("\\d+"))
                    .map(this::parseNode)
                    .toList();
            return new IndexerInformation(path, nodeInformation);
        } catch (IOException e) {
            throw new IndexerInformationParserException("Failed to list nodes", e);
        }
    }

    private NodeInformation parseNode(Path nodePath) {
        try (Stream<Path> indicesDirs = Files.list(nodePath.resolve("indices"))) {
            final StateFile state = getState(nodePath, "node");
            final List<IndexInformation> indices = indicesDirs
                    .map(this::parseIndex)
                    .collect(Collectors.toList());
            return new NodeInformation(nodePath, indices, state);
        } catch (IOException e) {
            throw new IndexerInformationParserException("Failed to list indices directories", e);
        }
    }

    private StateFile getState(Path path, String stateFilePrefix) {
        final Path stateFile = findStateFile(path, stateFilePrefix);
        return stateFileParser.parse(stateFile);
    }

    private IndexInformation parseIndex(Path path) {
        final String indexID = path.getFileName().toString();
        final StateFile state = getState(path, "state");
        try (Stream<Path> shardDirs = Files.list(path)) {
            final List<ShardInformation> shards = shardDirs
                    .filter(Files::isDirectory)
                    .filter(p -> p.getFileName().toString().matches("\\d+"))
                    .filter(p -> Files.exists(p.resolve("index")))
                    .map(this::getShardInformation)
                    .collect(Collectors.toList());
            return new IndexInformation(path, indexID, state, shards);
        } catch (IOException e) {
            throw new IndexerInformationParserException("Failed to parse shard information", e);
        }
    }

    private ShardInformation getShardInformation(Path path) {
        final ShardStats shardStats = shardReader.read(path);
        final StateFile state = getState(path, "state");
        return new ShardInformation(path, shardStats.documentsCount(), state, shardStats.minSegmentLuceneVersion());
    }

    private Path findStateFile(Path stateDir, String stateFilePrefix) {
        try (Stream<Path> stateFiles = Files.list(stateDir.resolve(STATE_DIR_NAME))) {
            return stateFiles
                    .filter(Files::isRegularFile)
                    .filter(file -> file.getFileName().toString().startsWith(stateFilePrefix))
                    .filter(file -> file.getFileName().toString().endsWith(STATE_FILE_EXTENSION))
                    .findFirst()
                    .orElseThrow(() -> new IndexerInformationParserException("No state file available in dir  " + stateDir));
        } catch (IOException e) {
            throw new IndexerInformationParserException("Failed to list state file of index" + stateDir, e);
        }
    }
}
