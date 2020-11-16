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
package org.graylog.schema;

public class UserFields {
    public static final String USER_COMMAND = "user_command";
    public static final String USER_COMMAND_PATH = "user_command_path";
    public static final String USER_DOMAIN = "user_domain";
    public static final String USER_EMAIL = "user_email";
    public static final String USER_ID = "user_id";
    public static final String USER_NAME = "user_name";

    // Derived and Enriched Fields
    public static final String USER_CATEGORY = "user_category";
    public static final String USER_NAME_MAPPED = "user_name_mapped";
    public static final String USER_PRIORITY = "user_priority";
    public static final String USER_PRIORITY_LEVEL = "user_priority_level";
    public static final String USER_TYPE = "user_type";

    // Target User Fields
    private static final String TARGET_PREFIX = "target_";

    public static final String TARGET_USER = "target_user";
    public static final String TARGET_USER_EMAIL = TARGET_PREFIX + USER_EMAIL;
    public static final String TARGET_USER_ID = TARGET_PREFIX + USER_ID;
    public static final String TARGET_USER_NAME = TARGET_PREFIX + USER_NAME;
}
