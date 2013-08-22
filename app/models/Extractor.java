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
import lib.APIException;
import lib.Api;
import models.api.requests.CreateExtractorRequest;
import models.api.responses.EmptyResponse;
import models.api.responses.system.ExtractorSummaryResponse;
import models.api.responses.system.ExtractorsResponse;
import play.Logger;

import java.io.IOException;
import java.util.*;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class Extractor {

    public enum Type {
        SUBSTRING,
        REGEX,
        SPLIT_AND_INDEX
    }

    public enum CursorStrategy {
        CUT,
        COPY
    }

    public enum ConditionType {
        NONE,
        STRING,
        REGEX
    }

    private final String id;
    private final String title;
    private final CursorStrategy cursorStrategy;
    private final Type extractorType;
    private final String sourceField;
    private final String targetField;
    private final User creatorUser;
    private final Map<String, Object> extractorConfig;
    private final List<Converter> converters;
    private final ConditionType conditionType;
    private final String conditionValue;
    private final ExtractorMetrics metrics;
    private final long exceptions;
    private final long converterExceptions;

    public Extractor(ExtractorSummaryResponse esr) {
        this(
                esr.id,
                esr.title,
                CursorStrategy.valueOf(esr.cursorStrategy.toUpperCase()),
                esr.sourceField,
                esr.targetField,
                Type.valueOf(esr.type.toUpperCase()),
                esr.extractorConfig,
                User.load(esr.creatorUserId),
                buildConverterList(esr.converters),
                ConditionType.valueOf(esr.conditionType.toUpperCase()),
                esr.conditionValue,
                new ExtractorMetrics(esr.metrics.get("total"), esr.metrics.get("converters")),
                esr.exceptions,
                esr.converterExceptions
        );
    }

    public Extractor(CursorStrategy cursorStrategy, String title, String sourceField, String targetField, Type type, User creatorUser, ConditionType conditionType, String conditionValue) {
        this(null, title, cursorStrategy, sourceField, targetField, type, new HashMap<String, Object>(), creatorUser, new ArrayList<Converter>(), conditionType, conditionValue, null, 0, 0);
    }

    public Extractor(String id,
                     String title,
                     CursorStrategy cursorStrategy,
                     String sourceField,
                     String targetField,
                     Type type,
                     Map<String, Object> extractorConfig,
                     User creatorUser,
                     List<Converter> converters,
                     ConditionType conditionType,
                     String conditionValue,
                     ExtractorMetrics metrics,
                     long exceptions,
                     long converterExceptions) {
        this.id = id;
        this.title = title;
        this.cursorStrategy = cursorStrategy;
        this.sourceField = sourceField;
        this.targetField = targetField;
        this.extractorType = type;

        this.extractorConfig = extractorConfig;
        this.converters = converters;

        this.conditionType = conditionType;
        this.conditionValue = conditionValue;

        this.exceptions = exceptions;
        this.converterExceptions = converterExceptions;

        this.creatorUser = creatorUser;
        this.metrics = metrics;
    }

    public void create(Node node, Input input) throws IOException, APIException {
        CreateExtractorRequest request = new CreateExtractorRequest();

        Map<String, Map<String, Object>> converterList = Maps.newHashMap();
        for (Converter converter : converters) {
            converterList.put(converter.getType(), converter.getConfig());
        }

        request.title = title;
        request.cutOrCopy = cursorStrategy.toString().toLowerCase();
        request.extractorType = extractorType.toString().toLowerCase();
        request.sourceField = sourceField;
        request.targetField = targetField;
        request.creatorUserId = creatorUser.getId();
        request.extractorConfig = extractorConfig;
        request.converters = converterList;
        request.conditionType = conditionType.toString().toLowerCase();
        request.conditionValue =  conditionValue;

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
        put(Type.SPLIT_AND_INDEX, "Split & Index");
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
                loadSubstringConfig(form);
                break;
            case SPLIT_AND_INDEX:
                loadSplitAndIndexConfig(form);
                break;
            default:
                throw new RuntimeException("Unknown extractor type <" + extractorType.toString() + ">");
        }
    }

    public void loadConvertersFromForm(Map<String,String[]> form) {
        for(String name : extractSelectedConverters(form)) {
            Converter.Type converterType = Converter.Type.valueOf(name.toUpperCase());
            Map<String, Object> converterConfig = extractConverterConfig(converterType, form);

            converters.add(new Converter(converterType, converterConfig));
        }
    }

    private Map<String, Object> extractConverterConfig(Converter.Type converterType, Map<String,String[]> form) {
        Map<String, Object> config = Maps.newHashMap();
        switch (converterType) {
            case DATE:
                if (formFieldSet(form, "converter_date_format")) {
                    config.put("date_format", form.get("converter_date_format")[0]);
                }
                break;
            case SPLIT_AND_COUNT:
                if (formFieldSet(form, "converter_split_and_count_by")) {
                    config.put("split_by", form.get("converter_split_and_count_by")[0]);
                }
                break;
        }

        return config;
    }

    private List<String> extractSelectedConverters(Map<String, String[]> form) {
        List<String> result = Lists.newArrayList();

        for (Map.Entry<String,String[]> f : form.entrySet()) {
            try {
                if (f.getKey().startsWith("converter_") && f.getValue()[0].equals("enabled")) {
                    result.add(f.getKey().substring("converter_".length()));
                }
            } catch(Exception e) {
                continue;
            }
        }

        return result;
    }

    private void loadRegexConfig(Map<String,String[]> form) {
        if (!formFieldSet(form, "regex_value")) {
            throw new RuntimeException("Missing extractor config: regex_value");
        }

        extractorConfig.put("regex_value", form.get("regex_value")[0]);
    }

    private void loadSubstringConfig(Map<String,String[]> form) {
        if (!formFieldSet(form, "begin_index") || !formFieldSet(form, "end_index")) {
            throw new RuntimeException("Missing extractor config: begin_index or end_index.");
        }

        extractorConfig.put("begin_index", Integer.parseInt(form.get("begin_index")[0]));
        extractorConfig.put("end_index", Integer.parseInt(form.get("end_index")[0]));
    }

    private void loadSplitAndIndexConfig(Map<String,String[]> form) {
        if (!formFieldSet(form, "split_by") || !formFieldSet(form, "index")) {
            throw new RuntimeException("Missing extractor config: split_by or index.");
        }

        extractorConfig.put("split_by", form.get("split_by")[0]);
        extractorConfig.put("index", Integer.parseInt(form.get("index")[0]));
    }

    private boolean formFieldSet(Map<String,String[]> form, String key) {
        return form.get(key) != null && form.get(key)[0] != null && !form.get(key)[0].isEmpty();
    }

    private static List<Converter> buildConverterList(List<Map<String, Object>> converters) {
        List<Converter> cl = Lists.newArrayList();

        for(Map<String, Object> converterSummary : converters) {
            cl.add(new Converter(
                    Converter.Type.valueOf(converterSummary.get("type").toString().toUpperCase()),
                    (Map<String, Object>) converterSummary.get("config")
            ));
        }

        return cl;
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

    public List<Converter> getConverters() {
        return converters;
    }

    public ConditionType getConditionType() {
        return conditionType;
    }

    public String getConditionValue() {
        return conditionValue;
    }

    public ExtractorMetrics getMetrics() {
        return metrics;
    }

    public long getExceptions() {
        return exceptions;
    }

    public long getConverterExceptions() {
        return converterExceptions;
    }

    public long getTotalExceptions() {
        return exceptions + converterExceptions;
    }
}
