package test.restresource;

import org.graylog2.plugin.rest.PluginRestResource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
@Path("/foo/bar")
public class TestResource extends PluginRestResource {
    @GET
    @Path("{test}")
    public String get(@PathParam("test") String test) {
        return "Hello World: " + test;
    }
}
