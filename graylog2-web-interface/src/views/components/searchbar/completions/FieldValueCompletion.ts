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
import { isEmpty } from 'lodash';
import * as Immutable from 'immutable';

import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import type { TimeRange, NoTimeRangeOverride } from 'views/logic/queries/Query';
import { ViewMetadataStore } from 'views/stores/ViewMetadataStore';
import { FieldTypesStore, FieldTypesStoreState, FieldTypeMappingsList } from 'views/stores/FieldTypesStore';

import type { Completer } from '../SearchBarAutocompletions';
import type { Token } from '../ace-types';

const suggestionsUrl = qualifyUrl('/search/suggest');

const formatValue = (value: string, type: string) => {
  if (type === 'constant.numeric') {
    return Number(value);
  }

  return value;
};

class FieldValueCompletion implements Completer {
  activeQuery: string;

  fields: FieldTypesStoreState;

  currentQueryFields: FieldTypeMappingsList;

  constructor() {
    this.onViewMetadataStoreUpdate(ViewMetadataStore.getInitialState());
    ViewMetadataStore.listen(this.onViewMetadataStoreUpdate);

    this._newFields(FieldTypesStore.getInitialState());
    FieldTypesStore.listen((newState) => this._newFields(newState));
  }

  getCompletions = (
    currentToken: Token | undefined | null,
    lastToken: Token | undefined | null,
    prefix: string,
    tokens,
    currentTokenId,
    timeRange?: TimeRange | NoTimeRangeOverride,
    streams?: Array<string>,
  ) => {
    if (lastToken?.type !== 'keyword') {
      return [];
    }

    if (currentToken?.type !== 'term') {
      return [];
    }

    const field = lastToken.value.slice(0, -1);
    const fieldType = this.currentQueryFields.find(({ name }) => name === field);
    const isEnumerable = fieldType.type.properties.find((property) => property === 'enumerable');

    if (!isEnumerable || fieldType.type.value.get('type') === 'numeric') {
      return [];
    }

    return fetch('POST', suggestionsUrl, {
      field,
      input: formatValue(currentToken.value, currentToken.type),
      timerange: !isEmpty(timeRange) ? timeRange : undefined,
      streams,
    }).then(({ suggestions }) => {
      return suggestions.map(({ value, occurrence }) => ({ name: value, value, score: occurrence }));
    });
  };

  _newFields = (fields: FieldTypesStoreState) => {
    this.fields = fields;
    const { queryFields } = this.fields;

    if (this.activeQuery) {
      // const currentQueryFields: FieldTypeMappingsList = ;
      this.currentQueryFields = queryFields.get(this.activeQuery, Immutable.List());
    }
  };

  onViewMetadataStoreUpdate = (newState: { activeQuery: string }) => {
    const { activeQuery } = newState;

    this.activeQuery = activeQuery;

    if (this.fields) {
      this._newFields(this.fields);
    }
  };
}

export default FieldValueCompletion;
