package models.sockjs;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.graylog2.restclient.lib.metrics.Metric;

import java.util.Collection;

@JsonAutoDetect
public class MetricValuesUpdate {

    public String nodeId;

    public Collection<NamedMetric> values;

    public static class NamedMetric {
        public String name;
        public Metric metric;

        public NamedMetric(String name, Metric value) {
            this.name = name;
            metric = value;
        }
    }
}
