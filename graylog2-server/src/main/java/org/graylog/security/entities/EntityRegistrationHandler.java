package org.graylog.security.entities;

import org.graylog.grn.GRN;
import org.graylog2.plugin.database.users.User;

public interface EntityRegistrationHandler {
    void handleRegistration(GRN entityGRN, User user);

    void handleUnregistration(GRN entityGRN);
}
