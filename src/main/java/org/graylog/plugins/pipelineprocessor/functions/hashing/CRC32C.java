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
package org.graylog.plugins.pipelineprocessor.functions.hashing;

import com.google.common.hash.Hashing;

import java.nio.charset.StandardCharsets;

public class CRC32C extends SingleArgStringFunction {

    public static final String NAME = "crc32c";

    @Override
    protected String getDigest(String value) {
        return Hashing.crc32c().hashString(value, StandardCharsets.UTF_8).toString();
    }

    @Override
    protected String getName() {
        return NAME;
    }
}
