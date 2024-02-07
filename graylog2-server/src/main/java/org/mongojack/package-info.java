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
/**
 * This package contains a compatibility layer to support old code using the Mongojack 2.x API. It is destined for
 * removal as soon as all code has been migrated to use the MongoDB driver API directly.
 * <p>
 * Instead of the classes from this package, use {@link org.graylog2.database.MongoCollections} as an entrypoint for
 * interacting with MongoDB.
 */
package org.mongojack;
