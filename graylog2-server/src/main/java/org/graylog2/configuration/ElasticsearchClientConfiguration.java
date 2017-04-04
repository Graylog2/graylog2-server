package org.graylog2.configuration;

import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.validators.PositiveIntegerValidator;
import org.graylog2.configuration.converters.URIListConverter;
import org.graylog2.configuration.validators.ListOfURIsWithHostAndSchemeValidator;
import org.graylog2.configuration.validators.NonEmptyListValidator;

import java.net.URI;
import java.util.Collections;
import java.util.List;

public class ElasticsearchClientConfiguration {
    @Parameter(value = "elasticsearch_hosts", converter = URIListConverter.class, validators = { NonEmptyListValidator.class, ListOfURIsWithHostAndSchemeValidator.class })
    private List<URI> elasticsearchHosts = Collections.singletonList(URI.create("http://127.0.0.1:9200"));

    @Parameter(value = "elasticsearch_connect_timeout", validators = { PositiveIntegerValidator.class })
    private int elasticsearchConnectTimeout = 1000;

    @Parameter(value = "elasticsearch_socket_timeout", validators = { PositiveIntegerValidator.class })
    private int elasticsearchSocketTimeout = 60000;

    @Parameter(value = "elasticsearch_max_retry_timeout", validators = { PositiveIntegerValidator.class })
    private int elasticsearchMaxRetryTimeout = 60000;
}
