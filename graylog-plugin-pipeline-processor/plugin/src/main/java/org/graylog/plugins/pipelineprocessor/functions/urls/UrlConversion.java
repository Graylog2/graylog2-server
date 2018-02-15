/**
 * This file is part of Graylog Pipeline Processor.
 *
 * Graylog Pipeline Processor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog Pipeline Processor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog Pipeline Processor.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.plugins.pipelineprocessor.functions.urls;

import com.google.common.base.Throwables;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.functions.AbstractFunction;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;

import java.net.MalformedURLException;
import java.util.Optional;

public class UrlConversion extends AbstractFunction<URL> {

    public static final String NAME = "to_url";

    private final ParameterDescriptor<Object, Object> urlParam = ParameterDescriptor.object("url").description("Value to convert").build();
    private final ParameterDescriptor<String, String> defaultParam = ParameterDescriptor.string("default").optional().description("Used when 'url' is null or malformed").build();

    @Override
    public URL evaluate(FunctionArgs args, EvaluationContext context) {
        final String urlString = String.valueOf(urlParam.required(args, context));
        try {
            return new URL(urlString);
        } catch (IllegalArgumentException | MalformedURLException e) {
            log.debug("Unable to parse URL for string {}", urlString, e);

            final Optional<String> defaultUrl = defaultParam.optional(args, context);
            if (!defaultUrl.isPresent()) {
                return null;
            }
            try {
                return new URL(defaultUrl.get());
            } catch (IllegalArgumentException | MalformedURLException e1) {
                log.warn("Parameter `default` for to_url() is not a valid URL: {}", defaultUrl.get());
                throw Throwables.propagate(e1);
            }
        }
    }

    @Override
    public FunctionDescriptor<URL> descriptor() {
        return FunctionDescriptor.<URL>builder()
                .name(NAME)
                .returnType(URL.class)
                .params(urlParam,
                        defaultParam)
                .description("Converts a value to a valid URL using its string representation")
                .build();
    }
}
