package org.graylog2.rest.resources.system;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.rest.models.system.responses.TimezonesList;
import org.graylog2.rest.resources.RestResource;
import org.joda.time.DateTimeZone;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Set;

@Api(value = "System/Timezones", description = "Available Timezones")
@Path("/system/timezones")
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class TimezonesResource extends RestResource {
    private final Multimap<String, String> groupedZones;

    public TimezonesResource() {
        groupedZones = TreeMultimap.create();

        final Set<String> zones = DateTimeZone.getAvailableIDs();
        for (String zone : zones) {
            // skip "simple" names, we only want descriptive names
            if (!zone.contains("/")) {
                continue;
            }
            final String[] groupAndZone = zone.split("/", 2);
            groupedZones.put(groupAndZone[0], groupAndZone[1]);
        }
    }

    @GET
    @Timed
    @ApiOperation(value = "Available Timezones.",
            notes = "This resource returns available timezones.")
    public TimezonesList timezones() {
        return TimezonesList.create(groupedZones);
    }
}
