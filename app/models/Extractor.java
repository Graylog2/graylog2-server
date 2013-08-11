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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lib.APIException;
import lib.Api;
import models.api.requests.CreateExtractorRequest;
import models.api.responses.EmptyResponse;
import models.api.responses.system.ExtractorSummaryResponse;
import models.api.responses.system.ExtractorsResponse;
import org.joda.time.DateTime;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
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

    private final String id;
    private final String title;
    private final CursorStrategy cursorStrategy;
    private final Type extractorType;
    private final String sourceField;
    private final String targetField;
    private final User creatorUser;
    private final Map<String, Object> extractorConfig;
    private final Set<Converter> converters;

    public Extractor(ExtractorSummaryResponse esr) {
        this(
                esr.id,
                esr.title,
                CursorStrategy.valueOf(esr.cursorStrategy.toUpperCase()),
                esr.sourceField,
                esr.targetField,
                Type.valueOf(esr.type.toUpperCase()),
                esr.extractorConfig,
                User.load(esr.creatorUserId)
        );
    }

    public Extractor(CursorStrategy cursorStrategy, String title, String sourceField, String targetField, Type type, User creatorUser) {
        this(null, title, cursorStrategy, sourceField, targetField, type, new HashMap<String, Object>(), creatorUser);
    }

    public Extractor(String id, String title, CursorStrategy cursorStrategy, String sourceField, String targetField, Type type, Map<String, Object> extractorConfig, User creatorUser) {
        this.id = id;
        this.title = title;
        this.cursorStrategy = cursorStrategy;
        this.sourceField = sourceField;
        this.targetField = targetField;
        this.extractorType = type;

        this.extractorConfig = extractorConfig;
        this.converters = Sets.newHashSet();

        this.creatorUser = creatorUser;
    }

    public void create(Node node, Input input) throws IOException, APIException {
        CreateExtractorRequest request = new CreateExtractorRequest();

        request.title = title;
        request.cutOrCopy = cursorStrategy.toString().toLowerCase();
        request.extractorType = extractorType.toString().toLowerCase();
        request.sourceField = sourceField;
        request.targetField = targetField;
        request.creatorUserId = creatorUser.getId();
        request.extractorConfig = extractorConfig;

        Api.post(node, "system/inputs/" + input.getId() + "/extractors", request, 201, EmptyResponse.class);
    }

    public static List<Extractor> all(Node node, Input input) throws IOException, APIException {
        List<Extractor> extractors = Lists.newArrayList();

        for(ExtractorSummaryResponse ex : Api.get(node, "system/inputs/" + input.getId() + "/extractors", ExtractorsResponse.class).extractors) {
            extractors.add(new Extractor(ex));
        }

        return extractors;
    }

    public static void delete(Node node, Input input, String extractorId) throws IOException, APIException {
        Api.delete(node, "system/inputs/" + input.getId() + "/extractors/" + extractorId, 204, EmptyResponse.class);
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

    public User getCreatorUser() {
        return creatorUser;
    }

    public Type getType() {
        return extractorType;
    }

    public String getId() {
        return id;
    }

    public Map<String, Object> getExtractorConfig() {
        return extractorConfig;
    }

    public String getTitle() {
        return title;
    }

    public String getTargetField() {
        return targetField;
    }

    public String getSourceField() {
        return sourceField;
    }

    public CursorStrategy getCursorStrategy() {
        return cursorStrategy;
    }
}
