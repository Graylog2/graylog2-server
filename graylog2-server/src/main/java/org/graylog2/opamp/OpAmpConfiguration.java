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
package org.graylog2.opamp;

import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.documentation.Documentation;
import com.github.joschi.jadconfig.util.Size;
import com.github.joschi.jadconfig.validators.PositiveSizeValidator;

// TODO: Uncomment when OpAMP configuration should be visible
// @DocumentationSection(heading = "OpAMP", description = "Settings for the OpAMP (Open Agent Management Protocol) endpoint")
public class OpAmpConfiguration {

    @Documentation(value = "Maximum allowed size for OpAMP request bodies. Requests exceeding this limit will be rejected with HTTP 413.", visible = false)
    @Parameter(value = "opamp_max_request_body_size", validator = PositiveSizeValidator.class)
    private Size maxRequestBodySize = Size.megabytes(10);
}
