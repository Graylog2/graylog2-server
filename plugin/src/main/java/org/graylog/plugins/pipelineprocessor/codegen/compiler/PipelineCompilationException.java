/*
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
package org.graylog.plugins.pipelineprocessor.codegen.compiler;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.tools.Diagnostic;

public class PipelineCompilationException extends RuntimeException {
    private final List<Diagnostic> errors;

    public PipelineCompilationException(List<Diagnostic> errors) {
        this.errors = errors;
    }

    @Override
    public String getMessage() {
        return errors.stream()
                .map(diagnostic -> diagnostic.getMessage(Locale.ENGLISH))
                .collect(Collectors.joining("\n"));
    }
}
