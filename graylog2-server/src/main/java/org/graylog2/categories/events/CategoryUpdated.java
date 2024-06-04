package org.graylog2.categories.events;

import org.graylog2.categories.model.Category;

public record CategoryUpdated(Category existing, Category updated) {
}
