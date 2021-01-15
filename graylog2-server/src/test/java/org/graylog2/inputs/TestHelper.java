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
package org.graylog2.inputs;

import com.google.common.io.ByteStreams;
import org.graylog2.inputs.codecs.gelf.GELFMessageChunk;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

public class TestHelper {

    public static byte[] zlibCompress(String what, int level) throws IOException {
        final ByteArrayInputStream compressMe = new ByteArrayInputStream(what.getBytes(StandardCharsets.UTF_8));
        final ByteArrayOutputStream compressedMessage = new ByteArrayOutputStream();

        try (DeflaterOutputStream out = new DeflaterOutputStream(compressedMessage, new Deflater(level))) {
            ByteStreams.copy(compressMe, out);
        }

        return compressedMessage.toByteArray();
    }

    public static byte[] zlibCompress(String what) throws IOException {
        return zlibCompress(what, -1);
    }

    public static byte[] gzipCompress(String what) throws IOException {
        // GZIP compress message.
        final ByteArrayInputStream compressMe = new ByteArrayInputStream(what.getBytes(StandardCharsets.UTF_8));
        final ByteArrayOutputStream compressedMessage = new ByteArrayOutputStream();

        try (GZIPOutputStream out = new GZIPOutputStream(compressedMessage)) {
            ByteStreams.copy(compressMe, out);
        }

        return compressedMessage.toByteArray();
    }

    public static byte[] buildGELFMessageChunk(String idString, int seqNum, int seqCnt, byte[] data) throws Exception {
        byte[] id = idString.getBytes(StandardCharsets.UTF_8);
        if (id.length != 8) {
            throw new Exception("IN TEST HELPER!!! YOU WROTE WRONG YOUR TESTS! - id must consist of 8 byte. length is: " + id.length);
        }

        byte[] chunkData = new byte[GELFMessageChunk.HEADER_TOTAL_LENGTH + data.length]; // Magic bytes/header + data
        chunkData[0] = (byte) 0x1e;
        chunkData[1] = (byte) 0x0f;

        // 2,3,4,5,6,7,8,9 id
        System.arraycopy(id, 0, chunkData, 2, id.length);

        // 10 seq num
        chunkData[10] = (byte) seqNum;

        // 11 seq cnt
        chunkData[11] = (byte) seqCnt;

        // 12... data
        System.arraycopy(data, 0, chunkData, 12, data.length);

        return chunkData;
    }

    public static String toHex(String arg) {
        return String.format(Locale.ENGLISH, "%x", new BigInteger(arg.getBytes(StandardCharsets.UTF_8)));
    }

    public static Message simpleLogMessage() {
        return new Message("bar", "foo", Tools.nowUTC());
    }

}
