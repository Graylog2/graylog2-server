package org.graylog2.indexer.cluster.jest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.RequestLine;
import org.apache.http.StatusLine;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestResponseLogger implements HttpResponseInterceptor {

  private static final Logger LOG = LoggerFactory.getLogger(RequestResponseLogger.class);

  private final Logger logger;

  public RequestResponseLogger() {
    this(LOG);
  }

  public RequestResponseLogger(Logger logger) {
    this.logger = logger;
  }

  @Override
  public void process(HttpResponse response, HttpContext context)
      throws HttpException, IOException {
    final StatusLine statusLine = response.getStatusLine();
    final HttpHost targetHost = (HttpHost) context.getAttribute(HttpCoreContext.HTTP_TARGET_HOST);
    final HttpRequest httpRequest = (HttpRequest) context
        .getAttribute(HttpCoreContext.HTTP_REQUEST);
    final RequestLine request = httpRequest.getRequestLine();
    URI uri = null;
    try {
      uri = new URIBuilder(targetHost.toURI()).setPath(request.getUri()).build();
    } catch (URISyntaxException ignore) {
      // we only update the path of a valid URI, that should never fail
    }
    logger.trace("[{} {}]: {} {}",
        statusLine.getStatusCode(),
        statusLine.getReasonPhrase(),
        request.getMethod(),
        uri
    );
  }
}
