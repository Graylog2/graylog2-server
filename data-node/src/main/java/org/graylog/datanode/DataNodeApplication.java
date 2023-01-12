package org.graylog.datanode;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.file.Path;
import java.util.LinkedHashMap;

@SpringBootApplication
public class DataNodeApplication implements CommandLineRunner {


    @Value("${opensearch.location}")
    private String openseachLocation;

    public static void main(String[] args) {
        SpringApplication.run(DataNodeApplication.class, args);
    }


    @Override
    public void run(String... args) throws Exception {
        final LinkedHashMap<String, String> config = new LinkedHashMap<>();
        config.put("discovery.type", "single-node");
        config.put("plugins.security.ssl.http.enabled", "false");
        config.put("plugins.security.disabled", "true");

        final DataNodeRunner runner = new DataNodeRunner();
        final RunningProcess dataNode = runner.start(getOpensearchLocation(), config);

        System.out.println("Data node up and running");

        try {
            dataNode.getProcess().waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private Path getOpensearchLocation() {
        return Path.of(openseachLocation);
    }
}
