package org.graylog.plugins.threatintel.whois.ip;

public class WhoisLookupException extends Exception {
    private final InternetRegistry registry;

    public WhoisLookupException(Throwable cause, InternetRegistry registry) {
        super(cause);
        this.registry = registry;
    }

    public WhoisLookupException(String message, Throwable cause, InternetRegistry registry) {
        super(message, cause);
        this.registry = registry;
    }

    public InternetRegistry getRegistry() {
        return registry;
    }
}
