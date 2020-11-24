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
import { trim } from 'lodash';

import type { Completer } from '../SearchBarAutocompletions';
import type { CompletionResult, Token } from '../ace-types';

const combiningOperators: Array<CompletionResult> = [
  {
    name: 'AND',
    value: 'AND ',
    score: 10,
    meta: 'operator',
  },
  {
    name: 'OR',
    value: 'OR ',
    score: 10,
    meta: 'operator',
  },
];

const operators: Array<CompletionResult> = [
  {
    name: 'NOT',
    value: 'NOT ',
    score: 10,
    meta: 'operator',
  },
];

const _matchesFieldName = (prefix) => {
  return (field) => (field.name.indexOf(prefix) >= 0);
};

const _lastNonEmptyToken = (tokens: Array<Token>, currentTokenIdx: number): Token | undefined | null => {
  return tokens.slice(0, currentTokenIdx).reverse().find((token) => (token.type !== 'text' || trim(token.value) !== ''));
};

class OperatorCompletion implements Completer {
  getCompletions = (currentToken: Token | undefined | null, lastToken: Token | undefined | null, prefix: string, tokens: Array<Token>, currentTokenIdx: number): Array<CompletionResult> => {
    const lastNonEmptyToken = _lastNonEmptyToken(tokens, currentTokenIdx);

    if (!lastNonEmptyToken || (lastNonEmptyToken && (lastNonEmptyToken.type === 'keyword.operator'))) {
      const matchesFieldName = _matchesFieldName(prefix);

      return operators.filter(matchesFieldName);
    }

    if (lastToken && (lastToken.type === 'string' || lastToken.type === 'text')) {
      const matchesFieldName = _matchesFieldName(prefix);

      return [...combiningOperators, ...operators].filter(matchesFieldName);
    }

    return [];
  }
}

export default OperatorCompletion;
