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
package org.graylog.integrations.aws;

import org.graylog.integrations.aws.codecs.KinesisCloudWatchFlowLogCodec;
import org.graylog.integrations.aws.codecs.KinesisRawLogCodec;
import org.graylog.integrations.aws.transports.KinesisTransport;
import org.graylog2.plugin.inputs.codecs.Codec;
import org.graylog2.plugin.inputs.transports.Transport;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Identifies the type of input for a particular log source (eg. CloudWatch or Kinesis) and
 * log format.
 *
 * This type will be saved with the input to indicate which transport and codec should be used.
 */
public enum AWSMessageType {

    /**
     * A raw string stored in CloudWatch or Kinesis.
     */
    KINESIS_RAW(Source.KINESIS, "Kinesis Raw", KinesisRawLogCodec.NAME,
                KinesisRawLogCodec.Factory.class, KinesisTransport.NAME, KinesisTransport.Factory.class),

    KINESIS_CLOUDWATCH_RAW(Source.KINESIS, "Kinesis CloudWatch Raw", KinesisRawLogCodec.NAME,
                           KinesisRawLogCodec.Factory.class, KinesisTransport.NAME, KinesisTransport.Factory.class),

    // Flow Logs delivered to Kinesis via CloudWatch subscriptions.
    KINESIS_CLOUDWATCH_FLOW_LOGS(Source.KINESIS, "Kinesis CloudWatch Flow Log", KinesisCloudWatchFlowLogCodec.NAME,
                                 KinesisCloudWatchFlowLogCodec.Factory.class, KinesisTransport.NAME, KinesisTransport.Factory.class),

    UNKNOWN();

    private Source source;
    private String label;
    private String codecName;
    private Class<? extends Codec.Factory> codecFactory;
    private String transportName;
    private Class<? extends Transport.Factory> transportFactory;

    AWSMessageType() {
    }

    AWSMessageType(Source source, String label, String codecName, Class<? extends Codec.Factory> codecFactory, String transportName, Class<? extends Transport.Factory> transportFactory) {
        this.source = source;
        this.label = label;
        this.codecName = codecName;
        this.codecFactory = codecFactory;
        this.transportName = transportName;
        this.transportFactory = transportFactory;
    }

    public Class<? extends Codec.Factory> getCodecFactory() {
        return codecFactory;
    }

    public Class<? extends Transport.Factory> getTransportFactory() {
        return transportFactory;
    }

    public String getLabel() {
        return label;
    }

    public String getCodecName() {
        return codecName;
    }

    public String getTransportName() {
        return transportName;
    }

    /**
     * @return True if the {@link AWSMessageType} enum instance has a Kinesis source.
     */
    public boolean isKinesis() {
        return Source.KINESIS.equals(this.source);
    }

    /**
     * Each source will require its own specific set of input configuration fields. Multiple {@link AWSMessageType}
     * enum instances might correspond to the same source and thus require the same configuration fields.
     * For example, {@code AWSMessageType.KINESIS_RAW} and {@code AWSMessageType.KINESIS_FLOW_LOGS}
     * correspond to two different types of AWS messages, but share a common KINESIS source.
     */
    public enum Source {
        KINESIS
    }

    /**
     * @return Return all message types except for UNKNOWN.
     */
    public static List<AWSMessageType> getMessageTypes() {

        return Arrays.stream(values()).filter(m -> !m.equals(UNKNOWN)).collect(Collectors.toList());
    }
}