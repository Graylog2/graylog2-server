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
package org.graylog2.search;

import com.mongodb.client.model.Filters;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.QueryParserConstants;
import org.apache.lucene.queryparser.classic.Token;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.util.BytesRef;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.graylog2.search.InvalidLuceneQueryException.Position;
import org.graylog2.search.InvalidLuceneQueryException.Reason;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static org.graylog2.search.SearchQueryField.Type.STRING;
import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * Translates queries in the Lucene query syntax into MongoDB filters.
 * <p>
 * The supported syntax is a subset of the Lucene query syntax as used for searches in OpenSearch:
 * <dl>
 *     <dt><code>text</code>, <code>"some text"</code></dt>
 *     <dd>Searches for the text in the default field</dd>
 *     <dt><code>field:text</code>, <code>field:"some text"</code></dt>
 *     <dd>Searches for the text in the given field. String fields match if they contain the text, ignoring the case,
 *     other fields match if they are equal to the value.</dd>
 *     <dt><code>field:&gt;value</code>, <code>field:&gt;=value</code>, <code>field:&lt;value</code>, <code>field:&lt;=value</code></dt>
 *     <dd>Compares the field to the value</dd>
 *     <dt><code>field:[from TO to]</code>, <code>field:{from TO to}</code>, <code>field:[from TO *]</code></dt>
 *     <dd>Searches for values in the inclusive or exclusive range, <code>*</code> leaves the range open</dd>
 *     <dt><code>field:te?t*</code></dt>
 *     <dd>Matches the whole value of string fields against the wildcard pattern, ignoring the case</dd>
 *     <dt><code>_exists_:field</code>, <code>field:*</code></dt>
 *     <dd>Searches for entries which have the field</dd>
 *     <dt><code>AND</code>, <code>OR</code>, <code>NOT</code>, <code>+</code>, <code>-</code>, <code>( )</code></dt>
 *     <dd>Combines and groups conditions, terms without operator are combined with <code>OR</code></dd>
 * </dl>
 * Fuzzy, proximity and regular expression queries are not supported. Boosts are ignored.
 * <p>
 * Only the fields of the mapping can be searched, which translates them to the database fields and types.
 * Instances of this class are safe to use from multiple threads.
 */
public class LuceneToMongoQueryTranslator {
    private static final String DEFAULT_FIELD = "_default_";
    private static final String EXISTS_FIELD = "_exists_";
    private static final List<String> COMPARISON_OPERATORS = List.of(">=", "<=", ">", "<");

    @Nonnull
    private final SearchQueryField defaultField;
    @Nonnull
    private final Map<String, SearchQueryField> fieldMapping;

    /**
     * @param defaultField the database name of the string field to search for terms without field
     * @param fieldMapping the fields which can be searched, keys are the names in queries
     */
    public LuceneToMongoQueryTranslator(@Nonnull String defaultField, @Nonnull Map<String, SearchQueryField> fieldMapping) {
        this.defaultField = SearchQueryField.create(requireNonNull(defaultField), STRING);
        this.fieldMapping = requireNonNull(fieldMapping);
    }

    /**
     * Translates the query into a MongoDB filter. An empty query or <code>*</code> matches all entries.
     *
     * @throws InvalidLuceneQueryException if the query is invalid, uses unknown fields or unsupported syntax
     */
    public Bson translate(String query) {
        if (QueryStringUtils.isEmptyOrMatchAllQueryString(query)) {
            return matchAll();
        }
        return toBson(parse(query));
    }

    private Query parse(String query) {
        // The parser is not thread-safe, so a new one is used for every query.
        final QueryParser parser = new QueryParser(DEFAULT_FIELD, new WhitespaceAnalyzer());
        parser.setSplitOnWhitespace(true);
        parser.setAllowLeadingWildcard(true);
        try {
            return parser.parse(query);
        } catch (ParseException e) {
            throw parseError(query, e);
        } catch (IllegalArgumentException e) {
            throw new InvalidLuceneQueryException(Reason.PARSE_ERROR, f("Invalid query: %s", e.getMessage()), null, e);
        }
    }

    private static InvalidLuceneQueryException parseError(String query, ParseException e) {
        // QueryParser#parse wraps the exception of the generated parser, which knows the position.
        final ParseException original = e.getCause() instanceof ParseException cause ? cause : e;
        final Token token = original.currentToken == null ? null : original.currentToken.next;
        if (token == null) {
            return new InvalidLuceneQueryException(Reason.PARSE_ERROR, f("Invalid query: %s", e.getMessage()), null, e);
        }
        // The parser counts the columns of tokens as offsets in the whole query, without lines.
        final Position position = positionOf(query, token.beginColumn, token.endColumn);
        final String message = token.kind == QueryParserConstants.EOF
                ? "Invalid query: the query ended unexpectedly."
                : f("Invalid query: unexpected <%s> at line %d, column %d.", token.image, position.beginLine(), position.beginColumn() + 1);
        return new InvalidLuceneQueryException(Reason.PARSE_ERROR, message, position, e);
    }

    private static Position positionOf(String query, int beginOffset, int endOffset) {
        final int begin = Math.min(beginOffset, query.length());
        final int end = Math.min(Math.max(endOffset, begin), query.length());
        return new Position(lineOf(query, begin), columnOf(query, begin), lineOf(query, end), columnOf(query, end));
    }

    private static int lineOf(String query, int offset) {
        return (int) query.substring(0, offset).chars().filter(c -> c == '\n').count() + 1;
    }

    private static int columnOf(String query, int offset) {
        return offset - (query.lastIndexOf('\n', offset - 1) + 1);
    }

    private Bson toBson(Query query) {
        return switch (query) {
            case BooleanQuery booleanQuery -> booleanToBson(booleanQuery);
            case BoostQuery boostQuery -> toBson(boostQuery.getQuery());
            case MatchAllDocsQuery ignored -> matchAll();
            case TermQuery termQuery -> termToBson(termQuery.getTerm());
            case PhraseQuery phraseQuery -> phraseToBson(phraseQuery);
            case TermRangeQuery rangeQuery -> rangeToBson(rangeQuery);
            case WildcardQuery wildcardQuery -> wildcardToBson(wildcardQuery.getTerm().field(), wildcardQuery.getTerm().text());
            case PrefixQuery prefixQuery -> wildcardToBson(prefixQuery.getField(), escapeWildcards(prefixQuery.getPrefix().text()) + "*");
            default -> throw new InvalidLuceneQueryException(Reason.UNSUPPORTED_QUERY,
                    f("Unsupported query <%s>, fuzzy, proximity and regular expression queries are not supported.", query));
        };
    }

    private Bson booleanToBson(BooleanQuery query) {
        final List<Bson> required = new ArrayList<>();
        final List<Bson> optional = new ArrayList<>();
        final List<Bson> prohibited = new ArrayList<>();
        for (final BooleanClause clause : query.clauses()) {
            final Bson filter = toBson(clause.query());
            switch (clause.occur()) {
                case MUST, FILTER -> required.add(filter);
                case SHOULD -> optional.add(filter);
                case MUST_NOT -> prohibited.add(filter);
            }
        }

        // Like in Lucene, optional clauses don't restrict the matches if there are required ones.
        if (required.isEmpty() && !optional.isEmpty()) {
            required.add(optional.size() == 1 ? optional.getFirst() : Filters.or(optional));
        }
        if (!prohibited.isEmpty()) {
            required.add(Filters.nor(prohibited));
        }

        return switch (required.size()) {
            case 0 -> matchAll();
            case 1 -> required.getFirst();
            default -> Filters.and(required);
        };
    }

    private Bson termToBson(Term term) {
        if (EXISTS_FIELD.equals(term.field())) {
            return existsFilter(resolveField(term.text()));
        }
        final SearchQueryField field = resolveField(term.field());
        final String text = term.text();
        for (final String operator : COMPARISON_OPERATORS) {
            if (text.startsWith(operator) && text.length() > operator.length()) {
                return comparisonToBson(field, operator, text.substring(operator.length()));
            }
        }
        return valueToBson(field, text);
    }

    private Bson phraseToBson(PhraseQuery query) {
        if (query.getSlop() != 0) {
            throw new InvalidLuceneQueryException(Reason.UNSUPPORTED_QUERY, f("Unsupported query <%s>, proximity queries are not supported.", query));
        }
        final String text = Arrays.stream(query.getTerms()).map(Term::text).collect(Collectors.joining(" "));
        return valueToBson(resolveField(query.getField()), text);
    }

    private Bson valueToBson(SearchQueryField field, String text) {
        final SearchQueryOperator operator = field.getFieldType() == STRING ? SearchQueryOperators.REGEXP : SearchQueryOperators.EQUALS;
        return filterToBson(field, operator, text);
    }

    private Bson comparisonToBson(SearchQueryField field, String operator, String text) {
        return filterToBson(field, switch (operator) {
            case ">=" -> SearchQueryOperators.GREATER_EQUALS;
            case "<=" -> SearchQueryOperators.LESS_EQUALS;
            case ">" -> SearchQueryOperators.GREATER;
            default -> SearchQueryOperators.LESS;
        }, text);
    }

    private Bson filterToBson(SearchQueryField field, SearchQueryOperator operator, String text) {
        final Object value = convert(field, text);
        return field.getBsonFilterCreator()
                .map(creator -> creator.createFilter(field.getDbField(), new SearchQueryParser.FieldValue(value, operator, false, field)))
                .orElseGet(() -> operator.buildBson(field.getDbField(), value));
    }

    private Bson rangeToBson(TermRangeQuery query) {
        final SearchQueryField field = resolveField(query.getField());
        checkNoFilterCreator(field, query);
        final List<Bson> bounds = new ArrayList<>(2);
        if (query.getLowerTerm() != null) {
            final Object lower = convert(field, text(query.getLowerTerm()));
            bounds.add(query.includesLower() ? Filters.gte(field.getDbField(), lower) : Filters.gt(field.getDbField(), lower));
        }
        if (query.getUpperTerm() != null) {
            final Object upper = convert(field, text(query.getUpperTerm()));
            bounds.add(query.includesUpper() ? Filters.lte(field.getDbField(), upper) : Filters.lt(field.getDbField(), upper));
        }
        return switch (bounds.size()) {
            case 0 -> existsFilter(field);
            case 1 -> bounds.getFirst();
            default -> Filters.and(bounds);
        };
    }

    private Bson wildcardToBson(String fieldName, String pattern) {
        final SearchQueryField field = resolveField(fieldName);
        if ("*".equals(pattern)) {
            return existsFilter(field);
        }
        if (field.getFieldType() != STRING) {
            throw new InvalidLuceneQueryException(Reason.UNSUPPORTED_QUERY, f("Wildcards are only supported for text fields, not for <%s>.", fieldName));
        }
        checkNoFilterCreator(field, pattern);
        return Filters.regex(field.getDbField(), Pattern.compile("^" + wildcardToRegex(pattern) + "$", CASE_INSENSITIVE));
    }

    private static String wildcardToRegex(String pattern) {
        final StringBuilder regex = new StringBuilder();
        final StringBuilder literal = new StringBuilder();
        for (int i = 0; i < pattern.length(); i++) {
            final char c = pattern.charAt(i);
            if (c == '*' || c == '?') {
                appendLiteral(regex, literal);
                regex.append(c == '*' ? ".*" : ".");
            } else if (c == '\\' && i + 1 < pattern.length()) {
                literal.append(pattern.charAt(++i));
            } else {
                literal.append(c);
            }
        }
        appendLiteral(regex, literal);
        return regex.toString();
    }

    private static String escapeWildcards(String text) {
        return text.replace("\\", "\\\\").replace("*", "\\*").replace("?", "\\?");
    }

    private static void appendLiteral(StringBuilder regex, StringBuilder literal) {
        if (!literal.isEmpty()) {
            regex.append(Pattern.quote(literal.toString()));
            literal.setLength(0);
        }
    }

    private SearchQueryField resolveField(String name) {
        if (DEFAULT_FIELD.equals(name)) {
            return defaultField;
        }
        final SearchQueryField field = fieldMapping.get(name);
        if (field == null) {
            throw new InvalidLuceneQueryException(Reason.UNKNOWN_FIELD, f("Unknown field <%s>, the supported fields are: %s",
                    name, String.join(", ", new TreeSet<>(fieldMapping.keySet()))));
        }
        return field;
    }

    private static void checkNoFilterCreator(SearchQueryField field, Object query) {
        if (field.getBsonFilterCreator().isPresent()) {
            throw new InvalidLuceneQueryException(Reason.UNSUPPORTED_QUERY,
                    f("Unsupported query <%s>, only values can be searched in field <%s>.", query, field.getDbField()));
        }
    }

    private static Object convert(SearchQueryField field, String text) {
        try {
            return field.getFieldType().getMongoValueConverter().apply(text);
        } catch (RuntimeException e) {
            throw new InvalidLuceneQueryException(Reason.INVALID_VALUE,
                    f("Invalid value <%s> for field <%s> of type %s.", text, field.getDbField(), field.getFieldType()), null, e);
        }
    }

    private static String text(BytesRef bytes) {
        return bytes.utf8ToString();
    }

    // Like in OpenSearch, fields with null values don't exist.
    private static Bson existsFilter(SearchQueryField field) {
        return Filters.ne(field.getDbField(), null);
    }

    private static Bson matchAll() {
        return new Document();
    }
}
