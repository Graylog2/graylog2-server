package org.graylog2.restroutes;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class PathMethod {
    private final String method;
    private final String path;

    public PathMethod(String method, String path) {
        this.method = method;
        this.path = path;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }
}
