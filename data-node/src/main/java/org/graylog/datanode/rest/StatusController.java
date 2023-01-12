package org.graylog.datanode.rest;

import org.graylog.datanode.RunningProcess;
import org.graylog.datanode.management.ManagedOpenSearch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatusController {

    @Value("${datanode.version}")
    private String dataNodeVersion;

    @Autowired
    private ManagedOpenSearch openSearch;

    @GetMapping("/")
    public StatusResponse index() {
        return openSearch.getDataNode()
                .map(os -> new StatusResponse(dataNodeVersion, os.getOpensearchVersion(), os.getProcess().pid(), os.getProcess().isAlive()))
                .orElse(new StatusResponse(dataNodeVersion, "unknown", -1, false));
    }
}
