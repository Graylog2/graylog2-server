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
package org.graylog2.database.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.BsonDocument;
import org.bson.BsonDocumentReader;
import org.bson.BsonDocumentWriter;
import org.bson.BsonObjectId;
import org.bson.UuidRepresentation;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.mongojack.internal.stream.JacksonCodec;
import org.mongojack.internal.stream.JacksonDecoder;
import org.mongojack.internal.stream.JacksonEncoder;

/**
 * A custom implementation of Mongojack's {@link JacksonCodec} that doesn't attempt to modify potentially immutable
 * domain objects, because that sometimes just isn't possible, e.g. for {@link Record}s.
 */
public class CustomJacksonCodec<T> extends JacksonCodec<T> {
    private final JacksonEncoder<T> encoder;
    private final JacksonDecoder<T> decoder;
    private final ObjectMapper objectMapper;
    private final CustomJacksonCodecRegistry jacksonCodecRegistry;

    public CustomJacksonCodec(JacksonEncoder<T> encoder, JacksonDecoder<T> decoder, final ObjectMapper objectMapper,
                              CustomJacksonCodecRegistry jacksonCodecRegistry) {
        super(encoder, decoder, objectMapper, jacksonCodecRegistry);
        this.encoder = encoder;
        this.decoder = decoder;
        this.objectMapper = objectMapper;
        this.jacksonCodecRegistry = jacksonCodecRegistry;
    }

    @Override
    public T generateIdIfAbsentFromDocument(T t) {
        if (documentHasId(t)) {
            return t;
        }

        try (final var writer = new BsonDocumentWriter(new BsonDocument())) {
            encode(writer, t, EncoderContext.builder().build());
            final BsonDocument document = writer.getDocument();
            document.put("_id", new BsonObjectId());
            try (final var reader = new BsonDocumentReader(document)) {
                return decode(reader, DecoderContext.builder().build());
            }
        }
    }

    @Override
    public Codec<T> withUuidRepresentation(final UuidRepresentation uuidRepresentation) {
        return new CustomJacksonCodec<>(
                encoder.withUuidRepresentation(uuidRepresentation),
                decoder.withUuidRepresentation(uuidRepresentation),
                objectMapper,
                jacksonCodecRegistry
        );
    }

}
