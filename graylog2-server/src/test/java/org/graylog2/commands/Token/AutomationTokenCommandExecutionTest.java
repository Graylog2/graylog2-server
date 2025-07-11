package org.graylog2.commands.Token;

import com.google.common.collect.ImmutableMap;
import org.bson.types.ObjectId;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.Configuration;
import org.graylog2.audit.AuditActor;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.security.AccessTokenCipher;
import org.graylog2.security.AccessTokenImpl;
import org.graylog2.security.AccessTokenService;
import org.graylog2.security.AccessTokenServiceImpl;
import org.graylog2.security.PaginatedAccessTokenEntityService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.graylog2.commands.Token.AutomationTokenCommandExecution.TOKEN_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class AutomationTokenCommandExecutionTest {
    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private AuditEventSender auditEventSender;

    @Mock
    private NodeId nodeId;

    @Mock
    private PaginatedAccessTokenEntityService paginatedAccessTokenEntityService;

    @Mock
    private ClusterConfigService configService;

    @Spy
    private Configuration configuration = new Configuration();

    private AccessTokenService accessTokenService;
    private AutomationTokenCommandExecution tokenCommandExecution;

    @Before
    public void setUp() {
        doReturn("password-secret").when(configuration)
                .getPasswordSecret();
        accessTokenService =
                new AccessTokenServiceImpl(mongodb.mongoConnection(), paginatedAccessTokenEntityService, new AccessTokenCipher(configuration), configService, configuration);
        tokenCommandExecution = new AutomationTokenCommandExecution(accessTokenService, configuration, auditEventSender, nodeId);
    }

    @Test
    public void createToken() {
        assertThat(accessTokenService.loadById(TOKEN_ID)).isNull();

        tokenCommandExecution.run("token");

        assertThat(accessTokenService.loadById(TOKEN_ID)).satisfies(token -> {
            assertThat(token).isNotNull();
            assertThat(token.getToken()).isEqualTo("token");
        });

        verify(auditEventSender).success(eq(AuditActor.system(nodeId)), eq(AuditEventTypes.USER_ACCESS_TOKEN_CREATE),
                anyMap());
    }

    @Test
    public void tokenAlreadyCreatedByPreviousCommand() {
        tokenCommandExecution.run("token");
        tokenCommandExecution.run("token");

        assertThat(accessTokenService.loadById(TOKEN_ID)).satisfies(token -> {
            assertThat(token).isNotNull();
            assertThat(token.getToken()).isEqualTo("token");
        });

        verify(auditEventSender, times(1)).success(any(AuditActor.class), anyString(), anyMap());
    }

    @Test
    public void changeToken() {
        tokenCommandExecution.run("token");
        tokenCommandExecution.run("new-token");

        assertThat(accessTokenService.loadById(TOKEN_ID)).satisfies(token -> {
            assertThat(token).isNotNull();
            assertThat(token.getToken()).isEqualTo("new-token");
        });

        verify(auditEventSender, times(2)).success(any(AuditActor.class), anyString(), anyMap());
    }

    @Test
    public void tokenAlreadyTaken() throws ValidationException {
        final AccessTokenImpl accessToken = new AccessTokenImpl(ObjectId.get(),
                ImmutableMap.of(AccessTokenImpl.TOKEN, "token",
                        AccessTokenImpl.NAME, "name",
                        AccessTokenImpl.USERNAME, "some-other-user"
                ));
        accessTokenService.save(accessToken);

        assertThatThrownBy(() -> tokenCommandExecution.run("token"))
                .hasMessageContaining("is already present")
                .isInstanceOf(RuntimeException.class);

        verifyNoMoreInteractions(auditEventSender);
    }

    @Test
    public void tokenInvalid() {
        assertThatThrownBy(() -> tokenCommandExecution.run(""))
                .hasMessageContaining("Unable to create a valid API token")
                .isInstanceOf(RuntimeException.class);

        verifyNoMoreInteractions(auditEventSender);
    }
}
