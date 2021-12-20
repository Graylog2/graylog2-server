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
import * as Immutable from 'immutable';

import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import type { TimeRange, NoTimeRangeOverride } from 'views/logic/queries/Query';
import { ViewMetadataStore } from 'views/stores/ViewMetadataStore';
import type { FieldTypesStoreState, FieldTypeMappingsList } from 'views/stores/FieldTypesStore';
import { FieldTypesStore } from 'views/stores/FieldTypesStore';
import type FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import { onSubmittingTimerange } from 'views/components/TimerangeForForm';
import { isNoTimeRangeOverride } from 'views/typeGuards/timeRange';

import type { Completer } from '../SearchBarAutocompletions';
import type { Token, Line } from '../ace-types';

type SuggestionsResponse = {
  field: string,
  input: string,
  suggestions: Array<{ value: string, occurrence: number}> | undefined,
}

const suggestionsUrl = qualifyUrl('/search/suggest');

const formatValue = (value: string, type: string) => {
  if (type === 'constant.numeric') {
    return Number(value);
  }

  return value;
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

const getFieldByName = (fields: FieldTypeMappingsList, fieldName: string) => {
  return fields.find(({ name }) => name === fieldName);
};

const isEnumerableField = (field: FieldTypeMapping | undefined) => {
  return field?.type.isEnumerable() ?? false;
};

class FieldValueCompletion implements Completer {
  activeQuery: string;

  allFields: FieldTypeMappingsList;

  currentQueryFields: FieldTypeMappingsList;

  fields: FieldTypesStoreState;

  constructor() {
    this.onViewMetadataStoreUpdate(ViewMetadataStore.getInitialState());
    ViewMetadataStore.listen(this.onViewMetadataStoreUpdate);

    this._newFields(FieldTypesStore.getInitialState());
    FieldTypesStore.listen((newState) => this._newFields(newState));
  }

  shouldFetchCompletions = (fieldName: string) => {
    if (!fieldName) {
      return false;
    }

    const queryField = getFieldByName(this.currentQueryFields, fieldName);

    if (!queryField || !isEnumerableField(queryField)) {
      const allFieldType = getFieldByName(this.allFields, fieldName);

      return isEnumerableField(allFieldType);
    }

    return true;
  }

  getCompletions = (
    currentToken: Token | undefined | null,
    lastToken: Token | undefined | null,
    prefix: string,
    tokens: Array<Token>,
    currentTokenIdx: number,
    timeRange: TimeRange | NoTimeRangeOverride | undefined,
    streams: Array<string> | undefined,
  ) => {
    const { fieldName, input } = getFieldNameAndInput(currentToken, lastToken);

    if (!this.shouldFetchCompletions(fieldName)) {
      return [];
    }

    const normalizedTimeRange = (!timeRange || isNoTimeRangeOverride(timeRange)) ? undefined : onSubmittingTimerange(timeRange);

    return fetch('POST', suggestionsUrl, {
      field: fieldName,
      input,
      timerange: normalizedTimeRange,
      streams,
    }).then(({ suggestions }: SuggestionsResponse) => {
      if (!suggestions) {
        return [];
      }

      return suggestions.map(({ value, occurrence }) => ({
        name: value,
        value: value,
        score: occurrence,
      }));
    });
  };

  _newFields = (fields: FieldTypesStoreState) => {
    this.fields = fields;
    const { queryFields, all } = this.fields;

    if (this.activeQuery) {
      this.currentQueryFields = queryFields.get(this.activeQuery, Immutable.List());
      this.allFields = all;
    }
  };

  onViewMetadataStoreUpdate = (newState: { activeQuery: string }) => {
    const { activeQuery } = newState;

    this.activeQuery = activeQuery;

    if (this.fields) {
      this._newFields(this.fields);
    }
  };

  shouldShowCompletions = (currentLine: number, lines: Array<Array<Line>>) => {
    const currentToken = lines[currentLine - 1].find((token) => token?.start !== undefined);

    return currentToken?.type === 'keyword' && currentToken?.value.endsWith(':');
  }
}

export default FieldValueCompletion;
