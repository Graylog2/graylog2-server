/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.indexer.messages;

import com.codahale.metrics.Meter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog2.plugin.utilities.date.DateTimeConverter;
import org.joda.time.DateTime;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Recovers messages that the indexer rejected with a {@code mapper_parsing_exception} because a date value was
 * written into a numerically mapped field.
 * <p>
 * This happens when an index mapped the field from a numeric timestamp but the message now carries a date string
 * (or vice versa). Since a field's type cannot be changed in an existing index, the only way to get the message
 * indexed is to write the value in the numeric shape the existing mapping expects. Which shape that is depends on
 * the mapped type, see {@link EpochFormat}.
 * <p>
 * The coercion is deliberately narrow: it only applies when the indexer named a field of a numeric type, and only
 * when that field's value can be understood as a date. Everything else is left untouched so the message takes the
 * regular failure-handling path.
 */
public final class MappingErrorCoercion {

    /**
     * Matches the indexer's message, e.g. {@code failed to parse field [took_ms] of type [long] in document with id
     * '01K...'}. The reason is embedded in a larger error string, so we search rather than match.
     */
    private static final Pattern FAILED_FIELD = Pattern.compile("failed to parse field \\[(.+?)] of type \\[(.+?)]");

    /**
     * Types whose values were written as epoch milliseconds, which is how Jackson writes Joda {@link DateTime} and
     * {@link java.util.Date} when it writes dates as timestamps. Dynamic mapping produces {@code long} for those.
     * <p>
     * {@code byte}, {@code short} and {@code integer} are excluded: epoch milliseconds do not fit into them, so a
     * retry could not succeed.
     */
    private static final Set<String> INTEGRAL_TYPES = Set.of("long", "unsigned_long");

    /**
     * Types whose values were written as epoch seconds with a fractional part, which is how Jackson writes
     * {@code java.time} values such as {@link java.time.Instant}. Grok's {@code ;date;} conversion produces an
     * {@code Instant}, so those fields end up dynamically mapped as {@code float}.
     * <p>
     * {@code half_float} is excluded: it tops out at 65504 and cannot hold an epoch value at all.
     */
    private static final Set<String> FRACTIONAL_TYPES = Set.of("float", "double", "scaled_float");

    private MappingErrorCoercion() {
    }

    /**
     * Returns a view of the given message that writes the field named in the error in the numeric shape its mapping
     * expects, or an empty {@link Optional} if the error does not describe a numerically mapped field that is not
     * already being rewritten.
     * <p>
     * The indexer only ever reports the first field it could not parse, so a message with several conflicting fields
     * needs one call per field. Passing an already coerced message adds the new field to it, which is why a field
     * that is already being rewritten yields an empty result: it means the rewrite did not help, and retrying it
     * again would not either.
     * <p>
     * Whether a value can actually be rewritten is only known once the message is serialized, because that is when
     * the document is built. {@link #rewrittenFields(Indexable)} reports what really happened.
     *
     * @param message      the message the indexer rejected
     * @param errorMessage the error the indexer reported for it
     */
    public static Optional<Indexable> coerce(Indexable message, @Nullable String errorMessage) {
        return numericField(errorMessage)
                .filter(target -> !targets(message, target.field()))
                .map(target -> CoercingIndexable.combine(message, target));
    }

    /**
     * Returns the fields of the given message that were really rewritten while it was serialized, which is only
     * those whose value could be read as a date. Empty for a message that was never coerced, or one whose value
     * turned out not to be a date after all.
     */
    public static Set<String> rewrittenFields(Indexable message) {
        return message instanceof CoercingIndexable coercing ? coercing.rewrittenFields() : Set.of();
    }

    private static boolean targets(Indexable message, String field) {
        return message instanceof CoercingIndexable coercing && coercing.targets(field);
    }

    /**
     * Extracts the name of the offending field, but only if the indexer mapped it to a numeric type.
     */
    static Optional<String> numericFieldFrom(@Nullable String errorMessage) {
        return numericField(errorMessage).map(NumericField::field);
    }

    private static Optional<NumericField> numericField(@Nullable String errorMessage) {
        if (errorMessage == null) {
            return Optional.empty();
        }
        final Matcher matcher = FAILED_FIELD.matcher(errorMessage);
        if (!matcher.find()) {
            return Optional.empty();
        }
        final String field = matcher.group(1);
        final String type = matcher.group(2).toLowerCase(Locale.ROOT);
        if (INTEGRAL_TYPES.contains(type)) {
            return Optional.of(new NumericField(field, EpochFormat.MILLISECONDS));
        }
        if (FRACTIONAL_TYPES.contains(type)) {
            return Optional.of(new NumericField(field, EpochFormat.SECONDS));
        }
        return Optional.empty();
    }

    private record NumericField(String field, EpochFormat format) {
    }

    /**
     * The two shapes a date can take in an existing numeric mapping. Which one applies is decided by the mapped type,
     * because that type was derived from the value the indexer saw first.
     */
    private enum EpochFormat {
        MILLISECONDS {
            @Override
            Object convert(DateTime value) {
                return value.getMillis();
            }
        },
        SECONDS {
            @Override
            Object convert(DateTime value) {
                // Millisecond precision is all we can recover; any finer detail is long gone by this point.
                return BigDecimal.valueOf(value.getMillis(), 3);
            }
        };

        abstract Object convert(DateTime value);
    }

    /**
     * Delegates everything to the wrapped message, except that it rewrites its target fields into the numeric shape
     * their mapping expects on the way to the indexer. {@link Indexable#serialize(SerializationContext)} is
     * intentionally not overridden, so the default implementation picks up the rewritten document.
     * <p>
     * A value that cannot be read as a date is passed through untouched and is not reported as rewritten.
     */
    private static final class CoercingIndexable implements Indexable {
        private final Indexable delegate;
        private final Map<String, EpochFormat> targets;
        private volatile Set<String> rewrittenFields = Set.of();

        private CoercingIndexable(Indexable delegate, Map<String, EpochFormat> targets) {
            this.delegate = delegate;
            this.targets = Map.copyOf(targets);
        }

        /**
         * Adds the given field to an already coerced message, or starts a new one.
         */
        private static Indexable combine(Indexable message, NumericField target) {
            if (message instanceof CoercingIndexable coercing) {
                final Map<String, EpochFormat> combined = new LinkedHashMap<>(coercing.targets);
                combined.put(target.field(), target.format());
                return new CoercingIndexable(coercing.delegate, combined);
            }
            return new CoercingIndexable(message, Map.of(target.field(), target.format()));
        }

        private boolean targets(String field) {
            return targets.containsKey(field);
        }

        private Set<String> rewrittenFields() {
            return rewrittenFields;
        }

        @Override
        public Map<String, Object> toElasticSearchObject(ObjectMapper objectMapper, @Nonnull Meter invalidTimestampMeter) {
            final Map<String, Object> source = delegate.toElasticSearchObject(objectMapper, invalidTimestampMeter);

            Map<String, Object> coerced = null;
            final Set<String> rewritten = new LinkedHashSet<>();
            for (Map.Entry<String, EpochFormat> target : targets.entrySet()) {
                final Object value = source.get(target.getKey());
                if (value == null) {
                    continue;
                }
                try {
                    final Object converted = target.getValue().convert(DateTimeConverter.convertToDateTime(value));
                    if (coerced == null) {
                        coerced = new HashMap<>(source);
                    }
                    coerced.put(target.getKey(), converted);
                    rewritten.add(target.getKey());
                } catch (IllegalArgumentException e) {
                    // Not a date, so the mapping conflict is not the one we know how to repair.
                }
            }

            this.rewrittenFields = Set.copyOf(rewritten);
            return coerced == null ? source : coerced;
        }

        @Override
        @SuppressWarnings("deprecation")
        public String getId() {
            return delegate.getId();
        }

        @Override
        public String getMessageId() {
            return delegate.getMessageId();
        }

        @Override
        public long getSize() {
            return delegate.getSize();
        }

        @Override
        public long getInputMessageSize() {
            return delegate.getInputMessageSize();
        }

        @Override
        public DateTime getReceiveTime() {
            return delegate.getReceiveTime();
        }

        @Override
        public DateTime getTimestamp() {
            return delegate.getTimestamp();
        }

        @Override
        public boolean supportsFailureHandling() {
            return delegate.supportsFailureHandling();
        }

        @Override
        public boolean isAccounted() {
            return delegate.isAccounted();
        }
    }
}
