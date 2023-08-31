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
 * This package contains the Integrations plugin, formerly located at https://github.com/Graylog2/graylog-plugin-integrations.
 * <p>
 * The plugin intentionally resides in the same legacy package that was used when it was in the separate repository and
 * <em>not</em> underneath the {@code org.graylog.plugin} package. Changing the package would break API routes which are,
 * based on the old package name.
 */
package org.graylog.integrations;
