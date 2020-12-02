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
package org.graylog2.security;

import org.apache.shiro.realm.Realm;

import java.util.Collection;
import java.util.Optional;

/**
 * The generic type is Realm, even though it really only contains AuthenticatingRealms. This is simply to avoid having to
 * cast the generic collection when passing it to the SecurityManager.
 */
public interface OrderedAuthenticatingRealms extends Collection<Realm> {
    Optional<Realm> getRootAccountRealm();
}
