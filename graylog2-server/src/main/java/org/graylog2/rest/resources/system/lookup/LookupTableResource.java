package org.graylog2.rest.resources.system.lookup;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.lookup.MongoDbLookupTableService;
import org.graylog2.plugin.lookup.LookupCache;
import org.graylog2.plugin.lookup.LookupDataAdapter;
import org.graylog2.rest.models.system.lookup.LookupTable;
import org.graylog2.shared.rest.resources.RestResource;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@RequiresAuthentication
@Path("/system/lookup")
@Produces("application/json")
@Consumes("application/json")
@Api(value = "System/Lookup", description = "Lookup tables")
public class LookupTableResource extends RestResource {

    private MongoDbLookupTableService lookupTableService;

    @Inject
    public LookupTableResource(MongoDbLookupTableService lookupTableService) {
        this.lookupTableService = lookupTableService;
    }

    @GET
    @Path("tables")
    @ApiOperation(value = "List all configured lookup tables")
    public List<LookupTable> listAll() {
        return lookupTableService.loadAllTables().stream()
                .map(LookupTable::fromDomainObject)
                .collect(Collectors.toList());
    }

    @POST
    @Path("tables")
    @ApiOperation(value = "Create a new lookup table")
    public LookupTable create(@ApiParam LookupTable lookupTable) {
        org.graylog2.lookup.LookupTable.Builder tableBuilder = lookupTable.toDomainObjectBuilder();

        // TODO resolve cache provider by name
        LookupCache cacheProvider = lookupTableService.findCacheProvider(lookupTable.cacheProviderName());

        // TODO resolve data provider by name
        LookupDataAdapter dataProvider = lookupTableService.findDataProvider(lookupTable.dataProviderName());

        org.graylog2.lookup.LookupTable table = tableBuilder
                .cacheProvider(cacheProvider)
                .dataProvider(dataProvider)
                .build();
        org.graylog2.lookup.LookupTable saved = lookupTableService.save(table);

        return LookupTable.fromDomainObject(saved);
    }
}
