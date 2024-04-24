package org.graylog.security;

import org.joda.time.DateTimeZone;

import java.util.Optional;

public interface UserDetails {
    Optional<DateTimeZone> timeZone();

    String username();
}
