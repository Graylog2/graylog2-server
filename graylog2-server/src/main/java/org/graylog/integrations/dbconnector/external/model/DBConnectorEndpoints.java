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
package org.graylog.integrations.dbconnector.external.model;

public enum DBConnectorEndpoints {
    ORACLE("Oracle", "Oracle"),
    MYSQL("MySQL", "MySQL"),
    MICROSOFT_SQL("Microsoft SQL", "Microsoft SQL"),
    POSTGRESQL("PostgreSQL", "PostgreSQL"),
    DB2("DB2", "DB2"),
    MONGODB("MongoDB", "MongoDB");

    private String displayName;
    private String url;

    private DBConnectorEndpoints(String displayName, String url) {
        this.displayName = displayName;
        this.url = url;
    }

    public static DBConnectorEndpoints getEnum(String value) {
        for (DBConnectorEndpoints v : values()) {
            if (v.name().equalsIgnoreCase(value)
                    || v.displayName().equalsIgnoreCase(value)
                    || v.url().equalsIgnoreCase(value)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unable to find enum value for " + value);
    }

    public String displayName() {
        return displayName;
    }

    public String url() {
        return url;
    }

}
