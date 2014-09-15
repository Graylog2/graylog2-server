package org.graylog2.restclient.models;

import org.graylog2.restclient.models.dashboards.Dashboard;

import java.util.List;

public class ConfigurationBundle {

    private String id;

    private String name;

    private String description;

    private String category;

    private List<Input> inputs;

    private List<Stream> streams;

    private List<Output> outputs;

    private List<Dashboard> dashboards;

}