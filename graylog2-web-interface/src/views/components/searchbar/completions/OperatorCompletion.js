// @flow strict
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

const _lastNonEmptyToken = (tokens: Array<Token>, currentTokenIdx: number): ?Token => {
  return tokens.slice(0, currentTokenIdx).reverse().find((token) => (token.type !== 'text' || trim(token.value) !== ''));
};

class OperatorCompletion implements Completer {
  getCompletions = (currentToken: ?Token, lastToken: ?Token, prefix: string, tokens: Array<Token>, currentTokenIdx: number): Array<CompletionResult> => {
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
