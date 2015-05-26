/*
 * Copyright 2012-2015 TORCH GmbH, 2015 Graylog, Inc.
 *
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package controllers;

import lib.BreadcrumbList;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.models.accounts.LdapSettings;
import org.graylog2.restclient.models.accounts.LdapSettingsService;
import org.graylog2.restclient.models.api.requests.accounts.LdapSettingsRequest;
import org.graylog2.restclient.models.api.requests.accounts.LdapTestConnectionRequest;
import org.graylog2.restclient.models.api.responses.accounts.LdapConnectionTestResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.data.DynamicForm;
import play.data.Form;
import play.libs.Json;
import play.mvc.Result;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Map;

import static com.google.common.base.MoreObjects.firstNonNull;
import static play.data.Form.form;

public class LdapController extends AuthenticatedController {
    private static final Logger log = LoggerFactory.getLogger(LdapController.class);
    private final Form<LdapSettingsRequest> settingsForm = form(LdapSettingsRequest.class);

    private final LdapSettingsService ldapSettingsService;

    @Inject
    public LdapController(final LdapSettingsService ldapSettingsService) {
        this.ldapSettingsService = ldapSettingsService;
    }

    public Result index() {
        final LdapSettings ldapSettings = ldapSettingsService.load();
        final Form<LdapSettingsRequest> ldapSettingsForm;
        if (ldapSettings == null) {
            final LdapSettingsRequest newRequest = new LdapSettingsRequest();
            newRequest.ldapUri = "ldap:///";
            newRequest.enabled = true;
            newRequest.defaultGroup = "";
            ldapSettingsForm = settingsForm.fill(newRequest);
        } else {
            ldapSettingsForm = settingsForm.fill(ldapSettings.toRequest());
        }

        return ok(views.html.system.ldap.index.render(currentUser(), breadcrumbs(), ldapSettingsForm));
    }

    public Result apiTestLdapConnection() {
        final DynamicForm dynamicForm = form().bindFromRequest("url", "systemUsername", "systemPassword", "ldapType", "useStartTls", "trustAllCertificates");
        final Map<String, String> formData = dynamicForm.data();
        LdapConnectionTestResponse result;
        try {
            final LdapTestConnectionRequest request = getLdapTestConnectionRequest(formData);
            request.testConnectOnly = true;
            result = ldapSettingsService.testLdapConfiguration(request);
        } catch (APIException e) {
            // couldn't connect
            log.error("Unable to test connection: {}", e.getMessage());
            return internalServerError();
        } catch (IOException e) {
            log.error("Unable to connect", e);
            return internalServerError();
        }
        return ok(Json.toJson(result));
    }

    public Result apiTestLdapLogin() {
        final DynamicForm dynamicForm = form().bindFromRequest(
                "url", "systemUsername", "systemPassword", "ldapType", "useStartTls", "trustAllCertificates",
                "searchBase", "searchPattern", "principal", "password");
        final Map<String, String> formData = dynamicForm.data();

        LdapConnectionTestResponse result;
        try {
            final LdapTestConnectionRequest request = getLdapTestConnectionRequest(formData);
            // also try to login, don't just test the connection
            request.testConnectOnly = false;

            request.searchBase = formData.get("searchBase");
            request.searchPattern = formData.get("searchPattern");
            request.principal = formData.get("principal");
            request.password = formData.get("password");
            result = ldapSettingsService.testLdapConfiguration(request);
        } catch (APIException e) {
            log.error("Unable to test login: {}", e.getMessage());
            return internalServerError();
        } catch (IOException e) {
            log.error("Unable to connect", e);
            return internalServerError();
        }
        return ok(Json.toJson(result));
    }

    private LdapTestConnectionRequest getLdapTestConnectionRequest(Map<String, String> formData) {
        final LdapTestConnectionRequest request = new LdapTestConnectionRequest();
        request.ldapUri = formData.get("url");
        request.systemUsername = formData.get("systemUsername");
        request.systemPassword = formData.get("systemPassword");
        request.isActiveDirectory = firstNonNull(formData.get("ldapType"), "ldap").equalsIgnoreCase("ad");
        request.useStartTls = firstNonNull(formData.get("useStartTls"), "false").equals("true");
        request.trustAllCertificates = firstNonNull(formData.get("trustAllCertificates"), "false").equals("true");
        return request;
    }

    public Result saveLdapSettings() {
        final Form<LdapSettingsRequest> form = settingsForm.bindFromRequest();
        if (form.hasErrors()) {
            flash("error", "Please correct these errors: " + form.errors());
            return badRequest(views.html.system.ldap.index.render(currentUser(), breadcrumbs(), form));
        }
        final LdapSettingsRequest formValue = form.get();
        try {
            final LdapSettings settings = ldapSettingsService.create(formValue);
            if (settings.save()) {
                flash("success", "LDAP settings updated");
            } else {
                flash("error", "Unable to update LDAP settings!");
            }
        } catch (RuntimeException e) {
            flash("error", "Unable to update LDAP settings!");
        }
        return redirect(routes.UsersController.index());
    }

    public Result removeLdapSettings() {
        final LdapSettings ldapSettings = ldapSettingsService.load();
        if (ldapSettings == null) {
            flash("error", "No LDAP configuration found.");
        } else {
            if (!ldapSettings.delete()) {
                flash("error", "Could not remove the LDAP configuration.");
            }
        }
        return redirect(routes.UsersController.index());
    }

    private BreadcrumbList breadcrumbs() {
        BreadcrumbList bc = new BreadcrumbList();
        bc.addCrumb("System", routes.SystemController.index(0));
        bc.addCrumb("Users", routes.UsersController.index());
        bc.addCrumb("LDAP settings", routes.LdapController.index());

        return bc;
    }

}
