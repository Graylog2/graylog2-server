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
