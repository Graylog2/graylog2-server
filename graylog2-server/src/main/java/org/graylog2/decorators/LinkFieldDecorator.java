/**
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
package org.graylog2.decorators;

import com.google.common.collect.ImmutableMap;
import com.google.inject.assistedinject.Assisted;
import org.apache.commons.validator.routines.UrlValidator;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.decorators.SearchResponseDecorator;
import org.graylog2.rest.models.messages.responses.ResultMessageSummary;
import org.graylog2.rest.resources.search.responses.SearchResponse;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public class LinkFieldDecorator implements SearchResponseDecorator {

    public static final String CK_LINK_FIELD = "link_field";

    private final String linkField;

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
    public LinkFieldDecorator(@Assisted Decorator decorator) {
        this.linkField = (String) requireNonNull(decorator.config().get(CK_LINK_FIELD),
                                                   CK_LINK_FIELD + " cannot be null");
    }

    @Override
    public SearchResponse apply(SearchResponse searchResponse) {
        final List<ResultMessageSummary> summaries = searchResponse.messages().stream()
                .map(summary -> {
                    if (!summary.message().containsKey(linkField)) {
                      return summary;
                    }
                    final Message message = new Message(ImmutableMap.copyOf(summary.message()));
                    final String href = (String) summary.message().get(linkField);
                    if (isValidUrl(href)) {
                        final Map<String, String> decoratedField = new HashMap<>();
                        decoratedField.put("type", "a");
                        decoratedField.put("href", href);
                        message.addField(linkField, decoratedField);
                    } else {
                        message.addField(linkField, href);
                    }
                    return summary.toBuilder().message(message.getFields()).build();
                })
                .collect(Collectors.toList());

        return searchResponse.toBuilder().messages(summaries).build();
    }

    /**
     * @param url a String URL.
     * @return true if the URL is valid and false if the URL is invalid.
     *
     * All URLS that do not start with the protocol "http" or "https" protocol scheme are considered invalid.
     * All other non-URL text strings will be considered invalid. This includes inline javascript expressions such as:
     *  - javascript:...
     *  - alert()
     *  - or any other javascript expressions.
     */
    private boolean isValidUrl(String url) {
        String[] schemes = {"http", "https"};
        // UrlValidator.ALLOW_LOCAL_URLS allows local links to be permitted such as http://my-local-server
        // Some users may reference such local URLs, and there should be no issue with doing so.
        UrlValidator urlValidator = new UrlValidator(schemes, UrlValidator.ALLOW_LOCAL_URLS + UrlValidator.ALLOW_2_SLASHES);
        return urlValidator.isValid(url);
    }
}
