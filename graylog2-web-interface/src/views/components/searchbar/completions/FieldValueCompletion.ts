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

const formatValue = (value: string, type: string) => {
  if (type === 'constant.numeric') {
    return Number(value);
  }

  return value;
};

const completionCaption = (fieldValue: string, input: string | number) => {
  if (fieldValue.startsWith(String(input))) {
    return fieldValue;
  }

  return `${fieldValue} ⭢ ${input}`;
};

const getFieldNameAndInput = (currentToken: Token | undefined | null, lastToken: Token | undefined | null) => {
  if (currentToken?.type === 'keyword' && currentToken?.value.endsWith(':')) {
    return {
      fieldName: currentToken.value.slice(0, -1),
      input: '',
    };
  }

  if (currentToken?.type === 'term' && lastToken?.type === 'keyword') {
    return {
      fieldName: lastToken.value.slice(0, -1),
      input: formatValue(currentToken.value, currentToken.type),
    };
  }

  return {};
};

const isEnumerableField = (field: FieldTypeMapping | undefined) => {
  return field?.type.isEnumerable() ?? false;
};

class FieldValueCompletion implements Completer {
  private previousSuggestions: undefined | {
    furtherSuggestionsCount: number,
    completions: Array<CompletionResult>,
    fieldName: string,
    input: string | number,
    timeRange: TimeRange | NoTimeRangeOverride | undefined,
    streams: Array<string> | undefined,
  };

  // eslint-disable-next-line class-methods-use-this
  shouldFetchCompletions = (fieldName: string, fieldTypes: FieldTypes) => {
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

  alreadyFetchedAllSuggestions(
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

  filterExistingSuggestions(input: string | number) {
    if (this.previousSuggestions) {
      return this.previousSuggestions.completions.filter((completion) => completion.name.startsWith(String(input)));
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
    const { fieldName, input } = getFieldNameAndInput(currentToken, lastToken);

    if (!this.shouldFetchCompletions(fieldName, fieldTypes)) {
      return [];
    }

    if (this.alreadyFetchedAllSuggestions(input, fieldName, streams, timeRange)) {
      const existingSuggestions = this.filterExistingSuggestions(input);

      if (existingSuggestions.length) {
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

      const completions = suggestions.map(({ value, occurrence }) => ({
        name: value,
        value: value,
        score: occurrence,
        caption: completionCaption(value, input),
        meta: `${occurrence} hits`,
      }));

      this.previousSuggestions = {
        furtherSuggestionsCount,
        streams,
        timeRange,
        fieldName,
        input,
        completions,
      };

      return completions;
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
    const currentTokenIsFieldValue = currentToken?.type === 'term' && previousToken?.type === 'keyword';
    const nextTokenIsTerm = nextToken?.type === 'term';

    return (currentTokenIsFieldName || currentTokenIsFieldValue) && !nextTokenIsTerm;
  };
}

export default FieldValueCompletion;
