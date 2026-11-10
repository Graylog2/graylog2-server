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
/*
 * Copyright © 2017 Jeremy Custenborder (jcustenborder@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.graylog.plugins.cef.parser;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.Map;

/**
 * The result of parsing a single CEF event.
 *
 * <p>{@code timestamp} and {@code host} come from the syslog-style prefix in front of {@code CEF:} and are both
 * {@code null} for a bare CEF string. {@code extensions} is empty, never {@code null}, when the event carries no
 * extension fields.
 */
public record CEFMessage(@Nullable Date timestamp,
                         @Nullable String host,
                         int cefVersion,
                         String deviceVendor,
                         String deviceProduct,
                         String deviceVersion,
                         String deviceEventClassId,
                         String name,
                         String severity,
                         Map<String, String> extensions) {
}
