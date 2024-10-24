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

import jakarta.inject.Inject;

import java.util.List;

public class InputCategoryServiceImpl implements InputCategoryService {
    private final InputCategoryDbService inputCategoryDbService;

    @Inject
    public InputCategoryServiceImpl(InputCategoryDbService inputCategoryDbService) {
        this.inputCategoryDbService = inputCategoryDbService;
    }

    public List<String> allCategories() {
        return inputCategoryDbService.get().stream()
                .map(InputCategoryDto::category)
                .distinct()
                .toList();
    }

    public List<String> subCategoryByCategory(String inputCategory) {
        return inputCategoryDbService.get().stream()
                .filter(dto -> dto.category().equals(inputCategory))
                .map(InputCategoryDto::subcategory)
                .distinct()
                .toList();
    }

}
