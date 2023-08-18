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

import {
  getFieldNameForFieldValueInBrackets,
  isFieldValueWithSpecialCharacter,
  isCompleteFieldName,
  isFieldValue,
  isProximityCondition, isSpace,
  isString as isStringToken, isSpecialCharacter,
} from 'views/components/searchbar/completions/token-helper';

import type { CompletionResult, Token } from '../queryinput/ace-types';
import type { Completer, CompleterContext } from '../SearchBarAutocompletions';

type Suggestion = Readonly<{
  name: string,
  type: Readonly<{
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

const _matchesFieldName = (prefix) => (field) => {
  const result = field.name.indexOf(prefix);

  if (result < 0) {
    return 0;
  }

  // If substring occurs at start, return boost
  return result === 0 ? 2 : 1;
};

const isFollowingExistsOperator = (lastToken: Token | undefined | null) => ((lastToken && lastToken.value === `${existsOperator.name}:`) === true);

const shouldShowSuggestions = ({
  tokens,
  currentTokenIdx,
  prefix,
}) => {
  const currentToken = tokens[currentTokenIdx];
  const prevToken = tokens[currentTokenIdx - 1] ?? null;

  if (
    !currentToken
    || isFieldValueWithSpecialCharacter(currentToken, prevToken)
    || isSpecialCharacter(currentToken)
    || isFieldValue(currentToken, prevToken)
    || isProximityCondition(currentToken)
    || (isCompleteFieldName(currentToken) && !prefix)
    || isStringToken(currentToken)
    || isSpace(currentToken)
    || !!getFieldNameForFieldValueInBrackets(tokens, currentToken)
  ) {
    return false;
  }

  return true;
};

class FieldNameCompletion implements Completer {
  private readonly staticSuggestions: Array<Suggestion>;

  constructor(staticSuggestions: Array<Suggestion> = [existsOperator]) {
    this.staticSuggestions = staticSuggestions;
  }

  getCompletions = ({ tokens, currentTokenIdx, prevToken, prefix, fieldTypes }: CompleterContext) => {
    const fieldNameShouldShowSuggestions = shouldShowSuggestions({ tokens, currentTokenIdx, prefix });

    if (!fieldNameShouldShowSuggestions) {
      return [];
    }

    const matchesFieldName = _matchesFieldName(prefix);
    const { all, query } = fieldTypes;
    const currentQueryFields = Immutable.List(Object.values(query));
    const valuePosition = isFollowingExistsOperator(prevToken);

    const allButInCurrent = Object.values(all).filter((field) => !query[field.name]);
    const fieldsToMatchIn = valuePosition
      ? [...currentQueryFields.toArray()]
      : [...this.staticSuggestions, ...currentQueryFields.toArray()];
    const currentQuery = fieldsToMatchIn.filter((field) => (matchesFieldName(field) > 0))
      .map((field) => _fieldResult(field, 10 + matchesFieldName(field), valuePosition));
    const allFields = allButInCurrent.filter((field) => (matchesFieldName(field) > 0))
      .map((field) => _fieldResult(field, 1 + matchesFieldName(field), valuePosition))
      .map((result) => ({ ...result, meta: `${result.meta} (not in streams)` }));

    return [...currentQuery, ...allFields];
  };
}

export default FieldNameCompletion;
