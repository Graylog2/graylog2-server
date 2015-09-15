package org.graylog2.users;

import org.apache.shiro.crypto.hash.SimpleHash;
import org.graylog2.Configuration;
import org.graylog2.plugin.security.PasswordAlgorithm;

import javax.inject.Inject;
import java.util.regex.Pattern;

public class SimpleHashPasswordAlgorithm implements PasswordAlgorithm {
    private static final String HASH_ALGORITHM = "SHA-1";
    private static final Pattern prefixPattern = Pattern.compile("^\\{[\\w\\d]+\\}");
    private final Configuration configuration;

    @Inject
    public SimpleHashPasswordAlgorithm(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public boolean supports(String hashedPassword) {
        return !prefixPattern.matcher(hashedPassword).matches();
    }

    private String hash(String password, String salt) {
        return new SimpleHash(HASH_ALGORITHM, password, salt).toString();
    }

    @Override
    public String hash(String password) {
        return hash(password, configuration.getPasswordSecret());
    }

    @Override
    public boolean matches(String hashedPasswordAndSalt, String otherPassword) {
        return hash(otherPassword).equals(hashedPasswordAndSalt);
    }
}
