package org.graylog2.plugin.security;

public interface PasswordAlgorithm {
    boolean supports(String hashedPassword);
    String hash(String password);
    boolean matches(String hashedPassword, String otherPassword);
}
