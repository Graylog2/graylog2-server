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
package org.graylog2.bootstrap.preflight.web.resources;

import org.assertj.core.api.Assertions;
import org.graylog.security.certutil.InMemoryClusterConfigService;
import org.graylog2.plugin.certificates.RenewalPolicy;
import org.junit.jupiter.api.Test;

class CertificateRenewalPolicyResourceTest {

    @Test
    void testCertificateLifetimeValidation() {
        final CertificateRenewalPolicyResource resource = new CertificateRenewalPolicyResource(new InMemoryClusterConfigService());
        Assertions.assertThatThrownBy(() -> resource.set(new RenewalPolicy(RenewalPolicy.Mode.AUTOMATIC, "10nonsense")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid certificate lifetime value: 10nonsense");

        Assertions.assertThatNoException()
                .isThrownBy(() -> resource.set(new RenewalPolicy(RenewalPolicy.Mode.AUTOMATIC, "P30D")));
    }
}
