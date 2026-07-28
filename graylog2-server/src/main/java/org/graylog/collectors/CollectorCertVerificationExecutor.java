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
 * Qualifies the {@code Executor} used to run collector mTLS certificate verification off the Netty
 * event loop: it is the {@code SslHandler} delegated-task executor for the ingest TLS handshake, so
 * the trust manager's {@code checkClientTrusted} (and its instance-binding resolution, which may
 * block on MongoDB on a cold cache miss) never runs on an event-loop thread.
 * <p>
 * The {@link CertBindingResolver}'s background cache work deliberately does <em>not</em> share this
 * pool — see {@link CollectorCertCacheRefreshExecutor}. The bound pool and its overload (shedding)
 * behavior are defined where the executor is provided in {@code CollectorsModule}.
 */
@Target({ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@BindingAnnotation
public @interface CollectorCertVerificationExecutor {}
