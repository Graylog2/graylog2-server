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
package org.graylog.collectors.opamp.auth;

import com.google.errorprone.annotations.MustBeClosed;
import com.mongodb.client.model.IndexModel;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.InsertOneResult;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.graylog.collectors.CollectorsConfig;
import org.graylog.collectors.CollectorsConfigService;
import org.graylog.collectors.TokenSigningKey;
import org.graylog.collectors.db.EnrollmentTokenCreator;
import org.graylog.collectors.db.EnrollmentTokenDTO;
import org.graylog.collectors.opamp.rest.CreateEnrollmentTokenRequest;
import org.graylog.collectors.opamp.rest.EnrollmentTokenResponse;
import org.graylog.security.pki.Algorithm;
import org.graylog.security.pki.KeyUtils;
import org.graylog.security.pki.PemUtils;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.PaginatedList;
import org.graylog2.database.filtering.DbSortResolver;
import org.graylog2.database.pagination.MongoPaginationHelper;
import org.graylog2.database.utils.MongoUtils;
import org.graylog2.plugin.cluster.ClusterIdService;
import org.graylog2.security.encryption.EncryptedValueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.mongodb.client.model.Filters.eq;
import static org.graylog2.database.utils.MongoUtils.idEq;
import static org.graylog2.database.utils.MongoUtils.idsIn;
import static org.graylog2.database.utils.MongoUtils.stream;
import static org.graylog2.database.utils.MongoUtils.stringIdsIn;

/**
 * Service for accessing enrollment tokens.
 */
@Singleton
public class EnrollmentTokenService {

    private static final Logger LOG = LoggerFactory.getLogger(EnrollmentTokenService.class);

    private static final String AUDIENCE_SUFFIX = ":opamp";

    public static final String COLLECTION_NAME = "collector_enrollment_tokens";

    private final ClusterIdService clusterIdService;
    private final Clock clock;
    private final EncryptedValueService encryptedValueService;
    private final CollectorsConfigService collectorsConfigService;
    private final org.graylog2.database.MongoCollection<EnrollmentTokenDTO> tokenCollection;
    private final MongoPaginationHelper<EnrollmentTokenDTO> paginationHelper;

    @Inject
    public EnrollmentTokenService(ClusterIdService clusterIdService,
                                  Clock clock,
                                  EncryptedValueService encryptedValueService,
                                  CollectorsConfigService collectorsConfigService,
                                  MongoCollections mongoCollections) {
        this.clusterIdService = clusterIdService;
        this.clock = clock;
        this.encryptedValueService = encryptedValueService;
        this.collectorsConfigService = collectorsConfigService;
        this.tokenCollection = mongoCollections.collection(COLLECTION_NAME, EnrollmentTokenDTO.class);
        this.paginationHelper = mongoCollections.paginationHelper(tokenCollection);

        tokenCollection.createIndexes(List.of(
                new IndexModel(Indexes.ascending(EnrollmentTokenDTO.FIELD_JTI), new IndexOptions().unique(true)),
                new IndexModel(Indexes.ascending(EnrollmentTokenDTO.FIELD_FLEET_ID)),
                new IndexModel(Indexes.ascending(EnrollmentTokenDTO.FIELD_EXPIRES_AT))
        ));
    }

    /**
     * Creates a new token signing key.
     *
     * @return the new token signing key
     */
    public TokenSigningKey createTokenSigningKey() throws NoSuchAlgorithmException, NoSuchProviderException, IOException {
        final var keyPair = KeyUtils.generateKeyPair(Algorithm.ED25519);
        final var privateKeyPem = PemUtils.toPem(keyPair.getPrivate());
        final var publicKeyPem = PemUtils.toPem(keyPair.getPublic());

        return new TokenSigningKey(
                encryptedValueService.encrypt(privateKeyPem),
                publicKeyPem,
                KeyUtils.sha256Fingerprint(keyPair),
                Instant.now(clock)
        );
    }

    /**
     * Creates a new enrollment token for OpAMP agents.
     * <p>
     * The token is a JWT signed with the token signing private key.
     * The JWT contains:
     * <ul>
     *   <li>{@code kid} header: fingerprint of the signing key</li>
     *   <li>{@code iss}: cluster ID (informational, not validated)</li>
     *   <li>{@code aud}: {@code {cluster_id}:opamp} (validated on receipt)</li>
     *   <li>{@code iat}: current timestamp</li>
     *   <li>{@code exp}: expiration timestamp (if expiry specified)</li>
     *   <li>{@code jti}: unique token identifier</li>
     * </ul>
     *
     * @param request the token creation request
     * @param creator the user who created the token
     * @return the created token and its expiration time
     */
    public EnrollmentTokenResponse createToken(CreateEnrollmentTokenRequest request, EnrollmentTokenCreator creator) {
        final var tokenSigningKey = getTokenSigningKey();
        final Instant now = Instant.now(clock);
        final @Nullable Instant expiresAt = request.expiresIn() != null ? now.plus(request.expiresIn()) : null;

        final String jti = UUID.randomUUID().toString();
        final String token = buildJwt(tokenSigningKey, jti, now, expiresAt);

        final InsertOneResult insertOneResult = tokenCollection.insertOne(EnrollmentTokenDTO.builder()
                .name(request.name())
                .jti(jti)
                .kid(tokenSigningKey.fingerprint())
                .fleetId(request.fleetId())
                .createdBy(creator)
                .createdAt(now)
                .expiresAt(expiresAt)
                .build());

        return new EnrollmentTokenResponse(MongoUtils.insertedIdAsString(insertOneResult), token, expiresAt);
    }

    /**
     * Validates an enrollment token and extracts the token metadata.
     * <p>
     * Validation includes:
     * <ul>
     *   <li>Signature verification using the key identified by {@code kid} header</li>
     *   <li>Expiration check (handled automatically by JJWT)</li>
     *   <li>Audience validation (must be {@code {cluster_id}:opamp})</li>
     *   <li>JTI lookup against persisted metadata (token must not be deleted)</li>
     * </ul>
     *
     * @param token the JWT token string
     * @return the enrollment token metadata if valid, empty otherwise
     */
    public Optional<EnrollmentTokenDTO> validateToken(String token) {
        try {
            final String expectedAudience = clusterIdService.getString() + AUDIENCE_SUFFIX;

            final Jws<Claims> jws = Jwts.parser()
                    .clock(() -> Date.from(clock.instant()))
                    .keyLocator(header -> {
                        final String kid = (String) header.get("kid");
                        if (kid == null) {
                            throw new SecurityException("Missing kid header");
                        }
                        final var tokenSigningKey = getTokenSigningKey();

                        if (!tokenSigningKey.fingerprint().equals(kid)) {
                            throw new SecurityException("Unknown kid: " + kid);
                        }

                        try {
                            return PemUtils.parsePublicKey(tokenSigningKey.publicKey());
                        } catch (IOException e) {
                            throw new SecurityException("Failed to parse token signing key", e);
                        }
                    })
                    .requireAudience(expectedAudience)
                    .build()
                    .parseSignedClaims(token);

            final String jti = jws.getPayload().getId();
            if (jti == null || jti.isBlank()) {
                LOG.warn("Enrollment token validation failed: missing jti");
                return Optional.empty();
            }

            final EnrollmentTokenDTO metadata = tokenCollection.find(eq(EnrollmentTokenDTO.FIELD_JTI, jti)).first();
            if (metadata == null) {
                LOG.warn("Enrollment token validation failed: token metadata not found (deleted?)");
                return Optional.empty();
            }

            return Optional.of(metadata);
        } catch (Exception e) {
            LOG.warn("Enrollment token validation failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public boolean delete(String id) {
        return tokenCollection.deleteOne(idEq(id)).getDeletedCount() > 0;
    }

    public long deleteMany(List<String> ids) {
        return tokenCollection.deleteMany(stringIdsIn(ids)).getDeletedCount();
    }

    public void deleteAllByFleet(String fleetId) {
        tokenCollection.deleteMany(eq(EnrollmentTokenDTO.FIELD_FLEET_ID, fleetId));
    }

    public void incrementUsage(String id) {
        tokenCollection.updateOne(
                idEq(id),
                Updates.combine(
                        Updates.inc(EnrollmentTokenDTO.FIELD_USAGE_COUNT, 1),
                        Updates.set(EnrollmentTokenDTO.FIELD_LAST_USED_AT, Date.from(Instant.now(clock)))
                )
        );
    }

    public Optional<EnrollmentTokenDTO> findOne(String tokenId) {
        return Optional.ofNullable(tokenCollection.find(idEq(tokenId)).first());
    }

    public PaginatedList<EnrollmentTokenDTO> findPaginated(Bson query,
                                                           DbSortResolver.ResolvedSort resolvedSort,
                                                           int page, int perPage, Predicate<EnrollmentTokenDTO> filter) {
        return paginationHelper
                .filter(query)
                .sort(resolvedSort.sort())
                .pipeline(resolvedSort.preSortStages())
                .postSortPipeline(resolvedSort.postSortStages())
                .perPage(perPage)
                .page(page, filter);
    }

    private String buildJwt(TokenSigningKey tokenSigningKey, String jti, Instant issuedAt, @Nullable Instant expiresAt) {
        try {
            final var privateKey = PemUtils.parsePrivateKey(encryptedValueService.decrypt(tokenSigningKey.privateKey()));

            final String clusterId = clusterIdService.getString();
            final String audience = clusterId + AUDIENCE_SUFFIX;

            var builder = Jwts.builder()
                    .header()
                    .add("ctt", "enrollment")
                    .add("kid", tokenSigningKey.fingerprint())
                    .and()
                    .id(jti)
                    .issuer(clusterId)
                    .audience().add(audience)
                    .and()
                    .issuedAt(Date.from(issuedAt));

            if (expiresAt != null) {
                builder.expiration(Date.from(expiresAt));
            }

            return builder.signWith(privateKey).compact();
        } catch (Exception e) {
            throw new RuntimeException("Failed to build enrollment JWT", e);
        }
    }

    private TokenSigningKey getTokenSigningKey() {
        return collectorsConfigService.get()
                .map(CollectorsConfig::tokenSigningKey)
                .orElseThrow(() -> new IllegalStateException("Token signing key not found"));
    }

    @MustBeClosed
    public Stream<EnrollmentTokenDTO> findByIds(List<String> ids) {
        return stream(tokenCollection.find(idsIn(ids.stream().map(ObjectId::new).toList())));
    }
}
