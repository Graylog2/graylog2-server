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
package org.graylog.events.notifications.types;

import com.google.common.base.Strings;
import com.unboundid.util.Base64;
import okhttp3.HttpUrl;
import okhttp3.Request;
import org.graylog.events.notifications.NotificationTestData;
import org.graylog.events.notifications.TemporaryEventNotificationException;
import org.graylog2.security.encryption.EncryptedValue;
import org.graylog2.security.encryption.EncryptedValueService;
import org.graylog2.system.urlwhitelist.UrlWhitelistNotificationService;
import org.graylog2.system.urlwhitelist.UrlWhitelistService;

import java.nio.charset.StandardCharsets;

public class HTTPNotification {
    private final UrlWhitelistService whitelistService;
    private final UrlWhitelistNotificationService urlWhitelistNotificationService;
    private final EncryptedValueService encryptedValueService;

    public HTTPNotification(UrlWhitelistService whitelistService,
                            UrlWhitelistNotificationService urlWhitelistNotificationService,
                            EncryptedValueService encryptedValueService) {
        this.whitelistService = whitelistService;
        this.urlWhitelistNotificationService = urlWhitelistNotificationService;
        this.encryptedValueService = encryptedValueService;
    }


    public HttpUrl validateUrl(String url, String notificationId, String eventDefTitle) throws TemporaryEventNotificationException {
        final HttpUrl httpUrl = HttpUrl.parse(url);
        if (httpUrl == null) {
            throw new TemporaryEventNotificationException(
                    "Malformed URL: <" + url + "> in notification <" + notificationId + ">");
        }
        if (!whitelistService.isWhitelisted(url)) {
            if (!NotificationTestData.TEST_NOTIFICATION_ID.equals(notificationId)) {
                publishSystemNotificationForWhitelistFailure(url, eventDefTitle);
            }
            throw new TemporaryEventNotificationException("URL <" + url + "> is not whitelisted.");
        }
        return httpUrl;
    }

    public void addAuthHeader(Request.Builder builder, EncryptedValue auth) {
        final String basicAuthHeaderValue = getBasicAuthHeaderValue(auth);
        if (!Strings.isNullOrEmpty(basicAuthHeaderValue)) {
            builder.addHeader("Authorization", basicAuthHeaderValue);
        }
    }

    public void addApiKey(Request.Builder builder, HttpUrl httpUrl, String apiKey, EncryptedValue apiSecret, boolean sendAsHeader) {
        // Add API key if it exists
        if (!Strings.isNullOrEmpty(apiKey)) {
            final String apiKeyValue = getApiKeyValue(apiSecret);
            if (sendAsHeader) {
                builder.url(httpUrl);
                builder.addHeader(apiKey, apiKeyValue);
            } else {
                builder.url(httpUrl.newBuilder().addQueryParameter(apiKey, apiKeyValue).build());
            }
        } else {
            builder.url(httpUrl);
        }
    }

    public void addHeaders(Request.Builder builder, String headerString) {
        if (!Strings.isNullOrEmpty(headerString)) {
            for (String nameVal : headerString.split(";")) {
                String[] nameValArr = nameVal.split(":", 2);
                String name = nameValArr[0].trim();
                String val = nameValArr[1].trim();
                builder.addHeader(name, val);
            }
        }
    }

    private String getBasicAuthHeaderValue(EncryptedValue auth) {
        EncryptedValue basicAuth = auth == null ? EncryptedValue.createUnset() : auth;
        String credentials = encryptedValueService.decrypt(basicAuth);
        return credentials == null ? null : "Basic " + Base64.encode(credentials.getBytes(StandardCharsets.UTF_8));
    }

    private String getApiKeyValue(EncryptedValue apiSecret) {
        if (apiSecret == null || !apiSecret.isSet()) {
            return null;
        }
        return encryptedValueService.decrypt(apiSecret);
    }

    private void publishSystemNotificationForWhitelistFailure(String url, String eventNotificationTitle) {
        final String description = "The alert notification \"" + eventNotificationTitle +
                "\" is trying to access a URL which is not whitelisted. Please check your configuration. [url: \"" +
                url + "\"]";
        urlWhitelistNotificationService.publishWhitelistFailure(description);
    }
}
