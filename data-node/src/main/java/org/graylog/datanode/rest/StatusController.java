package org.graylog.datanode.rest;

import org.graylog.datanode.RunningProcess;
import org.graylog.datanode.management.ManagedOpenSearch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatusController {

    @Autowired
    private ManagedOpenSearch openSearch;

    @GetMapping("/")
    public StatusResponse index() {
        return openSearch.getDataNode()
                .map(RunningProcess::getProcess)
                .map(process -> new StatusResponse(process.pid(), process.isAlive()))
                .orElse(new StatusResponse(-1, false));
    }
}
