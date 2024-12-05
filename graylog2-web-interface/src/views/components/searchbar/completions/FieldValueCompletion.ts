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
import isEqual from 'lodash/isEqual';

import { SearchSuggestions } from '@graylog/server-api';

import type { TimeRange, NoTimeRangeOverride } from 'views/logic/queries/Query';
import type FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import { normalizeFromSearchBarForBackend } from 'views/logic/queries/NormalizeTimeRange';
import { isNoTimeRangeOverride } from 'views/typeGuards/timeRange';
import { escape } from 'views/logic/queries/QueryHelper';
import {
  getFieldNameForFieldValueInBrackets,
  isCompleteFieldName,
  isTypeString,
  isTypeTerm, isKeywordOperator, isSpace, isTypeNumber, removeFinalColon,
} from 'views/components/searchbar/completions/token-helper';

import type { Completer, CompleterContext, FieldTypes } from '../SearchBarAutocompletions';
import type { Token, CompletionResult } from '../queryinput/ace-types';

const SUGGESTIONS_PAGE_SIZE = 50;

const unquote = (s: string) => s.replace(/^"(.*(?="$))"$/, '$1');

const formatValue = (value: string, type: string) => {
  const trimmedValue = value?.trim();

  switch (type) {
    case 'constant.numeric': return Number(trimmedValue);
    case 'string': return unquote(trimmedValue);
    case 'keyword.operator': return '';
    case 'paren.lparen': return '';
    default: return trimmedValue;
  }
};

const completionCaption = (fieldValue: string, input: string | number, isQuoted: boolean) => {
  const quotedValue = isQuoted ? fieldValue : escape(fieldValue);

  if (quotedValue.startsWith(String(input))) {
    return quotedValue;
  }

  return `${fieldValue} ⭢ ${input}`;
};

const getFieldNameAndInput = ({
  tokens,
  currentToken,
  prevToken,
  currentTokenIdx,
}: {
  tokens: Array<Token>,
  currentToken: Token | undefined | null,
  prevToken: Token | undefined | null,
  currentTokenIdx: number
}) => {
  const nextToken = tokens[currentTokenIdx + 1] ?? null;

  if (isCompleteFieldName(currentToken) && (!nextToken || isSpace(nextToken))) {
    return {
      fieldName: removeFinalColon(currentToken.value),
      input: '',
      isQuoted: false,
    };
  }

  if ((isTypeTerm(currentToken) || isTypeString(currentToken) || isKeywordOperator(currentToken) || isTypeNumber(currentToken)) && isCompleteFieldName(prevToken)) {
    return {
      fieldName: removeFinalColon(prevToken.value),
      input: formatValue(currentToken.value, currentToken.type),
      isQuoted: isTypeString(currentToken),
    };
  }

  const fieldNameFromForValueInBrackets = getFieldNameForFieldValueInBrackets(tokens, currentTokenIdx);

  if (fieldNameFromForValueInBrackets && !(isSpace(currentToken) && (isTypeTerm(prevToken) || isTypeNumber(prevToken)))) {
    return {
      fieldName: fieldNameFromForValueInBrackets,
      input: formatValue(currentToken.value, currentToken.type),
      isQuoted: false,
    };
  }

  return {};
};

const isEnumerableField = (field: FieldTypeMapping | undefined) => field?.type.isEnumerable() ?? false;

const formatSuggestion = (value: string, occurrence: number, input: string | number, isQuoted: boolean, title: string | undefined): CompletionResult => ({
  name: value,
  value: isQuoted ? value : escape(value),
  score: occurrence,
  caption: completionCaption(value, input, isQuoted),
  meta: title ? `${title}: ${occurrence} hits` : `${occurrence} hits`,
});

type PreviousSuggestions = Array<{ value: string, occurrence: number, title?: string }> | undefined;

class FieldValueCompletion implements Completer {
  private previousSuggestions: undefined | {
    furtherSuggestionsCount: number,
    suggestions: PreviousSuggestions,
    fieldName: string,
    input: string | number,
    timeRange: TimeRange | NoTimeRangeOverride | undefined,
    streams: Array<string> | undefined,
  };

  // eslint-disable-next-line class-methods-use-this
  private readonly shouldFetchCompletions = (fieldName: string, fieldTypes: FieldTypes) => {
    if (!fieldName) {
      return false;
    }

    const queryField = fieldTypes?.query[fieldName];

    if (!queryField || !isEnumerableField(queryField)) {
      const allFieldType = fieldTypes?.all[fieldName];

      return isEnumerableField(allFieldType);
    }

    return true;
  };

  private alreadyFetchedAllSuggestions(
    input: string | number,
    fieldName: string,
    streams: Array<string> | undefined,
    timeRange: TimeRange | NoTimeRangeOverride | undefined,
  ) {
    if (!this.previousSuggestions) {
      return false;
    }

    const {
      fieldName: prevFieldName,
      streams: prevStreams,
      timeRange: prevTimeRange,
      furtherSuggestionsCount,
      input: prevInput,
    } = this.previousSuggestions;

    return String(input).startsWith(String(prevInput))
      && prevFieldName === fieldName
      && isEqual(prevStreams, streams)
      && isEqual(prevTimeRange, timeRange)
      && !furtherSuggestionsCount;
  }

  private filterExistingSuggestions(input: string | number, isQuoted: boolean) {
    if (this.previousSuggestions) {
      return this.previousSuggestions.suggestions
        .filter(({ value }) => (isQuoted ? value : escape(value)).startsWith(String(input)))
        .map(({ value, occurrence, title }) => formatSuggestion(value, occurrence, input, isQuoted, title));
    }

    return [];
  }

  getCompletions = ({
    tokens,
    currentToken,
    currentTokenIdx,
    prevToken,
    timeRange,
    streams,
    fieldTypes,
    userTimezone,
  }: CompleterContext) => {
    const { fieldName, input, isQuoted } = getFieldNameAndInput({
      tokens,
      currentToken,
      currentTokenIdx,
      prevToken,
    });

    if (!fieldName) {
      return [];
    }

    if (!this.shouldFetchCompletions(fieldName, fieldTypes)) {
      return [];
    }

    if (this.alreadyFetchedAllSuggestions(input, fieldName, streams, timeRange)) {
      const existingSuggestions = this.filterExistingSuggestions(input, isQuoted);

      if (existingSuggestions.length > 0) {
        return existingSuggestions;
      }
    }

    const normalizedTimeRange = (!timeRange || isNoTimeRangeOverride(timeRange)) ? undefined : normalizeFromSearchBarForBackend(timeRange, userTimezone);

    return SearchSuggestions.suggestFieldValue({
      field: fieldName,
      input: input as string,
      timerange: normalizedTimeRange,
      streams,
      size: SUGGESTIONS_PAGE_SIZE,
    }).then(({ suggestions, sum_other_docs_count: furtherSuggestionsCount }) => {
      if (!suggestions) {
        return [];
      }

      this.previousSuggestions = {
        furtherSuggestionsCount,
        streams,
        timeRange,
        fieldName,
        input,
        suggestions,
      };

      return suggestions.map(({ value, occurrence, title }: any) => formatSuggestion(value, occurrence, input, isQuoted, title));
    });
  };

  public identifierRegexps = [/[a-zA-Z_0-9$\\/\-\u00A2-\u2000\u2070-\uFFFF]/];
}

export default FieldValueCompletion;
