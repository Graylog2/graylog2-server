/**
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.graylog2.plugin.journal;

import com.eaio.uuid.UUID;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog2.plugin.Tools;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Serializable;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * A raw message is the unparsed data Graylog2 was handed by an input.
 * <p>
 * Typically this is a copy of the exact bytes received over the network, after all de-chunking, removal of transport
 * headers, etc has been performed, but before any parsing, decoding, checking of the actual payload has been performed.
 * </p>
 * <p>
 * Each raw message has a unique id, a timestamp it was received at (this might be different from the timestamp contained
 * in the payload, if that has any), a tag of what payload type this is supposed to be (e.g. syslog, GELF, RadioMessage etc.),
 * as well as an opaque meta data structure.<br>
 * The format of the meta data is not stable yet, but will likely be a JSON string.
 * </p>
 */
public class RawMessage implements Serializable {

    public static final byte CURRENT_VERSION = 1;

    public static final InetSocketAddress LOCALHOST_ANYPORT = new InetSocketAddress(0);

    private final long sequenceNumber;
    private final byte version;
    private final UUID id;
    private final DateTime timestamp;
    private final String sourceInputId;
    private final InetSocketAddress remoteAddress;
    private final String metaData;
    private final String payloadType;
    private final byte[] payload;
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public RawMessage(String payloadType,
                      String sourceInputId,
                      InetSocketAddress remoteAddress,
                      byte[] payload) {
        this(payloadType, sourceInputId, remoteAddress, null, payload);
    }

    public RawMessage(String payloadType,
                      String sourceInputId,
                      InetSocketAddress remoteAddress,
                      @Nullable String metaData,
                      byte[] payload) {
        this(Long.MIN_VALUE, new UUID(), Tools.iso8601(), payloadType, sourceInputId, remoteAddress, metaData, payload);
    }

    public RawMessage(long sequenceNumber,
                      UUID id,
                      DateTime timestamp,
                      String payloadType,
                      String sourceInputId,
                      InetSocketAddress remoteAddress,
                      @Nullable String metaData,
                      byte[] payload) {
        this.sequenceNumber = sequenceNumber;
        checkNotNull(payload, "The messsage payload must not be null!");
        checkArgument(payload.length > 0, "The message payload must not be empty!");
        checkArgument(!isNullOrEmpty(payloadType), "The payload type must not be null or empty!");

        this.version = CURRENT_VERSION;
        this.id = id;
        this.timestamp = timestamp;
        this.sourceInputId = sourceInputId;
        this.remoteAddress = firstNonNull(remoteAddress, LOCALHOST_ANYPORT);
        this.metaData = metaData == null ? "" : metaData;
        this.payloadType = payloadType;
        this.payload = payload.clone();
    }

    public byte[] encode() {
        final byte[] sourceInputIdBytes = sourceInputId.getBytes(UTF_8);
        final byte[] metaDataBytes = metaData.getBytes(UTF_8);
        final byte[] payloadTypeBytes = payloadType.getBytes(UTF_8);

        final int bufferSize =
                1 + /* version */
                        16 + /* UUID is 2 longs */
                        8 + /* timestamp is 1 long, in millis from 1970 */
                        4 + /* source input id length */
                        sourceInputIdBytes.length + /* source input id string. TODO could this be a proper UUID instead? would save many bytes*/
                        1 + /* inet sock address length */
                        remoteAddress.getAddress().getAddress().length + /* inet sock address byte representation */
                        2 + /* source port */
                        4 + /* payload type length */
                        payloadTypeBytes.length + /* utf-8 encoded name of the payload type */
                        4 + /* size of metadata, one int */
                        metaDataBytes.length + /* number of bytes of UTF-8 encoded metadata, or 0 */
                        4 + /* size of payload, one int */
                        payload.length; /* raw length of payload data */

        return ByteBuffer.allocate(bufferSize)
                .put(version)
                .putLong(id.getTime())
                .putLong(id.getClockSeqAndNode())
                .putLong(timestamp.getMillis())
                .putInt(payloadTypeBytes.length)
                .put(payloadTypeBytes)
                .putInt(sourceInputIdBytes.length)
                .put(sourceInputIdBytes)
                .put((byte) (remoteAddress.getAddress() instanceof Inet4Address ? 4 : 16))
                .put(remoteAddress.getAddress().getAddress())
                .putInt(remoteAddress.getPort())
                .putInt(metaDataBytes.length)
                .put(metaDataBytes)
                .putInt(payload.length)
                .put(payload)
                .array();
    }

    public static RawMessage decode(final ByteBuffer buffer, final long sequenceNumber) {

        try {
            final byte version = buffer.get();
            if (version > CURRENT_VERSION) {
                throw new IllegalArgumentException("Cannot decode raw message with version " + version +
                                                           " this decoder only supports up to version " + CURRENT_VERSION);
            }

            final long time = buffer.getLong();
            final long clockSeqAndNode = buffer.getLong();

            final long millis = buffer.getLong();

            final int payloadTypeLength = buffer.getInt();
            final byte[] payloadType = new byte[payloadTypeLength];
            buffer.get(payloadType);

            final int sourceInputLength = buffer.getInt();
            final byte[] sourceInput = new byte[sourceInputLength];
            buffer.get(sourceInput);

            final byte addressLength = buffer.get();
            final byte[] address = new byte[addressLength];
            buffer.get(address);

            final int port = buffer.getInt();

            final int metaDataLength = buffer.getInt();
            final byte[] metaData = new byte[metaDataLength];
            buffer.get(metaData);

            final int payloadLength = buffer.getInt();
            final byte[] payload = new byte[payloadLength];
            buffer.get(payload);

            return new RawMessage(
                    sequenceNumber,
                    new UUID(time, clockSeqAndNode),
                    new DateTime(millis, DateTimeZone.UTC),
                    new String(payloadType, UTF_8),
                    new String(sourceInput, UTF_8),
                    new InetSocketAddress(InetAddress.getByAddress(address), port),
                    new String(metaData, UTF_8),
                    payload);

        } catch (IndexOutOfBoundsException e) {
            throw new IllegalStateException("Cannot decode truncated raw message.", e);
        } catch (UnknownHostException e) {
            throw new IllegalStateException("Cannot decode raw message, malformed InetSockAddress", e);
        }
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }

    public int getVersion() {
        return version;
    }

    public DateTime getTimestamp() {
        return timestamp;
    }

    public String getSourceInputId() {
        return sourceInputId;
    }

    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    public String getMetaData() {
        return metaData;
    }

    @Nonnull
    public Map<String, Object> getParsedMetaData() {
        if (getMetaData() == null) {
            return Collections.emptyMap();
        }
        try {
            return OBJECT_MAPPER.readValue(getMetaData(), new TypeReference<Map<String, Object>>() {});
        } catch (IOException e) {
            return Collections.emptyMap();
        }
    }

    public String getPayloadType() {
        return payloadType;
    }

    public byte[] getPayload() {
        return payload;
    }

    public UUID getId() {
        return id;
    }

    public byte[] getIdBytes() {
        final long time = id.getTime();
        final long clockSeqAndNode = id.getClockSeqAndNode();

        return ByteBuffer.allocate(16)
                .putLong(time)
                .putLong(clockSeqAndNode)
                .array();
    }

    @Override
    public String toString() {
        return "RawMessage{" +
                "version=" + version +
                ", id=" + id +
                ", timestamp=" + timestamp +
                ", sourceInputId='" + sourceInputId + '\'' +
                ", metaData='" + metaData + '\'' +
                ", payloadType='" + payloadType + '\'' +
                ", payload.length=" + payload.length +
                '}';
    }
}
