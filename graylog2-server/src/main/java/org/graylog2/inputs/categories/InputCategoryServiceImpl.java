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

import au.com.bytecode.opencsv.CSVParser;
import au.com.bytecode.opencsv.CSVReader;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InputCategoryServiceImpl implements InputCategoryService {
    public record InputCategorization(String category, String subCategory, String type) {}

    private Set<InputCategorization> categorizationSet;

    public List<String> allCategories() {
        ensureCategorizationSet();
        return categorizationSet.stream()
                .map(inputCategorization -> inputCategorization.category)
                .distinct()
                .toList();
    }

    public List<String> subCategoryByCategory(String inputCategory) {
        ensureCategorizationSet();
        return categorizationSet.stream()
                .filter(c -> c.category.equals(inputCategory))
                .map(c -> c.subCategory)
                .distinct()
                .toList();
    }

    private void ensureCategorizationSet() {
        try {
            categorizationSet = getFromCSV();
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException("Unable to get input categories data", e);
        }
    }

    private Set<InputCategorization> getFromCSV() throws URISyntaxException, IOException {
        Set<InputCategorization> result = new HashSet<>();
        Path filePath = Paths.get(getClass().getResource("input_categories.csv").toURI());

        try (CSVReader csvReader = new CSVReader(Files.newBufferedReader(filePath), CSVParser.DEFAULT_SEPARATOR, CSVParser.DEFAULT_QUOTE_CHARACTER, 1);) {
            csvReader.readAll().forEach(
                    array -> result.add(new InputCategorization(array[0], array[1], array[2]))
            );
        }

        return result;
    }
}
