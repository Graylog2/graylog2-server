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
package org.graylog2.configuration;

import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.ValidatorMethod;
import com.github.joschi.jadconfig.validators.PositiveIntegerValidator;
import com.github.joschi.jadconfig.validators.StringNotBlankValidator;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;

public class MongoDbConfiguration {
    @Parameter(value = "mongodb_max_connections", validator = PositiveIntegerValidator.class)
    private int maxConnections = 1000;

    @Parameter(value = "mongodb_uri", required = true, validator = StringNotBlankValidator.class)
    private String uri = "mongodb://localhost/graylog";

    @Parameter(value = "mongodb_version_probe_attempts", validators = {PositiveIntegerValidator.class})
    int mongodbVersionProbeAttempts = 0;

    public int getMaxConnections() {
        return maxConnections;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public MongoClientURI getMongoClientURI() {
        final MongoClientOptions.Builder mongoClientOptionsBuilder = MongoClientOptions.builder()
                .connectionsPerHost(getMaxConnections());

        return new MongoClientURI(uri, mongoClientOptionsBuilder);
    }

    @ValidatorMethod
    public void validate() throws ValidationException {
        if(getMongoClientURI() == null) {
            throw new ValidationException("mongodb_uri is not a valid MongoDB connection string");
        }
    }
}
