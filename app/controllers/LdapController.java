/*
 * Copyright 2013 TORCH UG
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package controllers;

import com.google.gson.Gson;
import com.google.inject.Inject;
import lib.APIException;
import lib.BreadcrumbList;
import models.accounts.LdapSettings;
import models.accounts.LdapSettingsService;
import models.api.requests.accounts.LdapSettingsRequest;
import models.api.requests.accounts.LdapTestConnectionRequest;
import models.api.responses.accounts.LdapConnectionTestResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.data.DynamicForm;
import play.data.Form;
import play.mvc.Result;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.Map;

import static com.google.common.base.Objects.firstNonNull;
import static play.data.Form.form;

public class LdapController extends AuthenticatedController {
    private static final Logger log = LoggerFactory.getLogger(LdapController.class);
    private final Form<LdapSettingsRequest> settingsForm = form(LdapSettingsRequest.class);

    @Inject
    private LdapSettingsService ldapSettingsService;

    public Result index() {
        final LdapSettings ldapSettings = ldapSettingsService.load();
        final Form<LdapSettingsRequest> ldapSettingsForm;
        if (ldapSettings == null) {
            final LdapSettingsRequest newRequest = new LdapSettingsRequest();
            newRequest.ldapUri = "ldap:///";
            newRequest.enabled = true;
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
            result = api().post(LdapConnectionTestResponse.class).path("/system/ldap/test").body(request).execute();
        } catch (APIException e) {
            // couldn't connect
            log.error("Unable to test connection: {}", e.getMessage());
            return internalServerError();
        } catch (IOException e) {
            log.error("Unable to connect", e);
            return internalServerError();
        }
        return ok(new Gson().toJson(result)).as(MediaType.APPLICATION_JSON);
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
            result = api().post(LdapConnectionTestResponse.class).path("/system/ldap/test").body(request).execute();
        } catch (APIException e) {
            log.error("Unable to test login: {}", e.getMessage());
            return internalServerError();
        } catch (IOException e) {
            log.error("Unable to connect", e);
            return internalServerError();
        }
        return ok(new Gson().toJson(result)).as(MediaType.APPLICATION_JSON);
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
