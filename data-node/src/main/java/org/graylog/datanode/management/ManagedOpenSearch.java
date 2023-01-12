package org.graylog.datanode.management;

import org.graylog.datanode.DataNodeRunner;
import org.graylog.datanode.RunningProcess;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Optional;

@Service
public class ManagedOpenSearch {

    @Value("${opensearch.version}")
    private String opensearchVersion;
    @Value("${opensearch.location}")
    private String openseachLocation;

    private RunningProcess dataNode;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        final LinkedHashMap<String, String> config = new LinkedHashMap<>();
        config.put("discovery.type", "single-node");
        config.put("plugins.security.ssl.http.enabled", "false");
        config.put("plugins.security.disabled", "true");

        final DataNodeRunner runner = new DataNodeRunner();
        final RunningProcess dataNode = runner.start(Path.of(openseachLocation), opensearchVersion, config);

        System.out.println("Data node up and running");

        this.dataNode = dataNode;
    }


    public Optional<RunningProcess> getDataNode() {
        return Optional.ofNullable(dataNode);
    }
}
