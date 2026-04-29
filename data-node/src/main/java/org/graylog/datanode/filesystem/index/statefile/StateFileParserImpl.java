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
package org.graylog.datanode.filesystem.index.statefile;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.fasterxml.jackson.dataformat.smile.SmileGenerator;
import jakarta.inject.Singleton;
import org.apache.lucene.backward_codecs.store.EndiannessReverserUtil;
import org.apache.lucene.codecs.CodecUtil;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.graylog.datanode.filesystem.index.IndexerInformationParserException;
import org.graylog2.jackson.TypeReferences;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

@Singleton
public class StateFileParserImpl implements StateFileParser {

    private static final String STATE_FILE_CODEC = "state";
    private static final int MIN_COMPATIBLE_STATE_FILE_VERSION = 1;
    private static final int STATE_FILE_VERSION = 1;

    private final ObjectMapper objectMapper;

    public StateFileParserImpl() {
        this.objectMapper = new ObjectMapper(createSmileFactory());
    }

    private SmileFactory createSmileFactory() {
        final SmileFactory factory = new SmileFactory();
        // for now, this is an overhead, might make sense for web sockets
        factory.configure(SmileGenerator.Feature.ENCODE_BINARY_AS_7BIT, false);
        factory.configure(SmileFactory.Feature.FAIL_ON_SYMBOL_HASH_OVERFLOW, false); // this trips on many mappings now...
        // Do not automatically close unclosed objects/arrays in com.fasterxml.jackson.dataformat.smile.SmileGenerator#close() method
        factory.configure(JsonGenerator.Feature.AUTO_CLOSE_JSON_CONTENT, false);
        factory.configure(JsonParser.Feature.STRICT_DUPLICATE_DETECTION, true);
        factory.setStreamReadConstraints(StreamReadConstraints.builder().maxStringLength(50000000).build());
        factory.configure(StreamReadFeature.USE_FAST_DOUBLE_PARSER.mappedFeature(), true);
        return factory;
    }

    @Override
    public StateFile parse(Path file) throws IndexerInformationParserException {
        try {
            return parseStateFile(file);
        } catch (IOException e) {
            throw new IndexerInformationParserException("Failed to parse state file", e);
        }
    }

    private StateFile parseStateFile(Path file) throws IOException {
        final Path dir = file.getParent();
        final String filename = file.getFileName().toString();
        try (
                FSDirectory directory = FSDirectory.open(dir);
                // IOContext.READONCE signals sequential single-pass access, enabling read-ahead
                IndexInput indexInput = EndiannessReverserUtil.openInput(directory, filename, IOContext.READONCE)
        ) {
            // We checksum the entire file before we even go and parse it. If it's corrupted we barf right here.
            CodecUtil.checksumEntireFile(indexInput);
            CodecUtil.checkHeader(indexInput, STATE_FILE_CODEC, MIN_COMPATIBLE_STATE_FILE_VERSION, STATE_FILE_VERSION);
            indexInput.skipBytes(Integer.BYTES); // xcontentType, not used
            final long filePointer = indexInput.getFilePointer();
            final int contentSize = Math.toIntExact(indexInput.length() - CodecUtil.footerLength() - filePointer);
            final byte[] contentBytes = new byte[contentSize];
            indexInput.readBytes(contentBytes, 0, contentSize);
            final Map<String, Object> readValue = objectMapper.readValue(contentBytes, TypeReferences.MAP_STRING_OBJECT);
            return new StateFile(file, readValue);
        }
    }
}
