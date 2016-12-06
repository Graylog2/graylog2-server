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

import java.net.URI;

import javax.tools.SimpleJavaFileObject;
import javax.validation.constraints.NotNull;

import static javax.tools.JavaFileObject.Kind.SOURCE;

public class JavaSourceFromString extends SimpleJavaFileObject {

    private final String sourceCode;

    public JavaSourceFromString(@NotNull String name, String sourceCode) {
        super(URI.create("string:///" + name.replace('.', '/') + SOURCE.extension), SOURCE);
        this.sourceCode = sourceCode;
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        return sourceCode;
    }
}
