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
package org.graylog.security.certutil;

import java.security.cert.X509Certificate;

public class CertRenewalCalculator {
    public boolean needsRenewal(X509Certificate cert) {
        // policy lesen
        // Threshold aus der config, default 10%, abrunden auf ganze Tage
        // wenn <= 1, dann auf jeden Fall renewal

        // lesen aller Zertifikate
        // prüfen, ob jedes Zertifkat zum Zeitpunkt now() + x tage immer noch gültig ist
        // wenn nicht, dann renewal

        // ganze auch für CA certs machen



        cert.checkValidity();
    }
}
