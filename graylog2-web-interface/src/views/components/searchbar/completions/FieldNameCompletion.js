// @flow strict
import * as Immutable from 'immutable';
import { FieldTypesStore } from 'views/stores/FieldTypesStore';
import { ViewMetadataStore } from 'views/stores/ViewMetadataStore';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import type { FieldTypesStoreState } from 'views/stores/FieldTypesStore';

import type { CompletionResult, Token } from '../ace-types';
import type { Completer } from '../SearchBarAutocompletions';

const _fieldResult = (field: FieldTypeMapping, score: number = 1): CompletionResult => {
  const { name, type } = field;
  return {
    name,
    value: `${name}:`,
    score,
    meta: type.type,
  };
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

  constructor() {
    this.fields = FieldTypesStore.getInitialState();
    FieldTypesStore.listen((newState) => {
      this.fields = newState;
    });

    this.onViewMetadataStoreUpdate(ViewMetadataStore.getInitialState());
    ViewMetadataStore.listen(this.onViewMetadataStoreUpdate);
  }

  onViewMetadataStoreUpdate = (newState: { activeQuery: string }) => {
    const { activeQuery } = newState;
    this.activeQuery = activeQuery;
  };

  getCompletions = (currentToken: ?Token, lastToken: ?Token, prefix: string) => {
    if (lastToken && lastToken.type === 'keyword' && lastToken.value.endsWith(':')) {
      return [];
    }
    if (currentToken && currentToken.type === 'string') {
      return [];
    }
    const matchesFieldName = _matchesFieldName(prefix);
    const { all, queryFields } = this.fields;
    const currentQueryFields = queryFields.get(this.activeQuery, Immutable.Set());

    const currentQueryFieldNames = currentQueryFields.map(fieldMapping => fieldMapping.name);
    const allButInCurrent = all.filter(field => !currentQueryFieldNames.includes(field.name));
    const currentQuery = currentQueryFields.filter(matchesFieldName)
      .map(field => _fieldResult(field, 10 + matchesFieldName(field)));
    const allFields = allButInCurrent.filter(matchesFieldName)
      .map(field => _fieldResult(field, 1 + matchesFieldName(field)))
      .map(result => ({ ...result, meta: `${result.meta} (not in streams)` }));
    return [...currentQuery, ...allFields];
  }
}

export default FieldNameCompletion;
