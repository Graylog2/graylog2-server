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

import com.google.inject.Inject;
import models.accounts.LdapSettings;
import models.accounts.LdapSettingsService;
import models.api.requests.accounts.LdapSettingsRequest;
import play.data.Form;
import play.mvc.Result;
import static play.data.Form.form;

public class LdapController extends AuthenticatedController {

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
        return ok(views.html.system.ldap.index.render(currentUser(), ldapSettingsForm));
    }

    public Result apiTestLdapConnection() {
        return ok();
    }

    public Result apiTestLdapLogin() {
        return ok();
    }

    public Result saveLdapSettings() {
        final Form<LdapSettingsRequest> form = settingsForm.bindFromRequest();
        if (form.hasErrors()) {
            flash("error", "Please correct these errors: " + form.errors());
            return badRequest(views.html.system.ldap.index.render(currentUser(), form));
        }
        final LdapSettingsRequest formValue = form.get();
        final LdapSettings settings = ldapSettingsService.create(formValue);
        if (settings.save()) {
            flash("success", "LDAP settings updated");
        } else {
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
}
