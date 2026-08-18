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
package org.graylog2.system.urlallowlist;

import jakarta.ws.rs.BadRequestException;
import org.graylog2.plugin.inputs.MisfireException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class InputUrlAllowlistValidatorTest {

    private static final String FIELD = "my_endpoint";
    private static final String INPUT_TITLE = "My Input";
    private static final String URL = "https://vpce.example.internal:8443";

    @Mock
    private UrlAllowlistService allowlistService;

    @Mock
    private UrlAllowlistNotificationService notificationService;

    private InputUrlAllowlistValidator validator;

    @BeforeEach
    void setUp() {
        validator = new InputUrlAllowlistValidator(allowlistService, notificationService);
    }

    // --- validateForRequest ---

    @Test
    public void validateForRequest_passesWhenAllowlisted() {
        when(allowlistService.isAllowlisted(URL)).thenReturn(true);

        assertThatCode(() -> validator.validateForRequest(URL, FIELD)).doesNotThrowAnyException();
    }

    @Test
    public void validateForRequest_silentlySkipsNullUrl() {
        assertThatCode(() -> validator.validateForRequest(null, FIELD)).doesNotThrowAnyException();

        verify(allowlistService, never()).isAllowlisted(anyString());
    }

    @Test
    public void validateForRequest_silentlySkipsBlankUrl() {
        assertThatCode(() -> validator.validateForRequest("", FIELD)).doesNotThrowAnyException();

        verify(allowlistService, never()).isAllowlisted(anyString());
    }

    @Test
    public void validateForRequest_throwsBadRequestNamingField() {
        when(allowlistService.isAllowlisted(URL)).thenReturn(false);

        assertThatThrownBy(() -> validator.validateForRequest(URL, FIELD))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining(URL)
                .hasMessageContaining(FIELD)
                .hasMessageContaining(InputUrlAllowlistValidator.ALLOWLIST_CONFIG_PATH);
    }

    // --- validateForStartup ---

    @Test
    public void validateForStartup_silentlySkipsNullUrl() throws MisfireException {
        assertThatCode(() -> validator.validateForStartup(null, FIELD, INPUT_TITLE))
                .doesNotThrowAnyException();

        verify(allowlistService, never()).isAllowlisted(anyString());
        verify(notificationService, never()).publishAllowlistFailure(anyString());
    }

    @Test
    public void validateForStartup_silentlySkipsBlankUrl() throws MisfireException {
        assertThatCode(() -> validator.validateForStartup("", FIELD, INPUT_TITLE))
                .doesNotThrowAnyException();

        verify(allowlistService, never()).isAllowlisted(anyString());
        verify(notificationService, never()).publishAllowlistFailure(anyString());
    }

    @Test
    public void validateForStartup_passesWhenAllowlisted() throws MisfireException {
        when(allowlistService.isAllowlisted(URL)).thenReturn(true);

        assertThatCode(() -> validator.validateForStartup(URL, FIELD, INPUT_TITLE))
                .doesNotThrowAnyException();

        verify(notificationService, never()).publishAllowlistFailure(anyString());
    }

    @Test
    public void validateForStartup_publishesNotificationAndWarnsWhenEnforceDisabled() throws MisfireException {
        when(allowlistService.isAllowlisted(URL)).thenReturn(false);
        when(allowlistService.getAllowlist()).thenReturn(allowlistWithEnforceForInputs(false));

        assertThatCode(() -> validator.validateForStartup(URL, FIELD, INPUT_TITLE))
                .doesNotThrowAnyException();

        verify(notificationService).publishAllowlistFailure(anyString());
    }

    @Test
    public void validateForStartup_throwsMisfireExceptionWhenEnforceEnabled() {
        when(allowlistService.isAllowlisted(URL)).thenReturn(false);
        when(allowlistService.getAllowlist()).thenReturn(allowlistWithEnforceForInputs(true));

        assertThatThrownBy(() -> validator.validateForStartup(URL, FIELD, INPUT_TITLE))
                .isInstanceOf(MisfireException.class)
                .hasMessageContaining(URL)
                .hasMessageContaining(FIELD)
                .hasMessageContaining(InputUrlAllowlistValidator.ALLOWLIST_CONFIG_PATH);

        verify(notificationService).publishAllowlistFailure(anyString());
    }

    private static UrlAllowlist allowlistWithEnforceForInputs(boolean enforce) {
        return UrlAllowlist.create(new ArrayList<>(), false, false, enforce);
    }
}
