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

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.BadRequestException;
import org.apache.commons.lang3.StringUtils;
import org.graylog2.plugin.inputs.MisfireException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Generic URL allowlist validator for input endpoint override fields.
 *
 * <p>This class is the reusable building block for all input types that accept user-supplied URLs
 * (e.g. AWS, Salesforce, Mimecast, Okta). Each input type should inject this class and call the
 * appropriate method depending on where the validation is triggered:
 *
 * <ul>
 *   <li>{@link #validateForRequest(String, String)} — called from REST/setup paths (input create,
 *       edit, setup wizard, health check). Always enforces: throws {@link BadRequestException}
 *       naming the offending field.</li>
 *   <li>{@link #validateForStartup(String, String, String)} — called from the input startup path
 *       (e.g. {@code doLaunch()}). Conditionally enforces: when {@code enforce_for_inputs} is
 *       enabled it throws {@link MisfireException}; otherwise it publishes a system notification
 *       and logs a warning so existing inputs are not broken by a config upgrade.</li>
 * </ul>
 *
 * <p>All core allowlist logic lives here. For inputs with multiple URL fields (e.g. AWS with four
 * endpoint overrides), callers may loop over fields at the injection site rather than creating a
 * per-input wrapper class.
 */
@Singleton
public class InputUrlAllowlistValidator {

    private static final Logger LOG = LoggerFactory.getLogger(InputUrlAllowlistValidator.class);

    /** Navigation path shown in error messages so users know where to update the allowlist. */
    public static final String ALLOWLIST_CONFIG_PATH = "System > Configurations > URL Allowlist";

    private final UrlAllowlistService allowlistService;
    private final UrlAllowlistNotificationService notificationService;

    @Inject
    public InputUrlAllowlistValidator(UrlAllowlistService allowlistService,
                                      UrlAllowlistNotificationService notificationService) {
        this.allowlistService = allowlistService;
        this.notificationService = notificationService;
    }

    /**
     * Validates a single endpoint URL for a REST or setup-wizard context.
     *
     * <p>Enforcement is always on: if the URL is not in the allowlist, a {@link BadRequestException}
     * is thrown with a message that names the rejected field and points the user to the allowlist
     * configuration page. Null and blank URLs are silently skipped.
     *
     * @param url       the endpoint URL to validate
     * @param fieldName the configuration field name, included in the error message
     * @throws BadRequestException if {@code url} is non-empty and not in the allowlist
     */
    public void validateForRequest(String url, String fieldName) {
        if (StringUtils.isNotEmpty(url) && !allowlistService.isAllowlisted(url)) {
            throw new BadRequestException(f(
                    "The URL <%s> configured in field [%s] is not in the URL allowlist. " +
                            "Please add it in %s.",
                    url, fieldName, ALLOWLIST_CONFIG_PATH));
        }
    }

    /**
     * Validates a single endpoint URL for input startup.
     *
     * <p>For a non-empty URL that is not in the allowlist:
     * <ol>
     *   <li>A system notification is published so the event is visible in the UI.</li>
     *   <li>If {@code enforce_for_inputs} is enabled in the allowlist config, a
     *       {@link MisfireException} is thrown so the input fails to start with a clear reason.
     *       </li>
     *   <li>Otherwise only a warning is logged, allowing the input to continue starting (soft
     *       enforcement mode for safe upgrades).</li>
     * </ol>
     *
     * @param url        the endpoint URL to validate
     * @param fieldName  the configuration field name, included in the error/log message
     * @param inputTitle the human-readable input title, included in the notification and log message
     * @throws MisfireException if the URL is not in the allowlist and enforcement is enabled
     */
    public void validateForStartup(String url, String fieldName, String inputTitle) throws MisfireException {
        if (StringUtils.isEmpty(url) || allowlistService.isAllowlisted(url)) {
            return;
        }

        final String description = f(
                "The input <%s> is configured with an endpoint override [%s: %s] " +
                        "that is not in the URL allowlist. Please add it in %s.",
                inputTitle, fieldName, url, ALLOWLIST_CONFIG_PATH);
        notificationService.publishAllowlistFailure(description);

        if (allowlistService.getAllowlist().enforceForInputs()) {
            LOG.warn("Blocking input startup because endpoint URL is not allowlisted. " +
                    "[url: {}, field: {}, input: {}]", url, fieldName, inputTitle);
            throw new MisfireException(f(
                    "Endpoint URL <%s> in field [%s] is not in the URL allowlist. " +
                            "Please add it in %s, or disable \"Enforce for inputs\" to allow " +
                            "non-allowlisted endpoint overrides.",
                    url, fieldName, ALLOWLIST_CONFIG_PATH));
        }
        LOG.warn("Input is using an endpoint that is not in the URL allowlist. " +
                "Enable \"Enforce for inputs\" in {} to block non-allowlisted URLs. " +
                "[url: {}, field: {}, input: {}]", ALLOWLIST_CONFIG_PATH, url, fieldName, inputTitle);
    }
}
