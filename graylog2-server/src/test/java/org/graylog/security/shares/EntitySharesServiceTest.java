package org.graylog.security.shares;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import org.apache.shiro.subject.Subject;
import org.graylog.grn.GRN;
import org.graylog.grn.GRNRegistry;
import org.graylog.grn.GRNTypes;
import org.graylog.security.BuiltinCapabilities;
import org.graylog.security.Capability;
import org.graylog.security.DBGrantService;
import org.graylog.security.entities.EntityDependencyPermissionChecker;
import org.graylog.security.entities.EntityDependencyResolver;
import org.graylog.testing.GRNExtension;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog.testing.mongodb.MongoJackExtension;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.plugin.database.users.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(MongoJackExtension.class)
@ExtendWith(GRNExtension.class)
@ExtendWith(MockitoExtension.class)
@MongoDBFixtures("EntitySharesServiceTest.json")
public class EntitySharesServiceTest {

    private EntitySharesService entitySharesService;

    @Mock
    private EntityDependencyResolver entityDependencyResolver;

    @Mock
    private EntityDependencyPermissionChecker entityDependencyPermissionChecker;

    @Mock
    private GranteeService granteeService;

    private GRNRegistry grnRegistry;

    @BeforeEach
    void setUp(MongoDBTestService mongodb,
               MongoJackObjectMapperProvider mongoJackObjectMapperProvider,
               GRNRegistry grnRegistry) {
        this.grnRegistry = grnRegistry;

        final DBGrantService dbGrantService = new DBGrantService(mongodb.mongoConnection(), mongoJackObjectMapperProvider, this.grnRegistry);

        lenient().when(entityDependencyResolver.resolve(any())).thenReturn(ImmutableSet.of());
        lenient().when(entityDependencyPermissionChecker.check(any(), any(), any())).thenReturn(ImmutableMultimap.of());
        lenient().when(granteeService.getAvailableGrantees(any())).thenReturn(ImmutableSet.of());

        this.entitySharesService = new EntitySharesService(dbGrantService, entityDependencyResolver, entityDependencyPermissionChecker, grnRegistry, granteeService);

        // TODO this is needed to initialize the CAPABILITIES field
        new BuiltinCapabilities();
    }

    // TODO Test more EntitySharesService functionality

    @DisplayName("Validates we cannot remove the last owner")
    @Test
    void validateLastOwnerCannotBeRemoved() {
        final GRN entity = grnRegistry.newGRN(GRNTypes.STREAM, "54e3deadbeefdeadbeefaffe");
        final EntityShareRequest shareRequest = EntityShareRequest.create(ImmutableMap.of());

        final User user = mock(User.class);
        final Subject subject = mock(Subject.class);
        when(user.getName()).thenReturn("hans");
        final EntityShareResponse entityShareResponse = entitySharesService.prepareShare(entity, shareRequest, user, subject);
        assertThat(entityShareResponse.validationResult()).satisfies(validationResult -> {
            assertThat(validationResult.failed()).isTrue();
            assertThat(validationResult.getErrors()).isNotEmpty();
            assertThat(validationResult.getErrors().get(EntityShareRequest.SELECTED_GRANTEE_CAPABILITIES))
                    .contains("Removing the following owners <[grn::::user:jane]> will leave the entity ownerless.");
        });
    }

    @DisplayName("Validates we cannot remove the last owner by changing the own capability")
    @Test
    void validateLastOwnerCannotBeRemovedByChangingCapability() {
        final GRN entity = grnRegistry.newGRN(GRNTypes.STREAM, "54e3deadbeefdeadbeefaffe");
        final GRN jane = grnRegistry.newGRN(GRNTypes.USER, "jane");
        final EntityShareRequest shareRequest = EntityShareRequest.create(ImmutableMap.of(jane, Capability.VIEW));

        final User user = mock(User.class);
        final Subject subject = mock(Subject.class);
        when(user.getName()).thenReturn("hans");
        final EntityShareResponse entityShareResponse = entitySharesService.prepareShare(entity, shareRequest, user, subject);
        assertThat(entityShareResponse.validationResult()).satisfies(validationResult -> {
            assertThat(validationResult.failed()).isTrue();
            assertThat(validationResult.getErrors()).isNotEmpty();
            assertThat(validationResult.getErrors().get(EntityShareRequest.SELECTED_GRANTEE_CAPABILITIES))
                    .contains("Removing the following owners <[grn::::user:jane]> will leave the entity ownerless.");
        });
    }

    @DisplayName("Validates we can switch owners")
    @Test
    void validateOwnerSwitch() {
        final GRN entity = grnRegistry.newGRN(GRNTypes.STREAM, "54e3deadbeefdeadbeefaffe");
        final GRN horst = grnRegistry.newGRN(GRNTypes.USER, "horst");
        final EntityShareRequest shareRequest = EntityShareRequest.create(ImmutableMap.of(horst, Capability.OWN));

        final User user = mock(User.class);
        final Subject subject = mock(Subject.class);
        when(user.getName()).thenReturn("hans");
        final EntityShareResponse entityShareResponse = entitySharesService.prepareShare(entity, shareRequest, user, subject);
        assertThat(entityShareResponse.validationResult()).satisfies(validationResult -> {
            assertThat(validationResult.failed()).isFalse();
            assertThat(validationResult.getErrors()).isEmpty();
        });
    }

    @DisplayName("Validates we can modify ownerless entitites")
    @Test
    void validateOwnerless() {
        final GRN entity = grnRegistry.newGRN(GRNTypes.DASHBOARD, "54e3deadbeefdeadbeefaffe");
        final GRN horst = grnRegistry.newGRN(GRNTypes.USER, "horst");
        final EntityShareRequest shareRequest = EntityShareRequest.create(ImmutableMap.of(horst, Capability.MANAGE));

        final User user = mock(User.class);
        final Subject subject = mock(Subject.class);
        when(user.getName()).thenReturn("hans");
        final EntityShareResponse entityShareResponse = entitySharesService.prepareShare(entity, shareRequest, user, subject);
        assertThat(entityShareResponse.validationResult()).satisfies(validationResult -> {
            assertThat(validationResult.failed()).isFalse();
            assertThat(validationResult.getErrors()).isEmpty();
        });
    }
}
