package org.graylog2.migrations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.graylog.grn.GRNRegistry;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog2.inputs.EncryptedInputConfigs;
import org.graylog2.inputs.codecs.JsonPathCodec;
import org.graylog2.inputs.misc.jsonpath.JsonPathInput;
import org.graylog2.inputs.transports.HttpPollTransport;
import org.graylog2.jackson.InputConfigurationBeanDeserializerModifier;
import org.graylog2.migrations.V20230213160000_EncryptedInputConfigMigration.MigrationCompleted;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.security.encryption.EncryptedValue;
import org.graylog2.security.encryption.EncryptedValueMapperConfig;
import org.graylog2.security.encryption.EncryptedValueService;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.shared.inputs.InputDescription;
import org.graylog2.shared.inputs.MessageInputFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.plugin.inputs.MessageInput.FIELD_CONFIGURATION;
import static org.graylog2.plugin.inputs.MessageInput.FIELD_TITLE;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(MockitoExtension.class)
@MongoDBFixtures("V20230213160000_EncryptedInputConfigMigrationTest.json")
public class V20230213160000_EncryptedInputConfigMigrationTest {
    private static final String ENCRYPTED_FIELD = "encrypted_headers";


    private final EncryptedValueService encryptedValueService =
            new EncryptedValueService("0123456789012345");

    private ObjectMapper dbObjectMapper;
    private MongoCollection<Document> collection;
    private V20230213160000_EncryptedInputConfigMigration migration;

    @Mock
    ClusterConfigService clusterConfigService;

    @Mock
    MessageInputFactory messageInputFactory;

    @BeforeEach
    public void setUp(MongoDBTestService mongodb) throws Exception {

        when(clusterConfigService.getOrDefault(MigrationCompleted.class, new MigrationCompleted(Map.of())))
                .thenReturn(new MigrationCompleted(Map.of()));

        // necessary setup for type information
        when(messageInputFactory.getConfig(eq(JsonPathInput.class.getCanonicalName())))
                .thenReturn(Optional.of(fakeJsonPathInputConfig()));
        when(messageInputFactory.getAvailableInputs())
                .thenReturn(Map.of(JsonPathInput.class.getCanonicalName(), mock(InputDescription.class)));

        ObjectMapper objectMapper = new ObjectMapperProvider(ObjectMapperProvider.class.getClassLoader(),
                Collections.emptySet(),
                encryptedValueService,
                GRNRegistry.createWithBuiltinTypes(),
                new InputConfigurationBeanDeserializerModifier(messageInputFactory)).get();

        dbObjectMapper = objectMapper.copy();
        EncryptedValueMapperConfig.enableDatabase(dbObjectMapper);

        migration = new V20230213160000_EncryptedInputConfigMigration(clusterConfigService,
                mongodb.mongoConnection(), messageInputFactory, objectMapper);

        collection = mongodb.mongoConnection().getMongoDatabase().getCollection("inputs");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void migrateUnencryptedSecret() {
        migration.upgrade();

        final Document migrated =
                collection.find(Filters.eq(FIELD_TITLE, "unencrypted-secret")).first();

        assertThat(migrated).isNotNull().satisfies(doc ->
                assertThat((Map<String, Object>) doc.get(FIELD_CONFIGURATION)).satisfies(config -> {
                    final Object sourceValue = config.get(ENCRYPTED_FIELD);
                    assertThat(sourceValue).isInstanceOf(Map.class);
                    final EncryptedValue encryptedValue = dbObjectMapper.convertValue(sourceValue, EncryptedValue.class);
                    assertThat(encryptedValueService.decrypt(encryptedValue)).isEqualTo("X-Encrypted-Header: secret");
                })
        );
    }

    @SuppressWarnings("unchecked")
    @Test
    public void migrateEmptySecret() {
        migration.upgrade();

        final Document migrated =
                collection.find(Filters.eq(FIELD_TITLE, "empty-secret")).first();

        assertThat(migrated).isNotNull().satisfies(doc ->
                assertThat((Map<String, Object>) doc.get(FIELD_CONFIGURATION)).satisfies(config -> {
                    final EncryptedValue encryptedValue =
                            dbObjectMapper.convertValue(config.get(ENCRYPTED_FIELD), EncryptedValue.class);
                    assertThat(encryptedValue.isSet()).isFalse();
                })
        );
    }

    @SuppressWarnings("unchecked")
    @Test
    public void migrateNullSecret() {
        migration.upgrade();

        final Document migrated =
                collection.find(Filters.eq(FIELD_TITLE, "null-secret")).first();

        assertThat(migrated).isNotNull().satisfies(doc ->
                assertThat((Map<String, Object>) doc.get(FIELD_CONFIGURATION)).satisfies(config -> {
                    final EncryptedValue encryptedValue =
                            dbObjectMapper.convertValue(config.get(ENCRYPTED_FIELD), EncryptedValue.class);
                    assertThat(encryptedValue.isSet()).isFalse();
                })
        );
    }

    @SuppressWarnings("unchecked")
    @Test
    public void migrateMissingSecret() {
        migration.upgrade();

        final Document migrated =
                collection.find(Filters.eq(FIELD_TITLE, "missing-secret")).first();

        assertThat(migrated).isNotNull().satisfies(doc ->
                assertThat((Map<String, Object>) doc.get(FIELD_CONFIGURATION))
                        .doesNotContainKey(ENCRYPTED_FIELD)
        );
    }

    @SuppressWarnings("unchecked")
    @Test
    public void alreadyEncrypted() {
        migration.upgrade();

        final Document migrated =
                collection.find(Filters.eq(FIELD_TITLE, "already-encrypted")).first();

        assertThat(migrated).isNotNull().satisfies(doc ->
                assertThat((Map<String, Object>) doc.get(FIELD_CONFIGURATION)).satisfies(config -> {
                    final Object sourceValue = config.get(ENCRYPTED_FIELD);
                    assertThat(sourceValue).isInstanceOf(Map.class);
                    final EncryptedValue encryptedValue = dbObjectMapper.convertValue(sourceValue, EncryptedValue.class);
                    assertThat(encryptedValueService.decrypt(encryptedValue)).isEqualTo("X-Encrypted-Header: secret");
                })
        );
    }

    @Test
    public void migrationCompleted() {
        migration.upgrade();

        final MigrationCompleted migrationCompleted = new MigrationCompleted(Map.of(
                JsonPathInput.class.getCanonicalName(),
                EncryptedInputConfigs.getEncryptedFields(fakeJsonPathInputConfig())
        ));

        verify(clusterConfigService, times(1)).write(migrationCompleted);

        // pretend that all fields have been migrated already
        when(clusterConfigService.getOrDefault(MigrationCompleted.class, new MigrationCompleted(Map.of())))
                .thenReturn(migrationCompleted);

        migration.upgrade();

        // this time, we should have aborted early and not persist the completion marker again
        verify(clusterConfigService, times(1)).write(migrationCompleted);
    }

    private static MessageInput.Config fakeJsonPathInputConfig() {
        return new JsonPathInput.Config(new HttpPollTransport.Factory() {
            @Override
            public HttpPollTransport create(Configuration configuration) {
                throw new IllegalStateException("Unexpected");
            }

            @Override
            public HttpPollTransport.Config getConfig() {
                return new HttpPollTransport.Config();
            }
        }, new JsonPathCodec.Factory() {
            @Override
            public JsonPathCodec create(Configuration configuration) {
                throw new IllegalStateException("Unexpected");
            }

            @Override
            public JsonPathCodec.Config getConfig() {
                return new JsonPathCodec.Config();
            }

            @Override
            public JsonPathCodec.Descriptor getDescriptor() {
                throw new IllegalStateException("Unexpected");
            }
        });
    }
}
