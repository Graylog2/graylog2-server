package org.graylog2.restclient.models.api.requests;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class SessionCreateRequest extends ApiRequest {
    private final String username;
    private final String password;
    private final String host;

    public SessionCreateRequest(String username, String password, String host) {
        this.username = username;
        this.password = password;
        this.host = host;
    }
}
