package org.graylog.security.authservices;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import java.util.List;
import java.util.Optional;

@AutoValue
public abstract class LdapConfig {
    public static final String TYPE_TYPE = "ldap-v1";

    public static final String FIELD_SYSTEM_USERNAME = "system_username";
    public static final String FIELD_SERVER_LIST = "server_list";
    public static final String FIELD_ENCRYPTION = "encryption";
    public static final String FIELD_VERIFY_CERTIFICATES = "verify_certificates";

    @JsonProperty(FIELD_SYSTEM_USERNAME)
    public abstract Optional<String> systemUsername();

    @JsonProperty("encrypted_system_password")
    public abstract String encryptedSystemPassword();

    @JsonProperty(FIELD_SERVER_LIST)
    public abstract List<LdapServer> serverList();

    @JsonProperty(FIELD_ENCRYPTION)
    public abstract EncryptionSetting encyrption();

    @JsonProperty(FIELD_VERIFY_CERTIFICATES)
    public abstract boolean verifyCertificates();

    public static Builder builder() {
        return Builder.create();
    }

    public enum EncryptionSetting {
        @JsonProperty("none")
        NONE,
        @JsonProperty("ssl")
        SSL,
        @JsonProperty("start_tls")
        START_TLS
    }

    @AutoValue
    public abstract static class LdapServer {

        public static final String FIELD_HOSTNAME = "hostname";
        public static final String FIELD_PORT = "port";

        @JsonProperty(FIELD_HOSTNAME)
        public abstract String hostname();

        @JsonProperty(FIELD_PORT)
        public abstract int port();

        public static LdapServer create(String hostname, int port) {
            return new AutoValue_LdapConfig_LdapServer(hostname, port);
        }
    }

    @AutoValue.Builder
    public abstract static class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_LdapConfig.Builder();
        }

        public abstract Builder systemUsername(String systemUsername);

        public abstract Builder encryptedSystemPassword(String encryptedSystemPassword);

        public abstract Builder serverList(List<LdapServer> serverList);

        public abstract Builder encyrption(EncryptionSetting encyrption);

        public abstract Builder verifyCertificates(boolean verifyCertificates);

        public abstract LdapConfig build();
    }
}
