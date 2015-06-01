package models.sockjs;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.google.common.collect.Sets;
import org.graylog2.restclient.lib.metrics.Metric;

import java.util.Set;

@JsonAutoDetect
public class MetricValuesUpdate {

    public final String nodeId;

    public final Set<NamedMetric> values;

    public MetricValuesUpdate(String nodeId) {
        this.nodeId = nodeId;
        this.values = Sets.newHashSet();
    }

    public static class NamedMetric {
        public String name;
        public Metric metric;

        public NamedMetric(String name, Metric value) {
            this.name = name;
            metric = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            NamedMetric that = (NamedMetric) o;

            return name.equals(that.name);
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }
    }
}
