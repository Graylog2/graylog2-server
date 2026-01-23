package org.graylog2.security.encryption;

public interface EncryptedConfigUpdatePreparation {
    Object prepareConfigUpdate(Object existingConfig, Object newConfigObject);
}
