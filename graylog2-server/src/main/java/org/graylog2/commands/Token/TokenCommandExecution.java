package org.graylog2.commands.Token;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mongodb.MongoException;
import jakarta.inject.Inject;
import java.util.Collections;
import java.util.Map;
import org.bson.types.ObjectId;
import org.graylog2.Configuration;
import org.graylog2.audit.AuditActor;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.database.utils.MongoUtils;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.security.AccessToken;
import org.graylog2.security.AccessTokenImpl;
import org.graylog2.security.AccessTokenService;

public class TokenCommandExecution {
    public static final String TOKEN_NAME = "Cloud Automation";
    public static final String TOKEN_ID = "000000000000000000000001";

    private final AccessTokenService accessTokenService;
    private final String rootUsername;
    private final AuditEventSender auditEventSender;
    private final NodeId nodeId;

    @Inject
    public TokenCommandExecution(AccessTokenService accessTokenService, Configuration configuration,
                                 AuditEventSender auditEventSender, NodeId nodeId) {
        this.accessTokenService = accessTokenService;
        this.rootUsername = configuration.getRootUsername();
        this.auditEventSender = auditEventSender;
        this.nodeId = nodeId;
    }

    public void run(String tokenValue) {
        AccessToken existingToken = accessTokenService.loadById(TOKEN_ID);
        if (existingToken != null && existingToken.getToken().equals(tokenValue)) {
            System.out.println("A token with name '" + existingToken.getName() +
                    "' and the provided value was already created by a previous run of this command. All good!");
            return;
        }

        AccessToken token = createToken(tokenValue);
        try {
            accessTokenService.save(token);
            logAuditEvent(token);
            System.out.println(
                    "Created/updated token with name '" + token.getName() + "' for user '" + rootUsername + "'.");
        } catch (MongoException e) {
            if (MongoUtils.isDuplicateKeyError(e)) {
                throw new RuntimeException("ERROR: Unable to add the token. This probably means that a token with the " +
                        "provided value is already present in the system but hasn't been created with this command. " +
                        "Please remove the offending token. Cause: " + e.getMessage() + ".", e);
            }
            throw e;
        } catch (ValidationException e) {
            throw new RuntimeException("ERROR: Unable to create a valid API token with the provided token value.", e);
        }
    }

    private void logAuditEvent(AccessToken token) {
        final Map<String, Object> context = ImmutableMap.of(
                "path_params", ImmutableMap.of(
                        "name", Collections.singletonList(token.getName()),
                        "username", Collections.singletonList(token.getUserName())
                )
        );

        auditEventSender.success(AuditActor.system(nodeId), AuditEventTypes.USER_ACCESS_TOKEN_CREATE,
                context);
    }

    private AccessToken createToken(String tokenValue) {
        Map<String, Object> fields = Maps.newHashMap();
        fields.put(AccessTokenImpl.TOKEN, tokenValue);
        fields.put(AccessTokenImpl.USERNAME, rootUsername);
        fields.put(AccessTokenImpl.NAME, TOKEN_NAME);
        fields.put(AccessTokenImpl.LAST_ACCESS, Tools.dateTimeFromDouble(0)); // aka never.
        return new AccessTokenImpl(new ObjectId(TOKEN_ID), fields);
    }
}
