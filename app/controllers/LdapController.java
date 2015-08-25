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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.net.MediaType;
import lib.BreadcrumbList;
import lib.json.Json;
import org.graylog2.rest.models.roles.responses.RoleResponse;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.models.RolesService;
import org.graylog2.restclient.models.accounts.LdapSettings;
import org.graylog2.restclient.models.accounts.LdapSettingsService;
import org.graylog2.restclient.models.api.requests.ApiRequest;
import org.graylog2.restclient.models.api.requests.accounts.LdapSettingsRequest;
import org.graylog2.restclient.models.api.requests.accounts.LdapTestConnectionRequest;
import org.graylog2.restclient.models.api.responses.accounts.LdapConnectionTestResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.data.DynamicForm;
import play.data.Form;
import play.mvc.BodyParser;
import play.mvc.Result;
import views.html.system.ldap.groups;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.MoreObjects.firstNonNull;
import static play.data.Form.form;

public class LdapController extends AuthenticatedController {
    private static final Logger log = LoggerFactory.getLogger(LdapController.class);
    private final Form<LdapSettingsRequest2> settingsForm = form(LdapSettingsRequest2.class);

    private final LdapSettingsService ldapSettingsService;
    private final RolesService rolesService;

    @Inject
    public LdapController(final LdapSettingsService ldapSettingsService, RolesService rolesService) {
        this.ldapSettingsService = ldapSettingsService;
        this.rolesService = rolesService;
    }

    public Result index() {
        final LdapSettings ldapSettings = ldapSettingsService.load();
        final Form<LdapSettingsRequest2> ldapSettingsForm;
        if (ldapSettings == null) {
            final LdapSettingsRequest2 newRequest = new LdapSettingsRequest2();
            newRequest.ldapUri = "ldap:///";
            newRequest.enabled = true;
            newRequest.defaultGroup = "";
            ldapSettingsForm = settingsForm.fill(newRequest);
        } else {
            final LdapSettingsRequest settingsRequest = ldapSettings.toRequest();
            final LdapSettingsRequest2 request = new LdapSettingsRequest2();
            request.enabled = settingsRequest.enabled;
            request.activeDirectory = settingsRequest.activeDirectory;
            request.ldapUri= settingsRequest.ldapUri;
            request.systemUsername = settingsRequest.systemUsername;
            request.systemPassword = settingsRequest.systemPassword;
            request.useStartTls = settingsRequest.useStartTls;
            request.trustAllCertificates = settingsRequest.trustAllCertificates;
            request.searchBase= settingsRequest.searchBase;
            request.searchPattern= settingsRequest.searchPattern;
            request.displayNameAttribute = settingsRequest.displayNameAttribute;
            request.defaultGroup = settingsRequest.defaultGroup;
            request.groupMapping = settingsRequest.groupMapping;
            request.groupSearchBase= settingsRequest.groupSearchBase;
            request.groupIdAttribute= settingsRequest.groupIdAttribute;
            request.additionalDefaultGroups = Lists.newArrayList(settingsRequest.additionalDefaultGroups == null ? Collections.<String>emptyList() : settingsRequest.additionalDefaultGroups);

            ldapSettingsForm = settingsForm.fill(request);
        }

        final List<RoleResponse> roles = Lists.newArrayList(rolesService.loadAll());
        final Set<String> selectedAdditionalGroups = ldapSettings != null && ldapSettings.getAdditionalDefaultGroups() != null ? ldapSettings.getAdditionalDefaultGroups() : Collections.<String>emptySet();
        return ok(views.html.system.ldap.index.render(currentUser(), breadcrumbs(), ldapSettingsForm, roles,
                                                      selectedAdditionalGroups));
    }
    public Result groups() {
        final LdapSettings ldapSettings = ldapSettingsService.load();
        boolean groupSettingsEnabled = false;
        if (ldapSettings != null) {
            groupSettingsEnabled = !Strings.isNullOrEmpty(ldapSettings.getGroupSearchBase()) && !Strings.isNullOrEmpty(ldapSettings.getGroupIdAttribute());
        }
        return ok(groups.render(currentUser(), ldapSettings != null && ldapSettings.isEnabled(), groupSettingsEnabled));
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
        return ok(Json.toJsonString(result)).as(MediaType.JSON_UTF_8.toString());
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
        return ok(Json.toJsonString(result)).as(MediaType.JSON_UTF_8.toString());
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
        final Form<LdapSettingsRequest2> form = settingsForm.bindFromRequest();
        if (form.hasErrors()) {
            flash("error", "Please correct these errors: " + form.errors());
            final List<RoleResponse> roles = Lists.newArrayList(rolesService.loadAll());
            final Set<String> additionalGroupsAsSet = safeAdditionalGroupsAsSet(form.get().additionalDefaultGroups);
            return badRequest(views.html.system.ldap.index.render(currentUser(), breadcrumbs(), form, roles,
                                                                  additionalGroupsAsSet));
        }
        final LdapSettingsRequest2 formValue = form.get();
        final LdapSettingsRequest request = new LdapSettingsRequest();
        request.enabled = formValue.enabled;
        request.activeDirectory = formValue.activeDirectory;
        request.ldapUri= formValue.ldapUri;
        request.systemUsername = formValue.systemUsername;
        request.systemPassword = formValue.systemPassword;
        request.useStartTls = formValue.useStartTls;
        request.trustAllCertificates = formValue.trustAllCertificates;
        request.searchBase= formValue.searchBase;
        request.searchPattern= formValue.searchPattern;
        request.displayNameAttribute = formValue.displayNameAttribute;
        request.defaultGroup = formValue.defaultGroup;
        request.groupMapping = formValue.groupMapping;
        request.groupSearchBase= formValue.groupSearchBase;
        request.groupIdAttribute= formValue.groupIdAttribute;
        request.additionalDefaultGroups = safeAdditionalGroupsAsSet(formValue.additionalDefaultGroups);

        try {
            final LdapSettings settings = ldapSettingsService.create(request);
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

    private Set<String> safeAdditionalGroupsAsSet(List<String> additionalDefaultGroups) {
        return Sets.newHashSet(additionalDefaultGroups == null ? Collections.<String>emptySet() : additionalDefaultGroups);
    }

    // TODO this is duplicated, because Set<String> additionalDefaultGroups cannot be a Set for form binding, it has to be a List
    // to avoid refactoring the entire hierarchy and all dependencies we duplicate it and then adapt the one attribute.
    public static class LdapSettingsRequest2 extends ApiRequest {

        @JsonProperty("enabled")
        public boolean enabled;

        @JsonProperty("active_directory")
        public boolean activeDirectory;

        @JsonProperty("ldap_uri")
        public String ldapUri;

        @JsonProperty("system_username")
        public String systemUsername;

        @JsonProperty("system_password")
        public String systemPassword;

        @JsonProperty("use_start_tls")
        public boolean useStartTls;

        @JsonProperty("trust_all_certificates")
        public boolean trustAllCertificates;

        @JsonProperty("search_base")
        public String searchBase;

        @JsonProperty("search_pattern")
        public String searchPattern;

        @JsonProperty("display_name_attribute")
        public String displayNameAttribute;

        @JsonProperty("default_group")
        public String defaultGroup;

        @JsonProperty("group_mapping")
        @Nullable
        public Map<String, String> groupMapping;

        @JsonProperty("group_search_base")
        @Nullable
        public String groupSearchBase;

        @JsonProperty("group_id_attribute")
        @Nullable
        public String groupIdAttribute;

        @JsonProperty("additional_default_groups")
        @Nullable
        public List<String> additionalDefaultGroups;

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

    public Result apiGroups() {
        Set<String> ldapGroups = ldapSettingsService.loadGroups();
        return ok(Json.toJsonString(ldapGroups));
    }

    public Result apiLoadGroupMapping() {
        final Map<String, String> groupMapping = ldapSettingsService.getGroupMapping();
        return ok(Json.toJsonString(groupMapping));
    }

    @BodyParser.Of(BodyParser.Json.class)
    public Result apiSaveGroupMapping() {
        //noinspection unchecked
        Map<String, String> mapping = (Map<String, String>)Json.fromJson(request().body().asJson(), Map.class);

        ldapSettingsService.updateGroupMapping(mapping);

        return ok();
    }

    private BreadcrumbList breadcrumbs() {
        BreadcrumbList bc = new BreadcrumbList();
        bc.addCrumb("System", routes.SystemController.index(0));
        bc.addCrumb("Users", routes.UsersController.index());
        bc.addCrumb("LDAP settings", routes.LdapController.index());

        return bc;
    }

}
