/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.restclient.models;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.rest.models.system.inputs.extractors.requests.CreateExtractorRequest;
import org.graylog2.rest.models.system.inputs.extractors.responses.ExtractorSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class Extractor {
    private static final Logger LOG = LoggerFactory.getLogger(Extractor.class);

    public interface Factory {
        Extractor fromResponse(ExtractorSummary es);

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
        REGEX_REPLACE("Replace with regular expression"),
        SPLIT_AND_INDEX("Split & Index"),
        COPY_INPUT("Copy Input"),
        GROK("Grok pattern"),
        JSON("JSON");
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

    private long order;

    @AssistedInject
    private Extractor(UserService userService, @Assisted ExtractorSummary es) {
        this.id = es.id();
        this.title = es.title();
        this.cursorStrategy = CursorStrategy.fromString(es.cursorStrategy());
        this.sourceField = es.sourceField();
        this.targetField = es.targetField();
        this.extractorType = Type.fromString(es.type());
        this.creatorUser = userService.load(es.creatorUserId());
        this.extractorConfig = es.extractorConfig();
        this.converters = buildConverterList(es.converters());
        this.conditionType = ConditionType.fromString(es.conditionType());
        this.conditionValue = es.conditionValue();
        this.metrics = new ExtractorMetrics(es.metrics().total(), es.metrics().converters());
        this.exceptions = es.exceptions();
        this.converterExceptions = es.converterExceptions();
        this.order = es.order();
    }

    @VisibleForTesting
    @AssistedInject
    Extractor(@Assisted CursorStrategy cursorStrategy,
              @Assisted("title") String title,
              @Assisted("sourceField") String sourceField,
              @Assisted("targetField") String targetField,
              @Assisted Type type,
              @Assisted User creatorUser,
              @Assisted ConditionType conditionType,
              @Assisted("conditionValue") String conditionValue) {
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

    public CreateExtractorRequest toCreateExtractorRequest() {
        final Map<String, Map<String, Object>> converterList = Maps.newHashMap();
        for (Converter converter : converters) {
            converterList.put(converter.getType(), converter.getConfig());
        }

        final CreateExtractorRequest request = CreateExtractorRequest.create(title, cursorStrategy.toString().toLowerCase(), sourceField, targetField,
                extractorType.toString().toLowerCase(), extractorConfig, converterList, conditionType.toString().toLowerCase(), conditionValue, order);

        return request;
    }

    public void loadConfigFromForm(Type extractorType, Map<String, String[]> form) {
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
            case REGEX_REPLACE:
                loadRegexReplaceConfig(form);
                break;
            case GROK:
                loadGrokConfig(form);
                break;
            case JSON:
                loadJsonConfig(form);
                break;
        }
    }

    public void loadConfigFromImport(Type type, Map<String, Object> extractorConfig) {
        checkNotNull(type, "Extractor type must not be null.");
        checkNotNull(extractorConfig, "Extractor configuration must not be null.");

        // we go the really easy way here.
        Map<String, String[]> looksLikeForm = Maps.newHashMapWithExpectedSize(extractorConfig.size());

        for (Map.Entry<String, Object> e : extractorConfig.entrySet()) {
            looksLikeForm.put(e.getKey(), new String[]{e.getValue().toString()});
        }

        loadConfigFromForm(type, looksLikeForm);
    }

    public void loadConvertersFromForm(Map<String, String[]> form) {
        if (form == null || form.isEmpty()) {
            return;
        }

        for (String name : extractSelectedConverters(form)) {
            Converter.Type converterType = Converter.Type.valueOf(name.toUpperCase());
            Map<String, Object> converterConfig = extractConverterConfig(converterType, form);

            converters.add(new Converter(converterType, converterConfig));
        }
    }

    @SuppressWarnings("unchecked")
    public void loadConvertersFromImport(List<Map<String, Object>> imports) {
        if (imports == null || imports.isEmpty()) {
            return;
        }

        for (Map<String, Object> imp : imports) {
            final Converter.Type type = Converter.Type.valueOf(((String) imp.get("type")).toUpperCase());
            converters.add(new Converter(type, (Map<String, Object>) imp.get("config")));
        }
    }

    private Map<String, Object> extractConverterConfig(Converter.Type converterType, Map<String, String[]> form) {
        Map<String, Object> config = Maps.newHashMap();
        switch (converterType) {
            case DATE:
                if (formFieldSet(form, "converter_date_format")) {
                    config.put("date_format", form.get("converter_date_format")[0]);
                }
                if (formFieldSet(form, "converter_date_time_zone")) {
                    config.put("time_zone", form.get("converter_date_time_zone")[0]);
                }
                break;
            case FLEXDATE:
                if (formFieldSet(form, "converter_flexdate_time_zone")) {
                    config.put("time_zone", form.get("converter_flexdate_time_zone")[0]);
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
                                case 'n':
                                    c = '\n';
                                    break;
                                case 't':
                                    c = '\t';
                                    break;
                                case '\\':
                                    c = '\\';
                                    break;
                                default:
                                    LOG.error("Unknown escape sequence {}, cannot create CSV converter", csv_separator);
                            }
                        } else {
                            LOG.error("Illegal escape sequence '{}', cannot create CSV converter", csv_separator);
                        }
                    } else {
                        LOG.error("No valid separator, cannot create CSV converter.");
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
        if (form == null || form.isEmpty()) {
            return Collections.emptyList();
        }

        final List<String> result = Lists.newArrayListWithCapacity(form.size());
        for (Map.Entry<String, String[]> f : form.entrySet()) {
            try {
                if (f.getKey().startsWith("converter_") && f.getValue()[0].equals("enabled")) {
                    result.add(f.getKey().substring("converter_".length()));
                }
            } catch (Exception e) {
                // Ignore
            }
        }

        return result;
    }

    private void loadRegexConfig(Map<String, String[]> form) {
        if (!formFieldSet(form, "regex_value")) {
            throw new RuntimeException("Missing extractor config: regex_value");
        }

        extractorConfig.put("regex_value", form.get("regex_value")[0]);
    }

    private void loadSubstringConfig(Map<String, String[]> form) {
        if (!formFieldSet(form, "begin_index") || !formFieldSet(form, "end_index")) {
            throw new RuntimeException("Missing extractor config: begin_index or end_index.");
        }

        extractorConfig.put("begin_index", Integer.parseInt(form.get("begin_index")[0]));
        extractorConfig.put("end_index", Integer.parseInt(form.get("end_index")[0]));
    }

    private void loadSplitAndIndexConfig(Map<String, String[]> form) {
        if (!formFieldSet(form, "split_by") || !formFieldSet(form, "index")) {
            throw new RuntimeException("Missing extractor config: split_by or index.");
        }

        extractorConfig.put("split_by", form.get("split_by")[0]);
        extractorConfig.put("index", Integer.parseInt(form.get("index")[0]));
    }

    private void loadRegexReplaceConfig(Map<String, String[]> form) {
        if (!formFieldSet(form, "regex")) {
            throw new RuntimeException("Missing extractor config: regex.");
        }

        extractorConfig.put("regex", form.get("regex")[0]);
        extractorConfig.put("replace_all", form.containsKey("replace_all"));

        if (formFieldSet(form, "replacement")) {
            extractorConfig.put("replacement", form.get("replacement")[0]);
        }
    }

    private void loadGrokConfig(Map<String, String[]> form) {
        if (!formFieldSet(form, "grok_pattern")) {
            throw new RuntimeException("Missing extractor config: grok_pattern");
        }

        extractorConfig.put("grok_pattern", form.get("grok_pattern")[0]);
    }

    private void loadJsonConfig(Map<String, String[]> form) {
        extractorConfig.put("flatten", form.containsKey("flatten"));
        extractorConfig.put("list_separator", form.get("list_separator")[0]);
        extractorConfig.put("key_separator", form.get("key_separator")[0]);
        extractorConfig.put("kv_separator", form.get("kv_separator")[0]);
    }

    private boolean formFieldSet(Map<String, String[]> form, String key) {
        return form.get(key) != null && form.get(key)[0] != null && !form.get(key)[0].isEmpty();
    }

    @SuppressWarnings("unchecked")
    private List<Converter> buildConverterList(List<Map<String, Object>> converters) {
        if (converters == null || converters.isEmpty()) {
            return Collections.emptyList();
        }

        final List<Converter> cl = Lists.newArrayListWithCapacity(converters.size());
        for (Map<String, Object> converterSummary : converters) {
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

    public long getOrder() {
        return order;
    }

    public void setOrder(long order) {
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
