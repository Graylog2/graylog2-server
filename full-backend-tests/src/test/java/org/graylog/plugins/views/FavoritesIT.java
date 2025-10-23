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
package org.graylog.plugins.views;

import io.restassured.response.ValidatableResponse;
import org.graylog.testing.completebackend.apis.GraylogApis;
import org.graylog.testing.completebackend.apis.Users;
import org.graylog.testing.completebackend.FullBackendTest;
import org.graylog.testing.completebackend.GraylogBackendConfiguration;
import org.junit.jupiter.api.BeforeAll;

import java.util.Collections;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.IsEqual.equalTo;

@GraylogBackendConfiguration
public class FavoritesIT {
    private static final String USERNAME = "john.doe1";
    private static final String PASSWORD = "asdfgh";
    private GraylogApis api;

    @BeforeAll
    public void init(GraylogApis api) {
        this.api = api;
        api.users().createUser(new Users.User(
                USERNAME,
                PASSWORD,
                "John",
                "Doe",
                "john.doe1@example.com",
                false,
                30_000,
                "Europe/Vienna",
                Collections.emptyList(),
                Collections.emptyList()
        ));
    }

    @FullBackendTest
    void testCreateDeleteFavorite() {
        final String defaultIndexSetId = api.indices().defaultIndexSetId();
        final String temporaryStream = api.streams().createStream("Temporary", defaultIndexSetId);
        var grn = "grn::::stream:" + temporaryStream;

        given()
                .spec(api.requestSpecification())
                .auth().basic(USERNAME, PASSWORD)
                .when()
                .put("/favorites/" + grn)
                .then()
                .log().ifStatusCodeMatches(not(204))
                .statusCode(204);

        var validatableResponse = getFavourites();
        validatableResponse.assertThat().body("favorites[0].grn", equalTo(grn));

        given()
                .spec(api.requestSpecification())
                .auth().basic(USERNAME, PASSWORD)
                .when()
                .delete("/favorites/" + grn)
                .then()
                .log().ifStatusCodeMatches(not(204))
                .statusCode(204);

        validatableResponse = getFavourites();
        validatableResponse.assertThat().body("favorites", empty());
    }

    private ValidatableResponse getFavourites() {
        return given()
                .spec(api.requestSpecification())
                .auth().basic(USERNAME, PASSWORD)
                .when()
                .get("/favorites")
                .then()
                .log().ifStatusCodeMatches(not(200))
                .statusCode(200);
    }


}
