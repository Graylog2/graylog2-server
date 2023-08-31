package org.graylog2.security;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Date;
import java.util.Map;

public class OpenSearchJWTTokenUtil {
    public static String createToken(byte[] apiKeySecretBytes) {
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;

        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);

        Key signingKey = new SecretKeySpec(apiKeySecretBytes, signatureAlgorithm.getJcaName());

        JwtBuilder builder = Jwts.builder().setId("graylog datanode connect")
                .addClaims(Map.of("os_roles", "admin"))
                .setIssuedAt(now)
                .setSubject("admin")
                .setIssuer("graylog")
                .setNotBefore(now)
                // TODO: expiration to a smaller time, automatic refresh
                .setExpiration(new Date(nowMillis + 24*60*60*1000))
                .signWith(signatureAlgorithm, signingKey);

        return builder.compact();
    }
}
