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
import { isEqual } from 'lodash';

import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import type { TimeRange, NoTimeRangeOverride } from 'views/logic/queries/Query';
import type FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import { onSubmittingTimerange } from 'views/components/TimerangeForForm';
import { isNoTimeRangeOverride } from 'views/typeGuards/timeRange';
import { escape } from 'views/logic/queries/QueryHelper';

import type { Completer, CompleterContext, FieldTypes } from '../SearchBarAutocompletions';
import type { Token, Line, CompletionResult } from '../queryinput/ace-types';

const SUGGESTIONS_PAGE_SIZE = 50;

type SuggestionsResponse = {
  field: string,
  input: string,
  suggestions: Array<{ value: string, occurrence: number }> | undefined,
  sum_other_docs_count: number,
}

const suggestionsUrl = qualifyUrl('/search/suggest');

const unquote = (s: string) => s.replace(/^"(.*(?="$))"$/, '$1');

const formatValue = (value: string, type: string) => {
  switch (type) {
    case 'constant.numeric': return Number(value);
    case 'string': return unquote(value);
    default: return value;
  }
};

const completionCaption = (fieldValue: string, input: string | number, isQuoted: boolean) => {
  if ((isQuoted ? fieldValue : escape(fieldValue)).startsWith(String(input))) {
    return fieldValue;
  }

  return `${fieldValue} ⭢ ${input}`;
};

const isValueToken = (token: Line) => ['term', 'string'].includes(token?.type);

const getFieldNameAndInput = (currentToken: Token | undefined | null, lastToken: Token | undefined | null) => {
  if (currentToken?.type === 'keyword' && currentToken?.value.endsWith(':')) {
    return {
      fieldName: currentToken.value.slice(0, -1),
      input: '',
      isQuoted: false,
    };
  }

  if (isValueToken(currentToken) && lastToken?.type === 'keyword') {
    return {
      fieldName: lastToken.value.slice(0, -1),
      input: formatValue(currentToken.value, currentToken.type),
      isQuoted: currentToken?.type === 'string',
    };
  }

  return {};
};

const isEnumerableField = (field: FieldTypeMapping | undefined) => {
  return field?.type.isEnumerable() ?? false;
};

const formatSuggestion = (value: string, occurrence: number, input: string | number, isQuoted: boolean): CompletionResult => ({
  name: value,
  value: isQuoted ? value : escape(value),
  score: occurrence,
  caption: completionCaption(value, input, isQuoted),
  meta: `${occurrence} hits`,
});

class FieldValueCompletion implements Completer {
  private previousSuggestions: undefined | {
    furtherSuggestionsCount: number,
    suggestions: SuggestionsResponse['suggestions'],
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
        .map(({ value, occurrence }) => formatSuggestion(value, occurrence, input, isQuoted));
    }

    return [];
  }

  getCompletions = ({
    currentToken,
    lastToken,
    timeRange,
    streams,
    fieldTypes,
  }: CompleterContext) => {
    const { fieldName, input, isQuoted } = getFieldNameAndInput(currentToken, lastToken);

    if (!this.shouldFetchCompletions(fieldName, fieldTypes)) {
      return [];
    }

    if (this.alreadyFetchedAllSuggestions(input, fieldName, streams, timeRange)) {
      const existingSuggestions = this.filterExistingSuggestions(input, isQuoted);

      if (existingSuggestions.length > 0) {
        return existingSuggestions;
      }
    }

    const normalizedTimeRange = (!timeRange || isNoTimeRangeOverride(timeRange)) ? undefined : onSubmittingTimerange(timeRange);

    return fetch('POST', suggestionsUrl, {
      field: fieldName,
      input,
      timerange: normalizedTimeRange,
      streams,
      size: SUGGESTIONS_PAGE_SIZE,
    }).then(({ suggestions, sum_other_docs_count: furtherSuggestionsCount }: SuggestionsResponse) => {
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

      return suggestions.map(({ value, occurrence }) => formatSuggestion(value, occurrence, input, isQuoted));
    });
  };

  // eslint-disable-next-line class-methods-use-this
  shouldShowCompletions = (currentLine: number, lines: Array<Array<Line>>) => {
    const currentLineTokens = lines[currentLine - 1];
    const currentTokenIndex = currentLineTokens.findIndex((token) => token?.start !== undefined);
    const currentToken = currentLineTokens[currentTokenIndex];

    if (!currentToken) {
      return false;
    }

    const previousToken = currentLineTokens[currentTokenIndex - 1];
    const nextToken = currentLineTokens[currentTokenIndex + 1];

    const currentTokenIsFieldName = currentToken?.type === 'keyword' && currentToken?.value.endsWith(':');
    const currentTokenIsFieldValue = isValueToken(currentToken) && previousToken?.type === 'keyword';
    const nextTokenIsTerm = nextToken?.type === 'term';

    return (currentTokenIsFieldName || currentTokenIsFieldValue) && !nextTokenIsTerm;
  };
}

export default FieldValueCompletion;
