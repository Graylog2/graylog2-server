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
package org.graylog.collectors.db;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import jakarta.annotation.Nullable;
import org.graylog2.database.BuildableMongoEntity;
import org.mongojack.ObjectId;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@AutoValue
@JsonDeserialize(builder = CollectorInstanceDTO.Builder.class)
public abstract class CollectorInstanceDTO implements BuildableMongoEntity<CollectorInstanceDTO, CollectorInstanceDTO.Builder> {
    public static final String FIELD_INSTANCE_UID = "instance_uid";
    public static final String FIELD_MESSAGE_SEQ_NUM = "message_seq_num";
    public static final String FIELD_CAPABILITIES = "capabilities";
    public static final String FIELD_LAST_SEEN = "last_seen";
    public static final String FIELD_FLEET_ID = "fleet_id";
    public static final String FIELD_CERTIFICATE_FINGERPRINT = "certificate_fingerprint";
    public static final String FIELD_CERTIFICATE_PEM = "certificate_pem";
    public static final String FIELD_ISSUING_CA_ID = "issuing_ca_id";
    public static final String FIELD_ENROLLED_AT = "enrolled_at";
    public static final String FIELD_IDENTIFYING_ATTRIBUTES = "identifying_attributes";
    public static final String FIELD_NON_IDENTIFYING_ATTRIBUTES = "non_identifying_attributes";
    public static final String FIELD_LAST_PROCESSED_TXN_SEQ = "last_processed_txn_seq";


    @JsonProperty(FIELD_INSTANCE_UID)
    public abstract String instanceUid();

    @JsonProperty(FIELD_MESSAGE_SEQ_NUM)
    public abstract long messageSeqNum();

    @JsonProperty(FIELD_CAPABILITIES)
    public abstract long capabilities();

    @JsonProperty(FIELD_LAST_SEEN)
    public abstract Instant lastSeen();

    @JsonProperty(FIELD_FLEET_ID)
    public abstract String fleetId();

    @JsonProperty(FIELD_CERTIFICATE_FINGERPRINT)
    public abstract String certificateFingerprint();

    @JsonProperty(FIELD_CERTIFICATE_PEM)
    public abstract String certificatePem();

    @ObjectId
    @JsonProperty(FIELD_ISSUING_CA_ID)
    public abstract String issuingCaId();

    @JsonProperty(FIELD_ENROLLED_AT)
    public abstract Instant enrolledAt();

    @JsonProperty(FIELD_IDENTIFYING_ATTRIBUTES)
    public abstract Optional<List<Attribute>> identifyingAttributes();

    @JsonProperty(FIELD_NON_IDENTIFYING_ATTRIBUTES)
    public abstract Optional<List<Attribute>> nonIdentifyingAttributes();

    @JsonProperty(FIELD_LAST_PROCESSED_TXN_SEQ)
    public abstract long lastProcessedTxnSeq();

    public static Builder builder() {
        return AutoValue_CollectorInstanceDTO.Builder.create();
    }


    @AutoValue.Builder
    public abstract static class Builder implements BuildableMongoEntity.Builder<CollectorInstanceDTO, CollectorInstanceDTO.Builder> {

        @JsonCreator
        public static Builder create() {
            return new AutoValue_CollectorInstanceDTO.Builder()
                    .messageSeqNum(0L)
                    .lastProcessedTxnSeq(0L);
        }

        @JsonProperty(FIELD_INSTANCE_UID)
        public abstract Builder instanceUid(String instanceUid);

        @JsonProperty(FIELD_MESSAGE_SEQ_NUM)
        public abstract Builder messageSeqNum(long messageSeqNum);

        @JsonProperty(FIELD_CAPABILITIES)
        public abstract Builder capabilities(long capabilities);

        @JsonProperty(FIELD_LAST_SEEN)
        public abstract Builder lastSeen(Instant lastSeen);

        @JsonProperty(FIELD_FLEET_ID)
        public abstract Builder fleetId(String fleetId);

        @JsonProperty(FIELD_CERTIFICATE_FINGERPRINT)
        public abstract Builder certificateFingerprint(String certificateFingerprint);

        @JsonProperty(FIELD_CERTIFICATE_PEM)
        public abstract Builder certificatePem(String certificatePem);

        @ObjectId
        @JsonProperty(FIELD_ISSUING_CA_ID)
        public abstract Builder issuingCaId(String issuingCaId);

        @JsonProperty(FIELD_ENROLLED_AT)
        public abstract Builder enrolledAt(Instant enrolledAt);

        @JsonProperty(FIELD_IDENTIFYING_ATTRIBUTES)
        public abstract Builder identifyingAttributes(@Nullable List<Attribute> identifyingAttributes);

        @JsonProperty(FIELD_NON_IDENTIFYING_ATTRIBUTES)
        public abstract Builder nonIdentifyingAttributes(@Nullable List<Attribute> nonIdentifyingAttributes);

        @JsonProperty(FIELD_LAST_PROCESSED_TXN_SEQ)
        public abstract Builder lastProcessedTxnSeq(long lastProcessedTxnSeq);

        public abstract CollectorInstanceDTO build();
    }
}
