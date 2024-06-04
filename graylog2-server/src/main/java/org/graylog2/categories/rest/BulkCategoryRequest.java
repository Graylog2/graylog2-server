package org.graylog2.categories.rest;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record BulkCategoryRequest(@JsonProperty("asset_ids") List<String> assetIds, @JsonProperty("categories") List<String> categories) {
}
