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
package org.graylog2.inputs.categories;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InputCategoryServiceTest {

    @Test
    void testCSVReader() {
        InputCategoryService inputCategoryService = new InputCategoryServiceImpl();
        final List<String> categories = inputCategoryService.allCategories();
        assertThat(categories).hasSize(2);

        final List<String> subcategories = inputCategoryService.subCategoryByCategory("Web Server");
        assertThat(subcategories).hasSize(1);
    }
}
