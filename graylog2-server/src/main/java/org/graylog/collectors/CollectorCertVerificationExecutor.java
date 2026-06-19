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
 * Qualifies the {@code ExecutorService} used to run collector mTLS certificate verification off
 * the Netty event loop.
 * <p>
 * It backs two uses, both of which would otherwise block an event-loop thread on MongoDB:
 * <ul>
 *   <li>the {@code SslHandler} delegated-task executor for the ingest TLS handshake, so the trust
 *       manager's {@code checkClientTrusted} (and its instance-binding lookup) runs off the event
 *       loop;</li>
 *   <li>the background refresh executor of the collector fingerprint cache.</li>
 * </ul>
 * The bound pool and its overload (shedding) behavior are defined where the executor is provided
 * in {@code CollectorsModule}.
 */
@Target({ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@BindingAnnotation
public @interface CollectorCertVerificationExecutor {}
