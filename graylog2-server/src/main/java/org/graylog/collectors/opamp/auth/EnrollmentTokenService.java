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

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexModel;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Updates;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.graylog.collectors.db.EnrollmentTokenCreator;
import org.graylog.collectors.db.EnrollmentTokenDTO;
import org.graylog.collectors.opamp.OpAmpCaService;
import org.graylog.collectors.opamp.rest.CreateEnrollmentTokenRequest;
import org.graylog.collectors.opamp.rest.EnrollmentTokenResponse;
import org.graylog.security.pki.CertificateEntry;
import org.graylog.security.pki.CertificateService;
import org.graylog.security.pki.PemUtils;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.PaginatedList;
import org.graylog2.database.filtering.DbSortResolver;
import org.graylog2.database.pagination.MongoPaginationHelper;
import org.graylog2.plugin.cluster.ClusterIdService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.PrivateKey;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Service for accessing enrollment certificates.
 * <p>
 * The CA hierarchy is created on-demand when the first enrollment token is requested.
 * This avoids creating certificates for deployments that don't use OpAMP enrollment.
 */
@Singleton
public class EnrollmentTokenService {

    private static final Logger LOG = LoggerFactory.getLogger(EnrollmentTokenService.class);

    private static final String AUDIENCE_SUFFIX = ":opamp";

    public static final String COLLECTION_NAME = "collector_enrollment_tokens";

    private final CertificateService certificateService;
    private final ClusterIdService clusterIdService;
    private final OpAmpCaService opAmpCaService;
    private final Clock clock;
    private final org.graylog2.database.MongoCollection<EnrollmentTokenDTO> tokenCollection;
    private final MongoPaginationHelper<EnrollmentTokenDTO> paginationHelper;

    @Inject
    public EnrollmentTokenService(CertificateService certificateService,
                                  ClusterIdService clusterIdService,
                                  OpAmpCaService opAmpCaService,
                                  Clock clock,
                                  MongoCollections mongoCollections) {
        this.certificateService = certificateService;
        this.clusterIdService = clusterIdService;
        this.opAmpCaService = opAmpCaService;
        this.clock = clock;
        this.tokenCollection = mongoCollections.collection(COLLECTION_NAME, EnrollmentTokenDTO.class);
        this.paginationHelper = mongoCollections.paginationHelper(tokenCollection);
        tokenCollection.createIndexes(List.of(
                new IndexModel(Indexes.ascending(EnrollmentTokenDTO.FIELD_JTI), new IndexOptions().unique(true)),
                new IndexModel(Indexes.ascending(EnrollmentTokenDTO.FIELD_FLEET_ID)),
                new IndexModel(Indexes.ascending(EnrollmentTokenDTO.FIELD_EXPIRES_AT))
        ));
    }

    /**
     * Returns the token signing certificate for signing enrollment JWTs.
     * <p>
     * Delegates to {@link OpAmpCaService} which creates the CA hierarchy on first call if it doesn't exist.
     *
     * @return the token signing certificate entry
     */
    public CertificateEntry getTokenSigningCert() {
        return opAmpCaService.getTokenSigningCert();
    }

    /**
     * Returns the certificate for signing agent enrollment CSRs.
     * <p>
     * Delegates to {@link OpAmpCaService} which creates the CA hierarchy on first call if it doesn't exist.
     *
     * @return the enrollment CA certificate entry
     */
    public CertificateEntry getEnrollmentSigningCert() {
        return opAmpCaService.getSigningCert();
    }

    /**
     * Creates a new enrollment token for OpAMP agents.
     * <p>
     * The token is a JWT signed with the token signing certificate's private key.
     * The JWT contains:
     * <ul>
     *   <li>{@code kid} header: fingerprint of the signing certificate</li>
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
     * @throws IllegalArgumentException if the requested expiry exceeds the signing certificate's validity
     */
    public EnrollmentTokenResponse createToken(CreateEnrollmentTokenRequest request, EnrollmentTokenCreator creator) {
        final CertificateEntry signingCert = opAmpCaService.getTokenSigningCert();

        final Instant now = Instant.now(clock);
        final @Nullable Instant expiresAt = request.expiresIn() != null ? now.plus(request.expiresIn()) : null;

        // Validate against cert expiry only when token has an expiry
        if (expiresAt != null) {
            final Instant certExpiry = signingCert.notAfter();
            if (expiresAt.isAfter(certExpiry)) {
                throw new IllegalArgumentException(
                        f("Token expiry %s exceeds signing certificate expiry %s", expiresAt, certExpiry));
            }
        }

        final String jti = UUID.randomUUID().toString();
        final String token = buildJwt(signingCert, jti, now, expiresAt);

        tokenCollection.insertOne(new EnrollmentTokenDTO(null, jti, signingCert.fingerprint(),
                request.fleetId(), creator, now, expiresAt, 0, null));

        return new EnrollmentTokenResponse(token, expiresAt);
    }

    /**
     * Validates an enrollment token and extracts the token metadata.
     * <p>
     * Validation includes:
     * <ul>
     *   <li>Signature verification using the cert identified by {@code kid} header</li>
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
                        return certificateService.findByFingerprint(kid)
                                .map(cert -> {
                                    try {
                                        return PemUtils.parseCertificate(cert.certificate()).getPublicKey();
                                    } catch (Exception e) {
                                        throw new SecurityException("Failed to parse certificate", e);
                                    }
                                })
                                .orElseThrow(() -> new SecurityException("Unknown kid: " + kid));
                    })
                    .requireAudience(expectedAudience)
                    .build()
                    .parseSignedClaims(token);

            final String jti = jws.getPayload().getId();
            if (jti == null || jti.isBlank()) {
                LOG.warn("Enrollment token validation failed: missing jti");
                return Optional.empty();
            }

            final EnrollmentTokenDTO metadata = tokenCollection.find(
                    Filters.eq(EnrollmentTokenDTO.FIELD_JTI, jti)).first();
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
        return tokenCollection.deleteOne(Filters.eq("_id", new ObjectId(id))).getDeletedCount() > 0;
    }

    public void deleteAllByFleet(String fleetId) {
        tokenCollection.deleteMany(Filters.eq(EnrollmentTokenDTO.FIELD_FLEET_ID, fleetId));
    }

    public void incrementUsage(String id) {
        tokenCollection.updateOne(
                Filters.eq("_id", new ObjectId(id)),
                Updates.combine(
                        Updates.inc(EnrollmentTokenDTO.FIELD_USAGE_COUNT, 1),
                        Updates.set(EnrollmentTokenDTO.FIELD_LAST_USED_AT, Date.from(Instant.now(clock)))
                )
        );
    }

    public PaginatedList<EnrollmentTokenDTO> findPaginated(Bson query,
                                                           DbSortResolver.ResolvedSort resolvedSort,
                                                           int page, int perPage) {
        return paginationHelper
                .filter(query)
                .sort(resolvedSort.sort())
                .pipeline(resolvedSort.preSortStages())
                .postSortPipeline(resolvedSort.postSortStages())
                .perPage(perPage)
                .page(page);
    }

    private String buildJwt(CertificateEntry signingCert, String jti,
                            Instant issuedAt, @Nullable Instant expiresAt) {
        try {
            final String privateKeyPem = certificateService.encryptedValueService()
                    .decrypt(signingCert.privateKey());
            final PrivateKey privateKey = PemUtils.parsePrivateKey(privateKeyPem);

            final String clusterId = clusterIdService.getString();
            final String audience = clusterId + AUDIENCE_SUFFIX;

            var builder = Jwts.builder()
                    .header()
                    .add("ctt", "enrollment")
                    .add("kid", signingCert.fingerprint())
                    .and()
                    .id(jti)
                    .issuer(clusterId)
                    .audience().add(audience).and()
                    .issuedAt(Date.from(issuedAt));

            if (expiresAt != null) {
                builder.expiration(Date.from(expiresAt));
            }

            return builder.signWith(privateKey).compact();
        } catch (Exception e) {
            throw new RuntimeException("Failed to build enrollment JWT", e);
        }
    }

}
