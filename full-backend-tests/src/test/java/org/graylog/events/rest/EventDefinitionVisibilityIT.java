/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.events.rest;

import com.github.rholder.retry.RetryException;
import io.restassured.response.ValidatableResponse;
import net.bytebuddy.utility.RandomString;
import org.graylog.testing.completebackend.FullBackendTest;
import org.graylog.testing.completebackend.GraylogBackendConfiguration;
import org.graylog.testing.completebackend.apis.GraylogApiResponse;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.completebackend.apis.Sharing;
import org.graylog.testing.completebackend.apis.SharingRequest;
import org.graylog.testing.completebackend.apis.Users;
import org.graylog2.shared.security.RestPermissions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static java.net.HttpURLConnection.HTTP_OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.testing.completebackend.Lifecycle.CLASS;

/**
 * End-to-end coverage for the {@code enforce_event_definition_permissions} cluster setting.
 * <p>
 * The adapter-level tests ({@code MoreSearchAdapterEventDefinitionVisibilityIT}) prove each search engine
 * applies an {@link org.graylog.events.search.EventDefinitionFilter} it is handed. This test covers the
 * layer above: that the setting, combined with the subject's grants, actually produces that filter, and
 * that flipping the setting changes what a user sees through {@code /events/search}.
 * <p>
 * Fixtures seed three event definitions with fixed ids and five events across them:
 * <pre>
 *   visibility-A (…0a01): evt-a1, evt-a2   -- shared with the restricted user
 *   visibility-B (…0b01): evt-b1, evt-b2   -- not shared
 *   visibility-C (…0c01): evt-c1           -- not shared
 * </pre>
 */
@GraylogBackendConfiguration(serverLifecycle = CLASS)
public class EventDefinitionVisibilityIT {
    private static final String SEARCH_URL = "/events/search";
    private static final String EVENTS_CONFIG_URL =
            "/system/cluster_config/org.graylog.events.configuration.EventsConfiguration";

    private static final String DEF_A = "6a0000000000000000000a01";
    private static final String DEF_B = "6a0000000000000000000b01";
    private static final String DEF_C = "6a0000000000000000000c01";

    private static GraylogApis api;

    /**
     * Reads every event definition but is not granted blanket {@code eventdefinitions:read}, so the
     * enforcement path has to fall back to the per-definition grants. Blanket {@code streams:read} keeps
     * the source stream filter out of the way, so only the event definition filter varies.
     */
    private static Users.User restrictedUser;

    @BeforeAll
    static void setUp(GraylogApis graylogApis) throws ExecutionException, RetryException {
        api = graylogApis;

        api.streams().createStream("stream-vis", api.indices().defaultIndexSetId(), true);
        api.indices().waitForIndex("gl-events_0");

        api.backend().importMongoDBFixture("mongodb-event-definition-visibility.json",
                EventDefinitionVisibilityIT.class);
        api.backend().importElasticsearchFixture("events-event-definition-visibility.json",
                EventDefinitionVisibilityIT.class);

        final GraylogApiResponse role = api.roles().create("event_definition_visibility_reader",
                "may read all streams but holds no blanket event definition read permission",
                Set.of(RestPermissions.STREAMS_READ), false);
        restrictedUser = api.users().generateUserWithDefaults("visibility.reader", RandomString.make(), role);
        waitForRolesCacheRefresh();
        api.users().createUser(restrictedUser);

        grantDefinitionToRestrictedUser(DEF_A);
    }

    @AfterEach
    void resetEnforcement() {
        setEnforcement(false);
    }

    /**
     * The setting defaults to off, and with it off the grants must not narrow anything: this is the
     * upgrade path for existing installations.
     */
    @FullBackendTest
    void restrictedUserSeesEveryEventWhileEnforcementIsDisabled() {
        setEnforcement(false);

        assertThat(definitionIdsSeenBy(restrictedUser))
                .containsExactlyInAnyOrder(DEF_A, DEF_A, DEF_B, DEF_B, DEF_C);
    }

    @FullBackendTest
    void restrictedUserOnlySeesGrantedDefinitionsWhileEnforcementIsEnabled() {
        setEnforcement(true);

        assertThat(definitionIdsSeenBy(restrictedUser)).containsExactlyInAnyOrder(DEF_A, DEF_A);
    }

    /**
     * The admin holds blanket {@code eventdefinitions:read}, which short-circuits the factory to
     * "all allowed" - enabling the setting must not change what an administrator sees.
     */
    @FullBackendTest
    void adminIsUnaffectedByEnforcement() {
        setEnforcement(false);
        final var beforeEnforcement = definitionIdsSeenByAdmin();

        setEnforcement(true);

        assertThat(definitionIdsSeenByAdmin())
                .containsExactlyInAnyOrderElementsOf(beforeEnforcement)
                .containsExactlyInAnyOrder(DEF_A, DEF_A, DEF_B, DEF_B, DEF_C);
    }

    /**
     * Toggling the setting back off has to restore full visibility immediately - the filter is resolved
     * per request, so nothing may be cached from the enforced run.
     */
    @FullBackendTest
    void disablingEnforcementRestoresFullVisibility() {
        setEnforcement(true);
        assertThat(definitionIdsSeenBy(restrictedUser)).hasSize(2);

        setEnforcement(false);
        assertThat(definitionIdsSeenBy(restrictedUser)).hasSize(5);
    }

    /**
     * A user with no grants at all must see nothing rather than everything: the empty allow-list may not
     * degrade into an unfiltered search.
     */
    @FullBackendTest
    void userWithoutAnyGrantSeesNothingWhileEnforcementIsEnabled() throws ExecutionException, RetryException {
        final GraylogApiResponse role = api.roles().create("event_definition_visibility_ungranted",
                "may read all streams but has been granted no event definition",
                Set.of(RestPermissions.STREAMS_READ), false);
        final Users.User ungranted =
                api.users().generateUserWithDefaults("visibility.ungranted", RandomString.make(), role);
        waitForRolesCacheRefresh();
        api.users().createUser(ungranted);

        setEnforcement(true);

        assertThat(definitionIdsSeenBy(ungranted)).isEmpty();
    }

    /**
     * Grants are evaluated on every request, so adding one has to take effect without a restart.
     */
    @FullBackendTest
    void grantingAFurtherDefinitionWidensVisibilityImmediately() {
        setEnforcement(true);
        assertThat(definitionIdsSeenBy(restrictedUser)).containsExactlyInAnyOrder(DEF_A, DEF_A);

        grantDefinitionToRestrictedUser(DEF_A, DEF_C);
        try {
            assertThat(definitionIdsSeenBy(restrictedUser)).containsExactlyInAnyOrder(DEF_A, DEF_A, DEF_C);
        } finally {
            grantDefinitionToRestrictedUser(DEF_A);
        }
    }

    /**
     * Revoking the last grant must close visibility again, not leave the previous allow-list in place.
     */
    @FullBackendTest
    void revokingEveryGrantClosesVisibilityImmediately() {
        setEnforcement(true);
        assertThat(definitionIdsSeenBy(restrictedUser)).containsExactlyInAnyOrder(DEF_A, DEF_A);

        revokeAllGrants(DEF_A);
        try {
            assertThat(definitionIdsSeenBy(restrictedUser)).isEmpty();
        } finally {
            grantDefinitionToRestrictedUser(DEF_A);
        }
    }

    // --- helpers ---

    /**
     * Roles live in MongoDB but the auth backend only refreshes them once a second, so a request issued
     * right after creating a role can be evaluated against stale permissions.
     *
     * @see org.graylog2.security.InMemoryRolePermissionResolver
     */
    private static void waitForRolesCacheRefresh() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private static void setEnforcement(boolean enabled) {
        api.put(EVENTS_CONFIG_URL, """
                {
                  "events_search_timeout": 60000,
                  "events_notification_retry_period": 300000,
                  "events_notification_default_backlog": 50,
                  "events_catchup_window": 3600000,
                  "events_notification_tcp_keepalive": false,
                  "enforce_event_definition_permissions": %s
                }
                """.formatted(enabled), 202);
    }

    private static void grantDefinitionToRestrictedUser(String... definitionIds) {
        final String userId = api.users().getUserInfo(restrictedUser.username()).getString("id");
        for (String definitionId : definitionIds) {
            api.sharing().setSharing(new SharingRequest(
                    new SharingRequest.Entity("event_definition", definitionId),
                    Map.of(new SharingRequest.Entity(Sharing.ENTITY_USER, userId), Sharing.PERMISSION_VIEW)));
        }
    }

    private static void revokeAllGrants(String definitionId) {
        api.sharing().setSharing(new SharingRequest(
                new SharingRequest.Entity("event_definition", definitionId), Map.of()));
    }

    private static List<String> definitionIdsSeenByAdmin() {
        return definitionIdsSeenBy(Users.LOCAL_ADMIN);
    }

    private static List<String> definitionIdsSeenBy(Users.User user) {
        final ValidatableResponse response = api.post(SEARCH_URL, user, SEARCH_BODY, HTTP_OK);
        return response.extract().jsonPath().getList("events.event.event_definition_id");
    }

    private static final String SEARCH_BODY = """
            {
              "query": "",
              "page": 1,
              "per_page": 50,
              "timerange": {
                "type": "absolute",
                "from": "2025-01-15T00:00:00.000Z",
                "to": "2025-01-16T00:00:00.000Z"
              }
            }
            """;
}
