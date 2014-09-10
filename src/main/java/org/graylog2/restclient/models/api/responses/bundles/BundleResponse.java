package org.graylog2.restclient.models.api.responses.bundles;

import org.graylog2.restclient.models.Input;
import org.graylog2.restclient.models.Output;
import org.graylog2.restclient.models.Stream;
import org.graylog2.restclient.models.dashboards.Dashboard;

import java.util.List;

public class BundleResponse {

    public String id;

    public String name;

    public String description;

    public String category;

    public List<Input> inputs;

    public List<Stream> streams;

    public List<Output> outputs;

    public List<Dashboard> dashboards;
}
