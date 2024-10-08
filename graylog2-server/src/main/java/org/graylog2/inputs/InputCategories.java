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
package org.graylog2.inputs;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InputCategories {
    public enum InputCategory {
        WEB_SERVER, DATA_LAKE
    }

    public enum InputSubCategory {
        APACHE_WEB_SERVER, AMAZON_SECURITY_LAKE
    }

    public enum InputType {
        BEATS, SYSLOG, API, S3, NXLOG
    }

    public record InputCategorization(InputCategory category, InputSubCategory subCategory, InputType type) {}

    private static final Set<InputCategorization> categorizationSet;

    static {
        categorizationSet = new HashSet<>();
        categorizationSet.add(new InputCategorization(InputCategory.WEB_SERVER, InputSubCategory.APACHE_WEB_SERVER, InputType.BEATS));
        categorizationSet.add(new InputCategorization(InputCategory.WEB_SERVER, InputSubCategory.APACHE_WEB_SERVER, InputType.SYSLOG));
        categorizationSet.add(new InputCategorization(InputCategory.DATA_LAKE, InputSubCategory.AMAZON_SECURITY_LAKE, InputType.API));
    }

    public static List<InputCategory> allCategories() {
        return Arrays.asList(InputCategory.values());
    }

    public static List<InputSubCategory> subCategoryByCategory(InputCategory inputCategory) {
        return categorizationSet.stream()
                .filter(c -> c.category.equals(inputCategory))
                .map(c -> c.subCategory)
                .distinct()
                .toList();
    }
}
