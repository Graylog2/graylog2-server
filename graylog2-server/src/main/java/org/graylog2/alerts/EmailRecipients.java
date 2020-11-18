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
package org.graylog2.alerts;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.graylog2.plugin.database.users.User;
import org.graylog2.shared.users.UserService;

import java.util.List;
import java.util.Set;

import static com.google.common.base.Strings.isNullOrEmpty;

public class EmailRecipients {
    private final UserService userService;

    private final List<String> usernames;
    private final List<String> emails;

    private Set<String> resolvedEmails;

    public interface Factory {
        EmailRecipients create(
                @Assisted("usernames") List<String> usernames,
                @Assisted("emails") List<String> emails);
    }

    @Inject
    public EmailRecipients(UserService userService,
                           @Assisted("usernames") List<String> usernames,
                           @Assisted("emails") List<String> emails) {
        this.userService = userService;
        this.usernames = usernames;
        this.emails = emails;
    }

    public Set<String> getEmailRecipients() {
        if (resolvedEmails != null) {
            return resolvedEmails;
        }

        final ImmutableSet.Builder<String> emails = ImmutableSet.builder();
        emails.addAll(this.emails);

        for (String username : usernames) {
            final User user = userService.load(username);

            if (user != null && !isNullOrEmpty(user.getEmail())) {
                // LDAP users might have multiple email addresses defined.
                // See: https://github.com/Graylog2/graylog2-server/issues/1439
                final Iterable<String> addresses = Splitter.on(",").omitEmptyStrings().trimResults().split(user.getEmail());
                emails.addAll(addresses);
            }
        }

        resolvedEmails = emails.build();

        return resolvedEmails;
    }

    public boolean isEmpty() {
        return usernames.isEmpty() && emails.isEmpty();
    }
}
