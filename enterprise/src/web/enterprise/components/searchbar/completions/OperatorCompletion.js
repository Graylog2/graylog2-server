// @flow strict
import type { Completer } from '../SearchBarAutocompletions';
import type { CompletionResult, Token } from '../ace-types';

const operators: Array<CompletionResult> = [
  {
    name: 'AND',
    value: 'AND',
    score: 10,
    meta: 'operator',
  },
  {
    name: 'OR',
    value: 'OR',
    score: 10,
    meta: 'operator',
  },
];

const _matchesFieldName = (prefix) => {
  return field => (field.name.indexOf(prefix) >= 0);
};

class OperatorCompletion implements Completer {
  operators: Array<CompletionResult>;

  constructor(ops: Array<CompletionResult> = operators) {
    this.operators = ops;
  }

  getCompletions = (currentToken: ?Token, lastToken: ?Token, prefix: string): Array<CompletionResult> => {
    if (lastToken && (lastToken.type === 'string' || lastToken.type === 'text')) {
      const matchesFieldName = _matchesFieldName(prefix);
      return operators.filter(matchesFieldName);
    }
    return [];
  }
}

export default OperatorCompletion;
