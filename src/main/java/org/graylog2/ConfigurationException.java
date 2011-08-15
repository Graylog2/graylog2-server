package org.graylog2;

/**
 * Exception thrown in case of an invalid configuration
 *
 * @see Configuration
 *
 * @author Jochen Schalanda <jochen@schalanda.name>
 */
public class ConfigurationException extends Exception {

    public ConfigurationException(String message) {

        super(message);
    }
}
