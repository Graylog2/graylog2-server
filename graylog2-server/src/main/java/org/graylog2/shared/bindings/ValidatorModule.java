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
package org.graylog2.shared.bindings;

import com.google.inject.AbstractModule;

import javax.validation.Validation;
import javax.validation.Validator;

public class ValidatorModule extends AbstractModule {
    @Override
    protected void configure() {
        // Validator instances are thread-safe and can be reused.
        // See: http://hibernate.org/validator/documentation/getting-started/
        //
        // The Validator instance creation is quite expensive.
        // Making this a Singleton reduced the CPU load by 50% and reduced the GC load from 5 GCs per second to 2 GCs
        // per second when running a load test of the collector registration endpoint.
        bind(Validator.class).toInstance(Validation.buildDefaultValidatorFactory().getValidator());
    }
}
