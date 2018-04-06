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

import com.floreysoft.jmte.Engine;
import com.floreysoft.jmte.template.Template;
import com.floreysoft.jmte.template.VariableDescription;
import com.google.common.collect.ImmutableMap;
import com.google.inject.assistedinject.Assisted;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.BooleanField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.decorators.SearchResponseDecorator;
import org.graylog2.rest.models.messages.responses.ResultMessageSummary;
import org.graylog2.rest.resources.search.responses.SearchResponse;

import javax.inject.Inject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public class LinkFieldConverter implements SearchResponseDecorator {

    private static final String CK_LINK_FIELD = "link_field";

    private final String linkField;

    public interface Factory extends SearchResponseDecorator.Factory {
        @Override
        LinkFieldConverter create(Decorator decorator);

        @Override
        LinkFieldConverter.Config getConfig();

        @Override
        LinkFieldConverter.Descriptor getDescriptor();
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
    public LinkFieldConverter(@Assisted Decorator decorator, Engine templateEngine) {
        this.linkField = (String) requireNonNull(decorator.config().get(CK_LINK_FIELD),
                                                   CK_LINK_FIELD + " cannot be null");
    }

    @Override
    public SearchResponse apply(SearchResponse searchResponse) {
        final List<ResultMessageSummary> summaries = searchResponse.messages().stream()
                .map(summary -> {
                    final Message message = new Message(ImmutableMap.copyOf(summary.message()));
                    if (summary.message().containsKey(linkField)) {
                        final String href = (String) summary.message().get(linkField);
                        final LinkedHashMap object = new LinkedHashMap<String, String>();
                        object.put("type", "a");
                        object.put("href", href);
                        message.addField(linkField, object);
                    }
                    return summary.toBuilder().message(message.getFields()).build();
                })
                .collect(Collectors.toList());

        return searchResponse.toBuilder().messages(summaries).build();
    }
}
