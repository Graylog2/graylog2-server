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
