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
package org.graylog2.datanode.repositories;

import jakarta.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Locale;
import java.util.regex.Pattern;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Names of the OpenSearch repository clients that Graylog manages, e.g. {@code repository_archive-eu_7f3a}. Data
 * Nodes configure such a client from the credentials of its repository, which the enterprise plugin stores together
 * with the client name.
 * <p>
 * A name is generated once, when a repository with its own client is created, and never derived again. The slug
 * tells which repository the client was created for. The random suffix makes a repository re-created under the same
 * name get a new client, which doesn't exist before the Data Nodes restart, instead of the old client with the old
 * credentials. Client names are part of OpenSearch setting and keystore keys, so they only contain lowercase
 * letters, digits, {@code -} and {@code _}.
 */
public final class RepositoryClientNames {

    private static final String PREFIX = "repository_";
    private static final int MAX_SLUG_LENGTH = 32;
    private static final int SUFFIX_BYTES = 2;
    // Repository names consisting only of characters that the slug drops still get a readable client name
    private static final String EMPTY_SLUG = "unnamed";

    private static final Pattern NOT_IN_SLUG = Pattern.compile("[^a-z0-9]+");
    private static final Pattern MANAGED = Pattern.compile(f("%s[a-z0-9-]+_[0-9a-f]{%d}", PREFIX, SUFFIX_BYTES * 2));

    private static final SecureRandom RANDOM = new SecureRandom();

    private RepositoryClientNames() {
    }

    /**
     * A new client name for the given repository.
     */
    public static String generate(String repositoryName) {
        final byte[] suffix = new byte[SUFFIX_BYTES];
        RANDOM.nextBytes(suffix);
        return f("%s%s_%s", PREFIX, slug(repositoryName), HexFormat.of().formatHex(suffix));
    }

    /**
     * Whether Graylog generated the given client name. Other clients, the {@code default} one or clients configured
     * manually, come from the configuration files.
     */
    public static boolean isManaged(@Nullable String client) {
        return client != null && MANAGED.matcher(client).matches();
    }

    /**
     * A readable form of the repository name that only contains lowercase letters, digits and {@code -}, at most 32
     * characters long. Also usable where AWS restricts names, e.g. role session names.
     */
    public static String slug(String repositoryName) {
        final String slug = NOT_IN_SLUG.matcher(repositoryName.toLowerCase(Locale.ROOT)).replaceAll("-");
        final String trimmed = StringUtils.strip(StringUtils.left(StringUtils.strip(slug, "-"), MAX_SLUG_LENGTH), "-");
        return trimmed.isEmpty() ? EMPTY_SLUG : trimmed;
    }
}
