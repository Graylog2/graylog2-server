package org.graylog2.users;

import com.google.common.base.Splitter;
import org.graylog2.plugin.security.PasswordAlgorithm;
import org.mindrot.jbcrypt.BCrypt;

import java.util.List;

public class BCryptPasswordAlgorithm implements PasswordAlgorithm {
    private static final String PREFIX = "{bcrypt}";
    private static final String SALTPREFIX = "{salt}";

    @Override
    public boolean supports(String hashedPassword) {
        return hashedPassword.startsWith(PREFIX);
    }

    private String hash(String password, String salt) {
        return PREFIX + BCrypt.hashpw(password, salt) + SALTPREFIX + salt;
    }

    @Override
    public String hash(String password) {
        return hash(password, BCrypt.gensalt(12));
    }

    @Override
    public boolean matches(String hashedPasswordAndSalt, String otherPassword) {
        final Splitter splitter = Splitter.on(SALTPREFIX);
        final List<String> splitted = splitter.splitToList(hashedPasswordAndSalt);
        final String salt = splitted.get(1);

        return hash(otherPassword, salt).equals(hashedPasswordAndSalt);
    }
}
