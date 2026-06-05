package org.graylog.collectors;

import jakarta.inject.Inject;
import org.graylog.collectors.opamp.auth.EnrollmentTokenService;

public class CollectorsInitializer {
    private final CollectorCaService caService;
    private final CollectorLogsDestinationService logsDestinationService;
    private final EnrollmentTokenService enrollmentTokenService;
    private final CollectorsConfigService configService;

    @Inject
    public CollectorsInitializer(CollectorCaService caService,
                                 CollectorLogsDestinationService logsDestinationService,
                                 EnrollmentTokenService enrollmentTokenService,
                                 CollectorsConfigService configService) {
        this.caService = caService;
        this.logsDestinationService = logsDestinationService;
        this.enrollmentTokenService = enrollmentTokenService;
        this.configService = configService;
    }

    public CollectorsConfig initialize(CollectorsConfig config) {
        final var caHierarchy = caService.initializeCa();
        final var tokenSigningKey = createTokenSigningKey();

        logsDestinationService.ensureExists();

        return config.toBuilder()
                .caCertId(caHierarchy.caCert().id())
                .signingCertId(caHierarchy.signingCert().id())
                .tokenSigningKey(tokenSigningKey)
                .otlpServerCertId(caHierarchy.otlpServerCert().id())
                .build();
    }

    private TokenSigningKey createTokenSigningKey() {
        try {
            return enrollmentTokenService.createTokenSigningKey();
        } catch (Exception e) {
            throw new IllegalStateException("Could not create token signing key", e);
        }
    }
}
