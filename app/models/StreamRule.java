/*
 * Copyright 2013 TORCH UG
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
 */
package models;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StreamRule {

    public static final Map<Integer, StreamRule> RULES = new HashMap<Integer, StreamRule>() {{
        put(1, new StreamRule(1, "match exactly"));
        put(2, new StreamRule(2, "match regular expression"));
        put(3, new StreamRule(3, "greater than", "be greater than"));
        put(4, new StreamRule(4, "smaller than", "be smaller than"));
    }};

    private final int id;
    private final String name;
    private final String sentenceRepresentation;

    public StreamRule(int id, String name) {
        this.id = id;
        this.name = name;
        this.sentenceRepresentation = name;
    }

    public StreamRule(int id, String name, String sentenceRepresentation) {
        this.id = id;
        this.name = name;
        this.sentenceRepresentation = sentenceRepresentation;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSentenceRepresentation() {
        return sentenceRepresentation;
    }

}
