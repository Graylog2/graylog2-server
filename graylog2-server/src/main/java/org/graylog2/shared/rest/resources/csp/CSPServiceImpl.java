package org.graylog2.shared.rest.resources.csp;

import org.graylog.security.authservice.DBAuthServiceBackendService;
import org.graylog2.configuration.TelemetryConfiguration;
import org.graylog2.rest.PaginationParameters;

import javax.inject.Inject;
import java.util.stream.Collectors;

public class CSPServiceImpl implements CSPService {
    private final String telemetryApiHost;
    private final DBAuthServiceBackendService dbService;
    private String connectSrcValue;

    @Inject
    protected CSPServiceImpl(TelemetryConfiguration telemetryConfiguration, DBAuthServiceBackendService dbService) {
        this.telemetryApiHost = telemetryConfiguration.getTelemetryApiHost();
        this.dbService = dbService;
        buildConnectSrc();
    }

    @Override
    public void buildConnectSrc() {
        final String hostList = dbService.findPaginated(new PaginationParameters(), x -> true).stream()
                .map(dto -> String.join(" ", dto.config().hostAllowList()))
                .collect(Collectors.joining(" "));
        connectSrcValue = telemetryApiHost + " " + hostList;
    }

    @Override
    public String connectSrcValue() {
        return connectSrcValue;
    }
}
