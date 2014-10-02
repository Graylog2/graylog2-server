package org.graylog2.restclient.models.api.requests;

import org.graylog2.restclient.models.bundles.Dashboard;
import org.graylog2.restclient.models.bundles.Input;
import org.graylog2.restclient.models.bundles.Output;
import org.graylog2.restclient.models.bundles.Stream;

import java.util.List;

public class CreateBundleRequest extends ApiRequest {
    public String id;
    public String name;
    public String description;
    public String category;
    public List<Input> inputs;
    public List<Stream> streams;
    public List<Output> outputs;
    public List<Dashboard> dashboards;
}
