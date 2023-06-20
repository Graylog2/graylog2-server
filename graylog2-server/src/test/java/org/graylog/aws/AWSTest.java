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
package org.graylog.aws;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertTrue;


public class AWSTest {

    // Verify that region choices are correctly built and formatted.
    @Test
    public void testRegionChoices() {

        Map<String, String> regionChoices = AWS.buildRegionChoices();

        // Check format of random region.
        assertTrue(regionChoices.containsValue("EU (London): eu-west-2"));
        assertTrue(regionChoices.containsKey("eu-west-2"));

        // Verify that the Gov regions are present.
        //"us-gov-west-1" -> "AWS GovCloud (US): us-gov-west-1"
        assertTrue(regionChoices.containsValue("AWS GovCloud (US): us-gov-west-1"));
        assertTrue(regionChoices.containsKey("us-gov-west-1"));
        assertTrue(regionChoices.containsValue("AWS GovCloud (US-East): us-gov-east-1"));
        assertTrue(regionChoices.containsKey("us-gov-east-1"));
    }
}