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
package org.graylog2.streams;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.graylog2.plugin.streams.Output;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.streams.StreamRule;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.List;

public class StreamListFingerprint {
    private final String fingerprint;

    public StreamListFingerprint(List<Stream> streams) {
        this.fingerprint = buildFingerprint(streams);
    }

    public String getFingerprint() {
        return fingerprint;
    }

    private String buildFingerprint(List<Stream> streams) {
        final MessageDigest sha1Digest = DigestUtils.getSha1Digest();

        final StringBuilder sb = new StringBuilder();
        for (Stream stream : Ordering.from(getStreamComparator()).sortedCopy(streams)) {
            sb.append(stream.hashCode());

            for (StreamRule rule : Ordering.from(getStreamRuleComparator()).sortedCopy(stream.getStreamRules())) {
                sb.append(rule.hashCode());
            }
            for (Output output : Ordering.from(getOutputComparator()).sortedCopy(stream.getOutputs())) {
                sb.append(output.hashCode());
            }
        }
        return String.valueOf(Hex.encodeHex(sha1Digest.digest(sb.toString().getBytes(StandardCharsets.US_ASCII))));
    }

    private Comparator<Output> getOutputComparator() {
        return new Comparator<Output>() {
            @Override
            public int compare(Output output1, Output stream2) {
                return comparisonResult(output1.getId(), stream2.getId());
            }
        };
    }

    private Comparator<Stream> getStreamComparator() {
        return new Comparator<Stream>() {
            @Override
            public int compare(Stream stream1, Stream stream2) {
                return comparisonResult(stream1.getId(), stream2.getId());
            }
        };
    }

    private Comparator<StreamRule> getStreamRuleComparator() {
        return new Comparator<StreamRule>() {
                @Override
                public int compare(StreamRule rule1, StreamRule rule2) {
                    return comparisonResult(rule1.getId(), rule2.getId());
                }
            };
    }

    private int comparisonResult(String id1, String id2) {
        return ComparisonChain.start()
                .compare(id1, id2, String.CASE_INSENSITIVE_ORDER)
                .compare(id1, id2)
                .result();
    }
}
