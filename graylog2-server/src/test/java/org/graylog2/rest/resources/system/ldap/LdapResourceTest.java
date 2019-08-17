package org.graylog2.rest.resources.system.ldap;

import org.graylog2.rest.models.system.ldap.requests.LdapSystemPasswordValidationRequest;
import org.graylog2.rest.models.system.ldap.responses.LdapSystemPasswordValidationResponse;
import org.graylog2.security.ldap.LdapConnector;
import org.graylog2.security.ldap.LdapSettingsImpl;
import org.graylog2.security.ldap.LdapSettingsService;
import org.graylog2.shared.bindings.GuiceInjectorHolder;
import org.graylog2.shared.security.ldap.LdapSettings;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import javax.ws.rs.BadRequestException;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LdapResourceTest {

    private static String VALID_PASSWORD = "1234";
    private static String INVALID_PASSWORD = "4321";

    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @Mock
    private LdapSettingsService ldapSettingsService;

    @Mock
    private LdapSettingsImpl.Factory ldapSettingsFactory;

    @Mock
    private LdapConnector ldapConnector;

    private LdapResource resource;

    public LdapResourceTest(){
        GuiceInjectorHolder.createInjector(Collections.emptyList());
    }

    @Before
    public void setUp() {
        this.resource = new LdapResource(this.ldapSettingsService, this.ldapSettingsFactory, this.ldapConnector);
    }

    @Test
    public void testValidateSystemPasswordThrowsExceptionIfLdapIsNotConfigured() throws BadRequestException {
        when(this.ldapSettingsService.load()).thenReturn(null);
        LdapSystemPasswordValidationRequest request = this.buildRequest(VALID_PASSWORD);

        expectedException.expect(BadRequestException.class);

        this.resource.validateSystemPassword(request);
    }

    @Test
    public void testValidateSystemPasswordThrowsExceptionIfLdapIsDisabled() throws BadRequestException {
        LdapSettings disabledLdapSettings = this.getLdapSettings(false, VALID_PASSWORD);
        when(this.ldapSettingsService.load()).thenReturn(disabledLdapSettings);
        LdapSystemPasswordValidationRequest request = this.buildRequest(VALID_PASSWORD);

        expectedException.expect(BadRequestException.class);

        this.resource.validateSystemPassword(request);
    }

    @Test
    public void testValidateSystemPasswordWithValidPassword() throws BadRequestException {
        LdapSettings enabledLdapSettings = this.getLdapSettings(true, VALID_PASSWORD);
        when(this.ldapSettingsService.load()).thenReturn(enabledLdapSettings);
        LdapSystemPasswordValidationRequest request = this.buildRequest(VALID_PASSWORD);

        LdapSystemPasswordValidationResponse response = this.resource.validateSystemPassword(request);

        assertThat(response.systemPasswordMatches()).isTrue();
    }

    @Test
    public void testValidateSystemPasswordWithInvalidPassword() throws BadRequestException {
        LdapSettings enabledLdapSettings = this.getLdapSettings(true, VALID_PASSWORD);
        when(this.ldapSettingsService.load()).thenReturn(enabledLdapSettings);
        LdapSystemPasswordValidationRequest request = this.buildRequest(INVALID_PASSWORD);

        LdapSystemPasswordValidationResponse response = this.resource.validateSystemPassword(request);

        assertThat(response.systemPasswordMatches()).isFalse();
    }

    private LdapSystemPasswordValidationRequest buildRequest(String password) {
        return new LdapSystemPasswordValidationRequest() {
            @Override
            public String systemPassword() {
                return password;
            }
        };
    }

    private LdapSettings getLdapSettings(boolean enabled, String password) {
        LdapSettings ldapSettings = mock(LdapSettings.class);
        when(ldapSettings.isEnabled()).thenReturn(enabled);
        when(ldapSettings.getSystemPassword()).thenReturn(password);
        return ldapSettings;
    }
}
