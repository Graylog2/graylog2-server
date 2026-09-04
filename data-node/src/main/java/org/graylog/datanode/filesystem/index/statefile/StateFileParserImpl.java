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

import jakarta.inject.Singleton;
import org.apache.lucene.backward_codecs.store.EndiannessReverserUtil;
import org.apache.lucene.codecs.CodecUtil;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;

import java.io.IOException;
import java.nio.file.Path;

@Singleton
public class StateFileParserImpl extends AbstractStateFileParser {

    @Override
    protected byte[] readPayload(Path file) throws IOException {
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
            return contentBytes;
        }
    }
}
