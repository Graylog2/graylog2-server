/**
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
package org.graylog2.restclient.models;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.models.api.requests.CreateExtractorRequest;
import org.graylog2.restclient.models.api.responses.system.ExtractorSummaryResponse;
import org.graylog2.restroutes.generated.routes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.mvc.Http;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class Extractor {
    private static final Logger log = LoggerFactory.getLogger(Extractor.class);

    public interface Factory {
        Extractor fromResponse(ExtractorSummaryResponse esr);
        Extractor forCreate(CursorStrategy cursorStrategy,
                            @Assisted("title") String title,
                            @Assisted("sourceField") String sourceField,
                            @Assisted("targetField") String targetField,
                            Type type,
                            User creatorUser,
                            ConditionType conditionType,
                            @Assisted("conditionValue") String conditionValue);
    }

    public enum Type {
        SUBSTRING("Substring"),
        REGEX("Regular expression"),
        SPLIT_AND_INDEX("Split & Index"),
        COPY_INPUT("Copy Input");
        private final String description;

        Type(String description) {
            this.description = description;
        }

        public String toHumanReadable() {
            return description;
        }

        public static Type fromString(String name) {
            return valueOf(name.toUpperCase());
        }
    }

    public enum CursorStrategy {
        CUT,
        COPY;

        public static CursorStrategy fromString(String name) {
            return valueOf(name.toUpperCase());
        }

    }
    public enum ConditionType {
        NONE,
        STRING,
        REGEX;

        public static ConditionType fromString(String name) {
            return valueOf(name.toUpperCase());
        }

    }
    private final ApiClient api;
    private final UserService userService;

    private String id;
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

    private int order;

    @AssistedInject
    private Extractor(ApiClient api, UserService userService, @Assisted ExtractorSummaryResponse esr) {
        this.api = api;
        this.userService = userService;

        this.id = esr.id;
        this.title = esr.title;
        this.cursorStrategy = CursorStrategy.fromString(esr.cursorStrategy);
        this.sourceField = esr.sourceField;
        this.targetField = esr.targetField;
        this.extractorType = Type.fromString(esr.type);
        this.creatorUser = userService.load(esr.creatorUserId);
        this.extractorConfig = esr.extractorConfig;
        this.converters = buildConverterList(esr.converters);
        this.conditionType = ConditionType.fromString(esr.conditionType);
        this.conditionValue = esr.conditionValue;
        this.metrics = new ExtractorMetrics(esr.metrics.get("total"), esr.metrics.get("converters"));
        this.exceptions = esr.exceptions;
        this.converterExceptions = esr.converterExceptions;
        this.order = esr.order;
    }

    @AssistedInject
    private Extractor(ApiClient api,
                     UserService userService,
                     @Assisted CursorStrategy cursorStrategy,
                     @Assisted("title") String title,
                     @Assisted("sourceField") String sourceField,
                     @Assisted("targetField") String targetField,
                     @Assisted Type type,
                     @Assisted User creatorUser,
                     @Assisted ConditionType conditionType,
                     @Assisted("conditionValue") String conditionValue) {
        this.api = api;
        this.userService = userService;

        this.id = null;
        this.title = title;
        this.cursorStrategy = cursorStrategy;
        this.sourceField = sourceField;
        this.targetField = targetField;
        this.extractorType = type;
        this.extractorConfig = Maps.newHashMap();
        this.creatorUser = creatorUser;
        this.converters = Lists.newArrayList();
        this.conditionType = conditionType;
        this.conditionValue = conditionValue;
        this.metrics = null;
        this.exceptions = 0;
        this.converterExceptions = 0;
        this.order = 0;
    }

    public Extractor create(Node node, Input input) throws IOException, APIException {
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
        request.extractorConfig = extractorConfig;
        request.converters = converterList;
        request.conditionType = conditionType.toString().toLowerCase();
        request.conditionValue = conditionValue;
        request.order = order;

        final Map response = api.path(routes.ExtractorsResource().create(input.getId()), Map.class)
                .node(node)
                .expect(Http.Status.CREATED)
                .body(request)
                .execute();
        this.id = response.get("extractor_id").toString();
        return this;
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
        }
    }

    public void loadConfigFromImport(Type type, Map<String, Object> extractorConfig) {
        // we go the really easy way here.
        Map<String, String[]> looksLikeForm = Maps.newHashMap();

        for (Map.Entry<String, Object> e : extractorConfig.entrySet()) {
            looksLikeForm.put(e.getKey(), new String[]{ e.getValue().toString() });
        }

        loadConfigFromForm(type, looksLikeForm);
    }

    public void loadConvertersFromForm(Map<String,String[]> form) {
        for(String name : extractSelectedConverters(form)) {
            Converter.Type converterType = Converter.Type.valueOf(name.toUpperCase());
            Map<String, Object> converterConfig = extractConverterConfig(converterType, form);

            converters.add(new Converter(converterType, converterConfig));
        }
    }

    public void loadConvertersFromImport(List<Map<String, Object>> imports) {
        for (Map<String, Object> imp : imports) {
            Converter converter = new Converter(
                    Converter.Type.valueOf(((String) imp.get("type")).toUpperCase()),
                    (Map<String, Object>) imp.get("config")
            );

            converters.add(converter);
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
            case CSV:
                if (formFieldSet(form, "csv_column_header")) {
                    config.put("column_header", form.get("csv_column_header")[0]);
                }
                if (formFieldSet(form, "csv_separator")) {
                    String csv_separator = form.get("csv_separator")[0];
                    char c = ',';
                    if (csv_separator.length() == 1) {
                        c = csv_separator.charAt(0);
                    } else if (csv_separator.length() == 2) {
                        if (csv_separator.charAt(0) == '\\') {
                            switch (csv_separator.charAt(1)) {
                                case 'n': c = '\n'; break;
                                case 't': c = '\t'; break;
                                case '\\': c = '\\'; break;
                                default: log.error("Unknown escape sequence {}, cannot create CSV converter", csv_separator);
                            }
                        } else {
                            log.error("Illegal escape sequence '{}', cannot create CSV converter", csv_separator);
                        }
                    } else {
                        log.error("No valid separator, cannot create CSV converter.");
                    }
                    config.put("separator", c);
                }
                if (formFieldSet(form, "csv_quote_char")) {
                    config.put("quote_char", form.get("csv_quote_char")[0]);
                }
                if (formFieldSet(form, "csv_escape_char")) {
                    config.put("escape_char", form.get("csv_escape_char")[0]);
                }
                if (formFieldSet(form, "csv_strict_quotes")) {
                    config.put("strict_quotes", Boolean.valueOf(form.get("csv_strict_quotes")[0]));
                }
                if (formFieldSet(form, "csv_trim_leading_whitespace")) {
                    config.put("trim_leading_whitespace", Boolean.valueOf(form.get("csv_trim_leading_whitespace")[0]));
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

    private List<Converter> buildConverterList(List<Map<String, Object>> converters) {
        List<Converter> cl = Lists.newArrayList();

        for(Map<String, Object> converterSummary : converters) {
            cl.add(new Converter(
                    Converter.Type.fromString(converterSummary.get("type").toString()),
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

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public Map<String, Object> export() {
        Map<String, Object> export = Maps.newTreeMap();

        List<Map<String, Object>> converterConfigList = Lists.newArrayList();
        for (Converter converter : converters) {
            Map<String, Object> converterExport = Maps.newHashMap();

            converterExport.put("config", converter.getConfig());
            converterExport.put("type", converter.getType());

            converterConfigList.add(converterExport);
        }

        export.put("title", title);
        export.put("order", order);
        export.put("source_field", sourceField);
        export.put("target_field", targetField);
        export.put("cursor_strategy", cursorStrategy.toString().toLowerCase());
        export.put("condition_type", conditionType.toString().toLowerCase());
        export.put("condition_value", conditionValue);
        export.put("extractor_type", extractorType.toString().toLowerCase());
        export.put("extractor_config", extractorConfig);
        export.put("converters", converterConfigList);

        return export;
    }

}
