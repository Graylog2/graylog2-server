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
// @flow strict
import * as Immutable from 'immutable';
import { $ReadOnly } from 'utility-types';

import { FieldTypesStore } from 'views/stores/FieldTypesStore';
import { ViewMetadataStore } from 'views/stores/ViewMetadataStore';
import type { FieldTypeMappingsList, FieldTypesStoreState } from 'views/stores/FieldTypesStore';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';

import type { CompletionResult, Token } from '../ace-types';
import type { Completer } from '../SearchBarAutocompletions';

type Suggestion = $ReadOnly<{
  name: string,
  type: $ReadOnly<{
    type: string,
  }>,
}>;

const _fieldResult = (field: Suggestion, score: number = 1, valuePosition: boolean = false): CompletionResult => {
  const { name, type } = field;

  return {
    name,
    value: `${name}${valuePosition ? '' : ':'}`,
    score,
    meta: type.type,
  };
};

export const existsOperator: Suggestion = {
  name: '_exists_',
  type: {
    type: 'operator',
  },
};

const _matchesFieldName = (prefix) => {
  return (field) => {
    const result = field.name.indexOf(prefix);

    if (result < 0) {
      return 0;
    }

    // If substring occurs at start, return boost
    return result === 0 ? 2 : 1;
  };
};

class FieldNameCompletion implements Completer {
  activeQuery: string;

  fields: FieldTypesStoreState;

  currentQueryFieldNames: { [key: string]: string };

  staticSuggestions: Array<Suggestion>;

  constructor(staticSuggestions: Array<Suggestion> = [existsOperator]) {
    this.staticSuggestions = staticSuggestions;
    this.onViewMetadataStoreUpdate(ViewMetadataStore.getInitialState());
    ViewMetadataStore.listen(this.onViewMetadataStoreUpdate);

    this._newFields(FieldTypesStore.getInitialState());
    FieldTypesStore.listen((newState) => this._newFields(newState));
  }

  _newFields = (fields: FieldTypesStoreState) => {
    this.fields = fields;
    const { queryFields } = this.fields;

    if (this.activeQuery) {
      const currentQueryFields: FieldTypeMappingsList = queryFields.get(this.activeQuery, Immutable.List());

      this.currentQueryFieldNames = currentQueryFields.map((fieldMapping) => fieldMapping.name)
        .reduce((prev, cur) => ({ ...prev, [cur]: cur }), {});
    }
  };

  onViewMetadataStoreUpdate = (newState: { activeQuery: string }) => {
    const { activeQuery } = newState;

    this.activeQuery = activeQuery;

    if (this.fields) {
      this._newFields(this.fields);
    }
  };

  _isFollowingExistsOperator = (lastToken: Token | undefined | null) => ((lastToken && lastToken.value === `${existsOperator.name}:`) === true);

  _isFollowingFieldName = (lastToken: Token | undefined | null) => (lastToken && lastToken.type === 'keyword' && lastToken.value.endsWith(':'));

  getCompletions = (currentToken: Token | undefined | null, lastToken: Token | undefined | null, prefix: string) => {
    if (this._isFollowingFieldName(lastToken) && !this._isFollowingExistsOperator(lastToken)) {
      return [];
    }

    if (currentToken && currentToken.type === 'string') {
      return [];
    }

    const matchesFieldName = _matchesFieldName(prefix);
    const { all, queryFields } = this.fields;
    // @ts-ignore
    const currentQueryFields: FieldTypeMapping[] = queryFields.get(this.activeQuery, Immutable.List());

    const valuePosition = this._isFollowingExistsOperator(lastToken);

    const allButInCurrent = all.filter((field) => !this.currentQueryFieldNames[field.name]);
    const fieldsToMatchIn = valuePosition
      ? [...currentQueryFields]
      : [...this.staticSuggestions, ...currentQueryFields];
    const currentQuery = fieldsToMatchIn.filter((field) => (matchesFieldName(field) > 0))
      .map((field) => _fieldResult(field, 10 + matchesFieldName(field), valuePosition));
    // @ts-ignore
    const allFields: CompletionResult[] = allButInCurrent.filter((field) => (matchesFieldName(field) > 0))
      .map((field) => _fieldResult(field, 1 + matchesFieldName(field), valuePosition))
      .map((result) => ({ ...result, meta: `${result.meta} (not in streams)` }));

    return [...currentQuery, ...allFields];
  }
}

export default FieldNameCompletion;
