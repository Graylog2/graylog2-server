package org.graylog2.bootstrap.preflight.web.resources;

import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.bootstrap.preflight.PreflightConstants;
import org.graylog2.plugin.certificates.RenewalPolicy;
import org.graylog2.plugin.cluster.ClusterConfigService;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path(PreflightConstants.API_PREFIX + "renewal_policy")
@Produces(MediaType.APPLICATION_JSON)
public class CertificateRenewalPolicyResource {
    private final ClusterConfigService clusterConfigService;

    @Inject
    public CertificateRenewalPolicyResource(ClusterConfigService clusterConfigService) {
        this.clusterConfigService = clusterConfigService;
    }

    @GET
    public RenewalPolicy get() {
        return this.clusterConfigService.get(RenewalPolicy.class);
    }

    @POST
    @Path("/create")
    @NoAuditEvent("No Audit Event needed")
    public void set(@NotNull RenewalPolicy renewalPolicy) {
        this.clusterConfigService.write(renewalPolicy);
    }
}
