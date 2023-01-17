package org.graylog.datanode.rest;

import org.graylog.datanode.management.ManagedNodes;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class StatusResource {

    private String dataNodeVersion;

    private ManagedNodes openSearch;

    @Inject
    public StatusResource(String dataNodeVersion, ManagedNodes openSearch) {
        this.dataNodeVersion = dataNodeVersion;
        this.openSearch = openSearch;
    }

    @GET
    @Path("status")
    public List<StatusResponse> index() {
        return openSearch.getProcesses()
                .stream()
                .map(process -> new StatusResponse(dataNodeVersion, process.getOpensearchVersion(), process.getProcessInfo()))
                .toList();
    }
}
