package org.graylog.security;

import org.graylog.plugins.views.search.permissions.UserStreams;

public interface HasStreams {
    UserStreams streams();
}
