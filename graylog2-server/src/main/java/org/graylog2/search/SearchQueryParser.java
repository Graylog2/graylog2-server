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
package org.graylog2.search;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.MoreObjects;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.tuple.Pair;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

/**
 * Parses a simple query language for use in list filtering of data sitting in MongoDB.
 * <p>
 *     The syntax is as follows:
 *     <dl>
 *         <dt><code>*</code></dt>
 *         <dd>The <code>*</code> or empty string is the match-all operator, it should not be combined with others</dd>
 *         <dt><code>sometext</code></dt>
 *         <dd>Searches for <code>sometext</code> in the default field</dd>
 *         <dt><code>field:sometext</code></dt>
 *         <dd>Searches in the given field named <code>field</code></dd>
 *         <dt><code>-field:sometext</code></dt>
 *         <dd>Searches in the given field <code>field</code> but negates the search condition</dd>
 *         <dt><code>field:some,text</code></dt>
 *         <dd>Adds <code>field</code> twice, as if <code>field:some field:text</code> was given</dd>
 *     </dl>
 *     <p>
 *         Whitespace is used to separate words, if whitespace is important you can quote the string with single or double quotes: <code>field:'hello world'</code>.
 *     </p>
 * </p>
 * <p>
 *     The class needs two parameters, the default field name to use for query text and the set of allowed fields to search in. The default field name must be the database name.
 *     <p>
 *     If the external field names should be different to what is used by the database, you can optionally pass the allowed fields as a {@link Map}. The parser will then try to replace each field name with the corresponding value in the map.
 *     <p>
 *     If the key has no mapping the default field name is used instead and the field name is recorded as being invalid. This lets the caller execute the query but still allows for error checking. It is the callers responsibility to decide which behavior is applicable.
 * </p>
 * Instances of this class are safe to use from multiple threads.
 */
public class SearchQueryParser {
    private static final Splitter FIELD_VALUE_SPLITTER = Splitter.on(":").limit(2).omitEmptyStrings().trimResults();
    private static final Splitter VALUE_SPLITTER = Splitter.on(",").omitEmptyStrings().trimResults();

    // This needs to be updated if more operators are added
    private static final Pattern QUERY_SPLITTER_PATTERN = Pattern.compile("(\\S+:(=|=~|<|<=|>|>=)?'(?:[^'\\\\]|\\\\.)*')|(\\S+:(=|=~|<|<=|>|>=)?\"(?:[^\"\\\\]|\\\\.)*\")|\\S+|\\S+:(=|=~|<|<=|>|>=)?\\S+");
    private static final String INVALID_ENTRY_MESSAGE = "Chunk [%s] is not a valid entry";
    private static final String QUOTE_REPLACE_REGEX = "^[\"']|[\"']$";
    public static final SearchQueryOperator DEFAULT_OPERATOR = SearchQueryOperators.REGEXP;

    // We parse all date strings in UTC because we store and show all dates in UTC as well.
    private static final List<DateTimeFormatter> DATE_TIME_FORMATTERS = ImmutableList.of(
            DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss").withZoneUTC(),
            DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS").withZoneUTC(),
            ISODateTimeFormat.dateTimeParser().withOffsetParsed().withZoneUTC()
    );

    @Nonnull
    private final Map<String, SearchQueryField> dbFieldMapping;

    @Nonnull
    private final String defaultField;
    private final SearchQueryField defaultFieldKey;

    /**
     * Constructs a new parser without field mapping.
     *
     * @param defaultField the name of the default field
     * @param allowedFields the names of allowed fields in the query
     */
    public SearchQueryParser(@Nonnull String defaultField, @Nonnull Set<String> allowedFields) {
        this(defaultField, allowedFields.stream()
                .collect(Collectors.toMap(Function.identity(), SearchQueryField::create)));
    }

    /**
     * Constructs a new parser with explicit field mapping.
     *
     * @param defaultField the name of the default field (already mapped)
     * @param allowedFieldsWithMapping the map of field mappings, keys are the allowed fields, values are the replacements
     */
    public SearchQueryParser(@Nonnull String defaultField,
                             @Nonnull Map<String, SearchQueryField> allowedFieldsWithMapping) {
        this.defaultField = requireNonNull(defaultField);
        this.defaultFieldKey = SearchQueryField.create(defaultField, SearchQueryField.Type.STRING);
        this.dbFieldMapping = allowedFieldsWithMapping;
    }

    @VisibleForTesting
    Matcher querySplitterMatcher(String queryString) {
        return QUERY_SPLITTER_PATTERN.matcher(queryString);
    }

    public SearchQuery parse(String queryString) {
        if (Strings.isNullOrEmpty(queryString) || "*".equals(queryString)) {
            return new SearchQuery(queryString);
        }

        final Matcher matcher = querySplitterMatcher(requireNonNull(queryString).trim());
        final ImmutableMultimap.Builder<String, FieldValue> builder = ImmutableMultimap.builder();
        final ImmutableSet.Builder<String> disallowedKeys = ImmutableSet.builder();

        while (matcher.find()) {
            final String entry = matcher.group();

            if (!entry.contains(":")) {
                builder.put(defaultField, createFieldValue(defaultFieldKey, entry, false));
                continue;
            }

            final Iterator<String> entryFields = FIELD_VALUE_SPLITTER.splitToList(entry).iterator();

            checkArgument(entryFields.hasNext(), INVALID_ENTRY_MESSAGE, entry);
            final String key = entryFields.next();

            // Skip if there are no valid k/v pairs. (i.e. "action:")
            if (!entryFields.hasNext()) {
                continue;
            }

            final boolean negate = key.startsWith("-");
            final String cleanKey = key.replaceFirst("^-", "");
            final String value = entryFields.next();
            VALUE_SPLITTER.splitToList(value).forEach(v -> {
                if (!dbFieldMapping.containsKey(cleanKey)) {
                    disallowedKeys.add(cleanKey);
                }
                final SearchQueryField translatedKey = dbFieldMapping.get(cleanKey);
                if (translatedKey != null) {
                    builder.put(translatedKey.getDbField(), createFieldValue(translatedKey, v, negate));
                } else {
                    builder.put(defaultField, createFieldValue(defaultFieldKey, v, negate));
                }
            });

            checkArgument(!entryFields.hasNext(), INVALID_ENTRY_MESSAGE, entry);
        }

        return new SearchQuery(queryString, builder.build(), disallowedKeys.build());
    }

    /* YOLO operator parser
     *
     * This tries to extract the operator by looking at the first or the first two characters of the value string to
     * find a supported operator.
     * If an operator has been found, it will be removed from the value string and the value string will also be
     * trimmed to remove leading and trailing whitespace.
     * If no operator can be found, the unmodified value string will be returned along the given default operator.
     */
    @VisibleForTesting
    Pair<String, SearchQueryOperator> extractOperator(String value, SearchQueryOperator defaultOperator) {
        if (value.length() >= 3) {
            final String substring2 = value.substring(0, 2);

            switch (substring2) {
                case ">=":
                    return Pair.of(value.substring(2).trim(), SearchQueryOperators.GREATER_EQUALS);
                case "<=":
                    return Pair.of(value.substring(2).trim(), SearchQueryOperators.LESS_EQUALS);
                case "=~":
                    return Pair.of(value.substring(2).trim(), SearchQueryOperators.REGEXP);
            }
        }

        if (value.length() >= 2) {
            final String substring1 = value.substring(0, 1);
            switch (substring1) {
                case ">":
                    return Pair.of(value.substring(1).trim(), SearchQueryOperators.GREATER);
                case "<":
                    return Pair.of(value.substring(1).trim(), SearchQueryOperators.LESS);
                case "=":
                    return Pair.of(value.substring(1).trim(), SearchQueryOperators.EQUALS);
            }
        }

        return Pair.of(value, defaultOperator);
    }

    private DateTime parseDate(String value) {
        for (DateTimeFormatter formatter : DATE_TIME_FORMATTERS) {
            try {
                return formatter.parseDateTime(value);
            } catch (IllegalArgumentException e) {
                // Try next one
            }
        }

        // It's probably not a date...
        throw new IllegalArgumentException("Unable to parse date: " + value);
    }

    /* Create a FieldValue for the query field from the string value.
     * We try to convert the value types according to the data type of the query field.
     */
    @VisibleForTesting
    FieldValue createFieldValue(SearchQueryField field, String quotedStringValue, boolean negate) {
        // Make sure there are no quotes in the value (e.g. `"foo"' --> `foo')
        final String value = quotedStringValue.replaceAll(QUOTE_REPLACE_REGEX, "");
        final Pair<String, SearchQueryOperator> pair = extractOperator(value, DEFAULT_OPERATOR);

        switch (field.getFieldType()) {
            case DATE:
                return new FieldValue(parseDate(pair.getLeft()), pair.getRight(), negate);
            case STRING:
                return new FieldValue(pair.getLeft(), pair.getRight(), negate);
            default:
                throw new IllegalArgumentException("Unhandled field type: " + field.getFieldType().toString());
        }
    }

    public static class FieldValue {
        private final Object value;
        private final SearchQueryOperator operator;
        private final boolean negate;

        public FieldValue(final Object value, final boolean negate) {
            this(value, DEFAULT_OPERATOR, negate);
        }

        public FieldValue(final Object value, final SearchQueryOperator operator, final boolean negate) {
            this.value = requireNonNull(value);
            this.operator = operator;
            this.negate = negate;
        }

        public Object getValue() {
            return value;
        }

        public SearchQueryOperator getOperator() {
            return operator;
        }

        public boolean isNegate() {
            return negate;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof FieldValue)) return false;
            FieldValue that = (FieldValue) o;
            return isNegate() == that.isNegate() &&
                    Objects.equals(getOperator(), that.getOperator()) &&
                    Objects.equals(getValue(), that.getValue());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getValue(), isNegate());
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("value", value)
                    .add("operator", operator.getClass().getCanonicalName())
                    .add("negate", negate)
                    .toString();
        }
    }
}
