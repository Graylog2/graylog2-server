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

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lib.APIException;
import lib.Api;
import models.api.requests.CreateExtractorRequest;
import models.api.requests.InputLaunchRequest;
import models.api.responses.EmptyResponse;
import play.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class Extractor {

    public enum Type {
        SUBSTRING,
        REGEX,
        START_END_CHAR
    }

    public enum CursorStrategy {
        CUT,
        COPY
    }

    private final CursorStrategy cursorStrategy;
    private final Type extractorType;
    private final String sourceField;
    private final String targetField;
    private final User creatorUser;
    private final Map<String, Object> extractorConfig;
    private final Set<Converter> converters;

    private final Input input;

    public Extractor(CursorStrategy cursorStrategy, String sourceField, String targetField, Type type, User creatorUser, Input input) {
        this.cursorStrategy = cursorStrategy;
        this.sourceField = sourceField;
        this.targetField = targetField;
        this.extractorType = type;

        this.extractorConfig = Maps.newHashMap();
        this.converters = Sets.newHashSet();

        this.creatorUser = creatorUser;
        this.input = input;
    }

    public void create(Node node) throws IOException, APIException {
        CreateExtractorRequest request = new CreateExtractorRequest();

        request.cutOrCopy = cursorStrategy.toString().toLowerCase();
        request.extractorType = extractorType.toString().toLowerCase();
        request.sourceField = sourceField;
        request.targetField = targetField;
        request.creatorUserId = creatorUser.getId();
        request.extractorConfig = extractorConfig;

        Api.post(node, "system/inputs/" + input.getId() + "/extractors", request, 201, EmptyResponse.class);
    }

    private static final Map<Type, String> TYPE_MAPPING = new HashMap<Type, String>() {{
        put(Type.SUBSTRING, "Substring");
        put(Type.REGEX, "Regular expression");
        put(Type.START_END_CHAR, "Start/End character");
    }};

    public static String typeToHuman(Type type) {
        return TYPE_MAPPING.get(type);
    }

    public void loadConfigFromForm(Type extractorType, Map<String,String[]> form) {
        switch (extractorType) {
            case REGEX:
                loadRegexConfig(form);
                break;
            case SUBSTRING:
                break;
            case START_END_CHAR:
                break;
            default:
                throw new RuntimeException("Unknown extractor type <" + extractorType.toString() + ">");
        }
    }

    public void loadConvertersFromForm(Map<String,String[]> form) {
    }

    private void loadRegexConfig(Map<String,String[]> form) {
        if (form.get("regex_value") == null || form.get("regex_value")[0] == null || form.get("regex_value")[0].isEmpty()) {
            throw new RuntimeException("Missing extractor config: regex_value");
        }

        extractorConfig.put("regex_value", form.get("regex_value")[0]);
    }

}
