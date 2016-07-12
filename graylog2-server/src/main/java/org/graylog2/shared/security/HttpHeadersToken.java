package org.graylog2.shared.security;

import org.apache.shiro.authc.HostAuthenticationToken;

import javax.ws.rs.core.MultivaluedMap;

public class HttpHeadersToken implements HostAuthenticationToken {

    private final MultivaluedMap<String, String> httpHeaders;
    private final String host;

    public HttpHeadersToken(MultivaluedMap<String, String> httpHeaders, String host) {
        this.httpHeaders = httpHeaders;
        this.host = host;
    }

    @Override
    public Object getPrincipal() {
        return httpHeaders.get("remote_user");
    }

    @Override
    public Object getCredentials() {
        return httpHeaders.get("remote_credentials");
    }

    @Override
    public String getHost() {
        return host;
    }

    public MultivaluedMap<String, String> getHeaders() {
        return httpHeaders;
    }
}
