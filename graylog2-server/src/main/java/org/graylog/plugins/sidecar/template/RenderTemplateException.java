package org.graylog.plugins.sidecar.template;

public class RenderTemplateException extends Exception {
    public RenderTemplateException() { }

    public RenderTemplateException(String message) {
        super(message);
    }

    public RenderTemplateException(String message, Throwable cause) {
        super(message, cause);
    }
}
