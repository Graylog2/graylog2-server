package org.graylog2.rest.resources.system;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.configuration.validators.SearchVersionRange;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.utilities.StringUtils;
import org.graylog2.storage.SearchVersion;
import org.graylog2.storage.providers.ElasticsearchVersionProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Locale;

@Api(value = "System/SearchVersion", description = "Checks system search version")
@Path("/system/searchVersion")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class SearchVersionResource extends RestResource implements PluginRestResource {

    private static final Logger LOG = LoggerFactory.getLogger(SearchVersionResource.class);
    final private ElasticsearchVersionProvider versionProvider;

    @Inject
    public SearchVersionResource(ElasticsearchVersionProvider versionProvider) {
        this.versionProvider = versionProvider;
    }

    @PUT
    @Path("/satisfiesVersion")
    @ApiOperation(value = "Confirms whether the current search version satisfies a given distribution and Semantic Versioning version")
    public SatisfiesVersionResponse satisfiesVersion(@ApiParam(name = "requiredVersion", required = true) SatisfiesVersionRequest request) {

        final SearchVersion currentVersion = versionProvider.get();
        final String requiredDistributionStr = request.distribution();
        final String requiredVersionExpression = request.expression();

        final SearchVersionRange requiredVersion;
        final SearchVersion.Distribution requiredDistribution;
        try {
            requiredDistribution = SearchVersion.Distribution.valueOf(requiredDistributionStr.toUpperCase(Locale.ENGLISH));
            requiredVersion = SearchVersionRange.of(requiredDistribution, requiredVersionExpression);
        } catch (Exception e) {
            LOG.error("Unable to create a search version range for distribution {} and SemVer expression {}",
                    requiredDistributionStr, requiredVersionExpression);
            throw new InternalServerErrorException(
                    StringUtils.f("Unable to create a search version range for distribution %s and SemVer expression %s",
                            requiredDistributionStr, requiredVersionExpression));
        }

        boolean satisfied = currentVersion.satisfies(requiredVersion);
        LOG.debug("Checking current version {} satisfies required version {} {}",
                currentVersion, requiredDistribution, requiredVersionExpression);
        String errorMessage = "";
        if (!satisfied) {
            errorMessage = StringUtils.f("Current search version %s does not satisfy required version %s %s",
                    currentVersion, requiredDistribution, requiredVersionExpression);
        }
        return SatisfiesVersionResponse.Builder.create().satisfied(satisfied).errorMessage(errorMessage).build();
    }
}

@AutoValue
@JsonAutoDetect
@JsonDeserialize(builder = SatisfiesVersionResponse.Builder.class)
abstract class SatisfiesVersionResponse {

    @JsonProperty("satisfied")
    public abstract boolean satisfied();

    @JsonProperty("errorMessage")
    public abstract String errorMessage();

    @AutoValue.Builder
    public abstract static class Builder {

        @JsonProperty("supported")
        public abstract SatisfiesVersionResponse.Builder satisfied(boolean satisfied);

        @JsonProperty("errorMessage")
        public abstract SatisfiesVersionResponse.Builder errorMessage(String errorMessage);

        public abstract SatisfiesVersionResponse build();

        @JsonCreator
        public static SatisfiesVersionResponse.Builder create() {
            return new AutoValue_SatisfiesVersionResponse.Builder();
        }
    }
}

@AutoValue
@JsonAutoDetect
@JsonDeserialize(builder = SatisfiesVersionRequest.Builder.class)
abstract class SatisfiesVersionRequest {

    @JsonProperty("distribution")
    public abstract String distribution();

    @JsonProperty("expression")
    public abstract String expression();

    @AutoValue.Builder
    public abstract static class Builder {

        @JsonProperty("distribution")
        public abstract SatisfiesVersionRequest.Builder distribution(String distribution);

        @JsonProperty("expression")
        public abstract SatisfiesVersionRequest.Builder expression(String expression);

        public abstract SatisfiesVersionRequest build();

        @JsonCreator
        public static SatisfiesVersionRequest.Builder create() {
            return new AutoValue_SatisfiesVersionRequest.Builder();
        }
    }
}

