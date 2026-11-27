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
package org.graylog2.decorators;

import com.floreysoft.jmte.Engine;
import com.floreysoft.jmte.template.Template;
import com.floreysoft.jmte.template.VariableDescription;
import com.google.common.collect.ImmutableMap;
import com.google.common.escape.Escaper;
import com.google.common.net.PercentEscaper;
import com.google.inject.assistedinject.Assisted;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import okhttp3.HttpUrl;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageFactory;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.decorators.SearchResponseDecorator;
import org.graylog2.rest.models.messages.responses.ResultMessageSummary;
import org.graylog2.rest.resources.search.responses.SearchResponse;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public class LinkFieldDecorator implements SearchResponseDecorator {
    public static final String CK_LINK_FIELD = "link_field";
    public static final String CK_LINK_URL = "link_url";
    public static final String CK_LINK_TEXT = "link_text";
    // Escapes everything except RFC 3986 unreserved characters, so values are safe in both path and query.
    private static final Escaper URL_VALUE_ESCAPER = new PercentEscaper("-_.~", false);
    private final String linkField;
    @Nullable
    private final Template linkUrlTemplate;
    private final List<VariableDescription> linkUrlVariables;
    @Nullable
    private final Template linkTextTemplate;
    private final MessageFactory messageFactory;

    public interface Factory extends SearchResponseDecorator.Factory {
        @Override
        LinkFieldDecorator create(Decorator decorator);

        @Override
        LinkFieldDecorator.Config getConfig();

        @Override
        LinkFieldDecorator.Descriptor getDescriptor();
    }

    public static class Config implements SearchResponseDecorator.Config {

        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            return new ConfigurationRequest() {
                {
                    addField(new TextField(
                            CK_LINK_FIELD,
                            "Link field",
                            "message",
                            "The field that will be transformed into a hyperlink."
                    ));
                    addField(new TextField(
                            CK_LINK_URL,
                            "URL template",
                            "",
                            "Optional URL built from message fields, e.g. \"https://whois.example.com/lookup?ip=${source}\". " +
                                    "Field values are URL-encoded. The link field is only decorated if it and all referenced fields exist. " +
                                    "Leave empty to use the value of the link field as the URL.",
                            ConfigurationField.Optional.OPTIONAL
                    ));
                    addField(new TextField(
                            CK_LINK_TEXT,
                            "Link text",
                            "",
                            "Optional text displayed instead of the URL, e.g. \"Whois lookup\" or \"Ticket ${ticket_id}\". " +
                                    "Message fields can be referenced with ${field_name}. The URL is still shown as a tooltip.",
                            ConfigurationField.Optional.OPTIONAL
                    ));
                }
            };
        }
    }

    public static class Descriptor extends SearchResponseDecorator.Descriptor {
        public Descriptor() {
            super("Hyperlink String", "http://docs.graylog.org/", "Hyperlink string");
        }
    }

    @Inject
    public LinkFieldDecorator(@Assisted Decorator decorator, Engine templateEngine, MessageFactory messageFactory) {
        this.linkField = (String) requireNonNull(decorator.config().get(CK_LINK_FIELD),
                CK_LINK_FIELD + " cannot be null");
        requireNonNull(templateEngine, "templateEngine");
        this.linkUrlTemplate = templateFromConfig(decorator, CK_LINK_URL, templateEngine);
        this.linkUrlVariables = linkUrlTemplate == null ? List.of() : linkUrlTemplate.getUsedVariableDescriptions();
        this.linkTextTemplate = templateFromConfig(decorator, CK_LINK_TEXT, templateEngine);
        this.messageFactory = messageFactory;
    }

    @WithSpan
    @Override
    public SearchResponse apply(SearchResponse searchResponse) {
        final List<ResultMessageSummary> summaries = searchResponse.messages().stream()
                .map(summary -> {
                    if (summary.message().get(linkField) == null) {
                        return summary;
                    }
                    return linkUrlTemplate == null ? decorateExistingUrl(summary) : decorateWithUrlTemplate(summary);
                })
                .collect(Collectors.toList());

        return searchResponse.toBuilder().messages(summaries).build();
    }

    private ResultMessageSummary decorateExistingUrl(ResultMessageSummary summary) {
        // Non-string values (numbers, lists) can't be URLs; leave them untouched instead of failing.
        if (!(summary.message().get(linkField) instanceof String href)) {
            return summary;
        }
        if (isValidUrl(href)) {
            return withLinkField(summary, linkFieldValue(href, summary.message()));
        }
        return withLinkField(summary, href);
    }

    private ResultMessageSummary decorateWithUrlTemplate(ResultMessageSummary summary) {
        final Map<String, Object> fields = summary.message();
        if (!linkUrlVariables.stream().allMatch(variable -> fields.get(variable.name) != null)) {
            return summary;
        }
        final String href = linkUrlTemplate.transform(urlEncodedValues(fields), Locale.ENGLISH);
        if (href == null || !isValidUrl(href) || hasDotSegments(href)) {
            return summary;
        }
        return withLinkField(summary, linkFieldValue(href, fields));
    }

    private ResultMessageSummary withLinkField(ResultMessageSummary summary, Object value) {
        final Message message = messageFactory.createMessage(ImmutableMap.copyOf(summary.message()));
        message.addField(linkField, value);
        return summary.toBuilder().message(message.getFields()).build();
    }

    private Map<String, String> linkFieldValue(String href, Map<String, Object> fields) {
        final Map<String, String> decoratedField = new HashMap<>();
        decoratedField.put("type", "a");
        decoratedField.put("href", href);
        renderLinkText(fields).ifPresent(text -> decoratedField.put("text", text));
        return decoratedField;
    }

    private Map<String, Object> urlEncodedValues(Map<String, Object> fields) {
        final Map<String, Object> encoded = new HashMap<>();
        for (final VariableDescription variable : linkUrlVariables) {
            final Object value = fields.get(variable.name);
            if (value != null) {
                encoded.put(variable.name, URL_VALUE_ESCAPER.escape(String.valueOf(value)));
            }
        }
        return encoded;
    }

    @Nullable
    private static Template templateFromConfig(Decorator decorator, String key, Engine templateEngine) {
        final String template = (String) decorator.config().get(key);
        return template == null || template.isBlank() ? null : templateEngine.getTemplate(template);
    }

    private Optional<String> renderLinkText(Map<String, Object> fields) {
        if (linkTextTemplate == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(linkTextTemplate.transform(fields, Locale.ENGLISH))
                .filter(text -> !text.isBlank());
    }

    /**
     * URL-encoding field values does not encode dots, so values like ".." could form dot-segments and escape the
     * template's fixed path prefix. Browsers resolve percent-encoded dots ("%2e") the same way, so reject any
     * rendered URL whose path contains a "." or ".." segment.
     */
    private static boolean hasDotSegments(String url) {
        final String path;
        try {
            path = URI.create(url).getRawPath();
        } catch (IllegalArgumentException e) {
            return true;
        }
        if (path == null) {
            return false;
        }
        return Arrays.stream(path.split("[/\\\\]", -1))
                .map(segment -> segment.replaceAll("(?i)%2e", "."))
                .anyMatch(segment -> segment.equals(".") || segment.equals(".."));
    }

    /**
     * @param url a String URL.
     * @return true if the URL is valid and false if the URL is invalid.
     *
     * All URLS that do not start with the protocol "http" or "https" protocol scheme are considered invalid.
     * All other non-URL text strings will be considered invalid. This includes inline javascript expressions such as:
     * - javascript:...
     * - alert()
     * - or any other javascript expressions.
     */
    private static boolean isValidUrl(String url) {
        return HttpUrl.parse(url) != null;
    }
}
