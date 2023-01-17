package org.graylog.datanode.process;

import java.util.LinkedHashMap;
import java.util.Map;

public record ProcessConfiguration(int httpPort, int transportPort, Map<String, String> additionalConfiguration) {
    public Map<String, String> mergedConfig() {

        Map<String, String> allConfig = new LinkedHashMap<>();
        allConfig.put("http.port", String.valueOf(httpPort));
        allConfig.put("transport.port", String.valueOf(transportPort));
        allConfig.putAll(additionalConfiguration);
        return allConfig;
    }
}
