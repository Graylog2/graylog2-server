package org.graylog2.restclient.models.bundles;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;

import javax.validation.constraints.NotNull;
import java.util.List;

public class ConfigurationBundle {
    @JsonProperty
    private String id;

    @JsonProperty
    @NotNull
    private String name;

    @JsonProperty
    @NotNull
    private String description;

    @JsonProperty
    @NotNull
    private String category;

    @JsonProperty
    private List<Input> inputs = Lists.newArrayList();

    @JsonProperty
    private List<Stream> streams = Lists.newArrayList();

    @JsonProperty
    private List<Output> outputs = Lists.newArrayList();

    @JsonProperty
    private List<Dashboard> dashboards = Lists.newArrayList();

}