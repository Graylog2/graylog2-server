/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package models;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class Extractor {

    public enum Type {
        SUBSTRING,
        REGEX,
        START_END_CHAR
    }

    private static final Map<Type, String> TYPE_MAPPING = new HashMap<Type, String>() {{
        put(Type.SUBSTRING, "Substring");
        put(Type.REGEX, "Regular expression");
        put(Type.START_END_CHAR, "Start/End character");
    }};

    public static String typeToHuman(Type type) {
        return TYPE_MAPPING.get(type);
    }

}
