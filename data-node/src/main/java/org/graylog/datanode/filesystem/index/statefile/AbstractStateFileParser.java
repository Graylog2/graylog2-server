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
import org.graylog.datanode.filesystem.index.IndexerInformationParserException;
import org.graylog2.jackson.TypeReferences;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

/**
 * State files are smile documents wrapped in a Lucene codec frame (header, payload, checksum footer). Reading that
 * frame requires a Lucene version that still understands the format the file was written with, so the actual read
 * is delegated to the subclasses while the smile decoding is shared.
 */
abstract class AbstractStateFileParser implements StateFileParser {

    protected static final String STATE_FILE_CODEC = "state";
    protected static final int MIN_COMPATIBLE_STATE_FILE_VERSION = 1;
    protected static final int STATE_FILE_VERSION = 1;

    private final ObjectMapper objectMapper;

    protected AbstractStateFileParser() {
        this.objectMapper = new ObjectMapper(createSmileFactory());
    }

    private static SmileFactory createSmileFactory() {
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
            final Map<String, Object> document = objectMapper.readValue(readPayload(file), TypeReferences.MAP_STRING_OBJECT);
            return new StateFile(file, document);
        } catch (IOException e) {
            throw new IndexerInformationParserException("Failed to parse state file", e);
        }
    }

    /**
     * Verifies the codec frame of the given state file and returns the smile encoded document inside it.
     */
    protected abstract byte[] readPayload(Path file) throws IOException;
}
