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
package org.graylog.collectors;

import com.google.inject.BindingAnnotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Qualifies the {@code Executor} that runs the {@link CertBindingResolver}'s background cache work:
 * event-driven and {@code refreshAfterWrite} reloads, and the startup prewarm.
 * <p>
 * Deliberately distinct from {@link CollectorCertVerificationExecutor}: these tasks block on MongoDB
 * with nobody waiting on the result, so they must not compete with the latency-critical TLS handshake
 * work. The bound pool and its queueing behavior are defined where the executor is provided in
 * {@code CollectorsModule}.
 */
@Target({ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@BindingAnnotation
public @interface CollectorCertCacheRefreshExecutor {}
