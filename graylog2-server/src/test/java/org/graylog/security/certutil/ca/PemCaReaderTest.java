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
package org.graylog.security.certutil.ca;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.graylog.security.certutil.ca.exceptions.CACreationException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.security.Security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PemCaReaderTest {
    private static final PemCaReader pemCaReader = new PemCaReader();
    private static final String PEM_CERT = """
            -----BEGIN CERTIFICATE-----
            MIIDSzCCAjOgAwIBAgIUCI/qZP6vie/Dmfd1Fo8cAnzRDMYwDQYJKoZIhvcNAQEL
            BQAwFjEUMBIGA1UEAwwLRWFzeS1SU0EgQ0EwHhcNMjMxMDA2MDczODA1WhcNMzMx
            MDAzMDczODA1WjAWMRQwEgYDVQQDDAtFYXN5LVJTQSBDQTCCASIwDQYJKoZIhvcN
            AQEBBQADggEPADCCAQoCggEBAKwNJ8b67VV+3Ci0/WLayJUk96nNDEoyRn7q1+N7
            5zR/7l2E560rHwq1hETZJefJ/RQLGFo1tSPZ2PMM6empwaNSKqXca+ilUO9cNZ3U
            Pein0FAxK1Q7JcCc2ihQoDOYWKasaS0SfXDuzitm6+FzMp3da5qG+0hyT6YTGZI7
            rFRHPOba7clOGpv+xGLCgKRECIvnwaigOzYJFdWOI+LQu2r0mplJOMl41CGKvOn8
            vQ522/MUPMnkcKjbIcO39HnjLnc7A8M2HETWy9vfwHBgfB6qGQRIvKSQGO3oZGZV
            vJVN6h5qzhPEB7DlY+wU6meVthGeV46x4TJm24t2wZz7RR8CAwEAAaOBkDCBjTAM
            BgNVHRMEBTADAQH/MB0GA1UdDgQWBBT1RqP+XT5vYnYPd+3lOqxx6y+2ATBRBgNV
            HSMESjBIgBT1RqP+XT5vYnYPd+3lOqxx6y+2AaEapBgwFjEUMBIGA1UEAwwLRWFz
            eS1SU0EgQ0GCFAiP6mT+r4nvw5n3dRaPHAJ80QzGMAsGA1UdDwQEAwIBBjANBgkq
            hkiG9w0BAQsFAAOCAQEAqb/8E0i2vKsmxJ902boeMuCPdMINTRogh9TU1KTG4aIW
            IGWeX3899CLO6Iu56m+WxOrksxUr72CMYWv3e7cUfddYSVLgcoSuQ8ZhssJCigIF
            JY5Kq1awBn54cpEMPW+eSJBbcmAuED8RbsiQh/DjqTmlGjg4hv9NWhVRw8icbA9A
            T11yUrUjKToPUJ197G1tFwnGzZcdq4OW13GqIbZAIEYYFAg+Gll0NDwzWDEeIz6W
            DD5RDezU2jkhY5frt1aqr2FnJz7NA86ZwW/kCRvAdklGgit3LbZIo7DIqGFZ+04p
            c4ASFUjIXSUzLXeB687EP+3dgcSm4rRz/821D7yKOA==
            -----END CERTIFICATE-----
            """;
    private static final String ENCRYPTED_KEY = """
            -----BEGIN ENCRYPTED PRIVATE KEY-----
            MIIFHDBOBgkqhkiG9w0BBQ0wQTApBgkqhkiG9w0BBQwwHAQI1LXJ8/2fCYkCAggA
            MAwGCCqGSIb3DQIJBQAwFAYIKoZIhvcNAwcECPvwHQkdc+d1BIIEyLAwBG8/sU15
            RjKtGgniaVXSTX3GJIzs7knTPd5YI/Pv2UCSr9/2VxrH8MdD7BYt5iYiyhS9UKs2
            XNyAQrGstjk2PwGF6tUsAEf7ATEoTNAk7FEi0sj77kjnV1zV1I68cP22gU3qedb2
            nLGW1S0eK+FqIjtzLc/Me1tHE2RZs8H4R8oYtXprrFEqgXKxpFcBD78PIm/6apd3
            CgIglEExYVxSj68q/mrfK3i/2XLDtiSexbSL+H1+HbVse1Nen+T8m7wvguXte+xs
            5nRnzvG0hI4KRNestNaH13+1rAJrh7G5aR6/Zi2sJgE8S94ui607e41hWGhg5gO/
            mSgxUBRB39dfvXd2ocBsfcjfrBXpAaeILGgPAb16JddL6Cd7TKKSHSHzENcZ7p4/
            VAYU4LLQnftleZkmAnSUZY6arinI03PztasvRCYWKlpOO+b4C0W/KCAiznJD+N9n
            KqHoWsBoBHSzm2yTOWOpE4xGHS+qQMwvMtljdpFS89GQdYOz56CkF/tO2X/9OJqL
            KdQM1pkwNkIMyw2/HGxQZMD2wY8BDwUgqrUayLswlrAVA2PBKPe+QWMyAtqIrw5x
            yauCaxXYDz/iU7v3a4tEpzCJpop8S2CEHygamQc4F7eX4KlbQ58FH9h15G8XTU+L
            AHhesK5QWisKlqbO/sihGDANWf01u2zTRVY2VvQCZ4v7F1b6C4jM8OTIwk2C463l
            +vVRde9fegCdhZMkx+6yVB9KfXNjhhuq+DrQP3zf48UBgPhEXIfj9HnVeCrDi+Jf
            0qQ1WjjPQum1TGC3HLLVh3WdCFPy0H0KravB2YzE9rJj4ReTnTR/CS8QVRSqAZR7
            7gg/GFWdyYaxQ8fE5+MzT7TgoQfnmZcvBS0DEIlr/Sv+TxMaLGL/fdCegWD5WZWs
            ElCmIYHai2ZSJ5AS9GCMBS5rnHkFj5pnZFQ65vpPxCVn2aKLBIQwkMWCy1WvkBjp
            fAcxt+XBZNeu+HHV4g7jeMDhRxQohn2ImD8FANAyV8IcFMBE6KdSAO4kmJAjcdPU
            1wzDW85u6/RqM6VwiU/gNup060ks+Q08O9BUPXtw/dwa5VYGIDTviwvhwKBt05/c
            zVTulrFkjwwwL76cGXHoSPSyNOq0Of19ZHFm5vfXWuvAsxD16vYhtLFx91voPDZS
            spGCUjqIABAMQUisyfFvJ8SghVCzNkzK35+Ywcanl7V1PW7ROnBr8Z8gaMJ20T6Q
            5iiH2QvoAOiLAKAuThtq/mmjSsZ92G6LUZHMQ61GLr6GICwBtwsxVg9aKCbvJPx+
            urJavnVW8leprdPmmQalW/UneCu0Fly/8BpxdHWq5D/9qGWu89fzBC92As1s3JVh
            DE+Tr6Z4SnThSJmv1cbRr6ls4bp3Sghl7t8Kzqkm7PY2t/2NPxA3tixX4TCdXJp0
            Oj0WFz/qMlHhRmbrpe9z0QGGrAhUOpTi3ypMZZUBd7XWFki5vpBLrUxTDwF9jSaX
            jsXc+tCMLaqfD6Je3FlRs2gn7h2eb7B2AgbLIjUVZQcVsmmf1Olh9dneg1QFsUTn
            ZSiucOM2kErHu1KCutMXuQQ9wXJCDVKbdW/HeetNDftYwqZJos786N1hjPxfB9q+
            aHXmMOpZ7lTdUdSoZrfvAQ==
            -----END ENCRYPTED PRIVATE KEY-----
            """;

    private static final String UNENCRYPTED_KEY = """
            -----BEGIN PRIVATE KEY-----
            MIIEvwIBADANBgkqhkiG9w0BAQEFAASCBKkwggSlAgEAAoIBAQCsDSfG+u1Vftwo
            tP1i2siVJPepzQxKMkZ+6tfje+c0f+5dhOetKx8KtYRE2SXnyf0UCxhaNbUj2djz
            DOnpqcGjUiql3GvopVDvXDWd1D3op9BQMStUOyXAnNooUKAzmFimrGktEn1w7s4r
            ZuvhczKd3WuahvtIck+mExmSO6xURzzm2u3JThqb/sRiwoCkRAiL58GooDs2CRXV
            jiPi0Ltq9JqZSTjJeNQhirzp/L0OdtvzFDzJ5HCo2yHDt/R54y53OwPDNhxE1svb
            38BwYHweqhkESLykkBjt6GRmVbyVTeoeas4TxAew5WPsFOpnlbYRnleOseEyZtuL
            dsGc+0UfAgMBAAECggEAQwjbIzF4UDPd1rRx91a0CTw+jLeaHryI8AFTPUx00GU5
            +9EBLVzcOuFvWB3dIXr3MpyCD//WBFJoL4asCsD788TbXgVMO/hRWPlt4IEl3+Sm
            iuAleCuVcX8LluKXEJM+ky4gypFmM56v0IRIym4GXjC7sJkABgGX2+acM3gxlCIK
            pUqRSZVw8TG+/Ig7L5sxZw9Ka2JVjj71xlG/4KJfqC4+/q0R+iF5F89KD569SGkh
            ggmJ+JnYOpWfMiBKbkjftYkaeOGIqo3PM59893zgj6e1svfUWOSpuURbbWuU+mPQ
            vgPes/CaDFo5CdEQ391uEXJ4peCj/syjDyI/Ty08RQKBgQDtZHCzdbTqsBV2Agiy
            Z6fSwOJg/xZQNFQYsnPyQ5Z+eW7MHAAIxsjvmdwDshS6g44hD47JYVzv4F4NaSX/
            ABpYTvgMhSuAsz9N0hXQ/YoUogmXVdo4GW0/pV+q7YHTN4+HZVnej5TKVuNY3Fqs
            LvONTRC/4wpkPbVBB3arh+RDowKBgQC5iZI1l8pjSBXWmoWBLmUhDW3R7ODlaVtm
            PkWHGhMFEegQ9dLabfUAlX54qgPm+wgmt8yIWHHrZtNAjzXAnV4VXrKfE7Fp/GkK
            em4cCGv7QSwR8t8c2JBq/emPcRFdZfoRJ+nkvuWOZkBoq6kSfCnFmvTOO0yEQU73
            thxIspjwVQKBgQC9VBm+RuYfNognMcAV6S2jnEnv6gG1vcZEXC60zMq928NN7hbo
            6QFgdmlOWTzG9BzqqSnL2mbwuRTJxU6UbVSVkYWrFpp3bn3SZvcXUt5JTmIv3DzJ
            +R10YURHYlzkQ6+o4GAobILSTTHMsRFvuZJs40W0hDLJd52TW4x9iUe32QKBgQCh
            /SXFWuCeK/q9Iq47KkmrQPFIHnwAcCsXqnjDyxUeERM/c5EDmosVVnBUY4QCr9vf
            CgwuYqIbt+vrat2wbPUOzV5Am04DzhfbySbHnObCOJWEmjsIEWCNuWCpFzvlArsB
            LYr9Z1o/KLFFcdKsy/EgkPj58jYNJoQOrFYndp8m/QKBgQDm2sIwQOk3PHYLJ6Y8
            zQyU0WKKQqocMr25Kzbei7fhwdA2cq/EnMbNS4loPI4Pp1r6z1nzsU3SUBLm9H8Z
            238WgswXUk+nRDqFBMec4DorrC5F+Wq52QLAUFZ2lI7AYTqX1EZ3mZNkm9SfmwKq
            8ebsZfvpFAA5D1C7pDgORNyZhg==
            -----END PRIVATE KEY-----
            """;

    private static final String PEM_CERT_WITH_ENCRYPTED_KEY = PEM_CERT + "\n" + ENCRYPTED_KEY;
    private static final String PEM_CERT_WITH_UNENCRYPTED_KEY = PEM_CERT + "\n" + UNENCRYPTED_KEY;

    @BeforeAll
    static void beforeAll() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Test
    void readsCombinedPemWithEncryptedPrivateKey() throws Exception {
        final var ca = pemCaReader.readCA(PEM_CERT_WITH_ENCRYPTED_KEY, "foobar");
        assertThat(ca.certificates()).hasSize(1);
        assertThat(ca.privateKey()).isNotNull();
    }

    @Test
    void throwsExceptionIfKeyIsEncryptedAndPasswordIsWrong() throws Exception {
        assertThatThrownBy(() -> pemCaReader.readCA(PEM_CERT_WITH_ENCRYPTED_KEY, "wrong!"))
                .isInstanceOf(CACreationException.class)
                .hasMessage("Error while decrypting private key. Wrong password?");
    }

    @Test
    void throwsExceptionIfCertificatesAreMissing() throws Exception {
        assertThatThrownBy(() -> pemCaReader.readCA(UNENCRYPTED_KEY, null))
                .isInstanceOf(CACreationException.class)
                .hasMessage("No certificate supplied in CA bundle!");
    }

    @Test
    void throwsExceptionIfPrivateKeyIsMissing() throws Exception {
        assertThatThrownBy(() -> pemCaReader.readCA(PEM_CERT, null))
                .isInstanceOf(CACreationException.class)
                .hasMessage("No private key supplied in CA bundle!");
    }

    @Test
    void readsCombinedPemWithUnencryptedPrivateKey() throws Exception {
        final var ca = pemCaReader.readCA(PEM_CERT_WITH_UNENCRYPTED_KEY, null);
        assertThat(ca.certificates()).hasSize(1);
        assertThat(ca.privateKey()).isNotNull();
    }

    @Test
    void throwsExceptionIfKeyIsEncryptedButPasswordIsMissing() {
        assertThatThrownBy(() -> pemCaReader.readCA(PEM_CERT_WITH_ENCRYPTED_KEY, null))
                .isInstanceOf(CACreationException.class)
                .hasMessage("Private key is encrypted, but no password was supplied!");
        assertThatThrownBy(() -> pemCaReader.readCA(PEM_CERT_WITH_ENCRYPTED_KEY, ""))
                .isInstanceOf(CACreationException.class)
                .hasMessage("Private key is encrypted, but no password was supplied!");
    }
}
