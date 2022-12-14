package org.graylog2.plugin.outputs;

public class InsufficientLicenseException extends Exception {

    public InsufficientLicenseException(String message) {
        super(message);
    }
}
