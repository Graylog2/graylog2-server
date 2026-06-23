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

// Stub for @graylog/server-api used during storybook builds.
// The real module is generated from the OpenAPI spec and requires the Java backend build.
// Each exported namespace (e.g. SystemFields, FavoriteFields) is an object of async functions.
const asyncFn = () => Promise.resolve({});
const namespace = new Proxy({}, { get: () => asyncFn });

// eslint-disable-next-line no-undef
module.exports = new Proxy({}, { get: () => namespace });
