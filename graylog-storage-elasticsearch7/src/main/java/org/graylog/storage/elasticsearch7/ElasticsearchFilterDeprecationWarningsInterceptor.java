package org.graylog.storage.elasticsearch7;

import org.graylog.shaded.elasticsearch7.org.apache.http.Header;
import org.graylog.shaded.elasticsearch7.org.apache.http.HttpException;
import org.graylog.shaded.elasticsearch7.org.apache.http.HttpResponse;
import org.graylog.shaded.elasticsearch7.org.apache.http.HttpResponseInterceptor;
import org.graylog.shaded.elasticsearch7.org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ElasticsearchFilterDeprecationWarningsInterceptor implements HttpResponseInterceptor {
    @Override
    public void process(HttpResponse response, HttpContext context) throws
            HttpException, IOException {
        List<Header> warnings = Arrays.stream(response.getHeaders("Warning")).filter(header -> !header.getValue().contains("setting was deprecated in Elasticsearch")).collect(Collectors.toList());
        response.removeHeaders("Warning");
        warnings.stream().forEach(header -> response.addHeader(header));
    }
}
