/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.shared.security.tls;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SelfSignedCertificateTest {

    @Test
    public void testCertificate() throws Exception {
        final SelfSignedCertificate ssc = SelfSignedCertificate.create();
        assertThat(ssc.certificate()).isNotNull();
    }

    @Test
    public void testPrivateKey() throws Exception {
        final SelfSignedCertificate ssc = SelfSignedCertificate.create();
        assertThat(ssc.privateKey()).isNotNull();
    }

    @Test
    public void testKeyStore() throws Exception {
        final SelfSignedCertificate ssc = SelfSignedCertificate.create();
        assertThat(ssc.keyStore()).isNotNull();
    }

    @Test
    public void testCreate() throws Exception {
        final SelfSignedCertificate ssc = SelfSignedCertificate.create();
        assertThat(ssc.keyStore().getCertificate("example.com")).isEqualTo(ssc.certificate());
    }

    @Test
    public void testCreateWithCustomFQDN() throws Exception {
        final SelfSignedCertificate ssc = SelfSignedCertificate.create("graylog.example.com", "password");
        assertThat(ssc.keyStore().getCertificate("graylog.example.com")).isEqualTo(ssc.certificate());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateWithMissingFQDN() throws Exception {
        SelfSignedCertificate.create(null, "password");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateWithEmptyFQDN() throws Exception {
        SelfSignedCertificate.create("", "password");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateWithMissingPassword() throws Exception {
        SelfSignedCertificate.create("example.com", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateWithEmptyPassword() throws Exception {
        SelfSignedCertificate.create("example.com", "");
    }

    @Test
    public void testCreateWithCustomFQDNAndKeyLength() throws Exception {
        final SelfSignedCertificate ssc =
                SelfSignedCertificate.create("graylog.example.com", ThreadLocalInsecureRandom.current(), 2048, "password");
        assertThat(ssc.keyStore().getCertificate("graylog.example.com")).isEqualTo(ssc.certificate());
    }
}