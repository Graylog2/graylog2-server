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
import OperatorCompletion from './OperatorCompletion';

import type { Completer } from '../SearchBarAutocompletions';

const NOT = {
  name: 'NOT',
  value: 'NOT ',
  score: 10,
  meta: 'operator',
};

const AND = {
  name: 'AND',
  value: 'AND ',
  score: 10,
  meta: 'operator',
};

const OR = {
  name: 'OR',
  value: 'OR ',
  score: 10,
  meta: 'operator',
};

const term = (value: string, index: ?number, start: ?number) => ({ type: 'term', value, index, start });
const whitespace = () => ({ type: 'text', value: ' ' });
const and = () => ({ type: 'keyword.operator', value: 'AND' });
const or = () => ({ type: 'keyword.operator', value: 'OR' });

describe('OperatorCompletion', () => {
  let operatorCompletion: Completer;

  beforeEach(() => {
    operatorCompletion = new OperatorCompletion();
  });

  it('suggests NOT operator', () => {
    const token = term('N', 0, 0);
    const results = operatorCompletion.getCompletions(token, null, 'N', [token], 0);

    expect(results).toEqual([NOT]);
  });

  it('suggests NOT operator after empty term', () => {
    const token = term('N', 1, 1);
    const lastToken = whitespace();
    const results = operatorCompletion.getCompletions(token, lastToken, 'N', [lastToken, token], 1);

    expect(results).toEqual([NOT]);
  });

  it('suggests AND operator after non-empty term for term starting with "A"', () => {
    const prefix = 'A';
    const token = term(prefix, 2, 4);
    const lastToken = whitespace();
    const tokens = [term('foo'), lastToken, token];
    const results = operatorCompletion.getCompletions(token, lastToken, prefix, tokens, 2);

    expect(results).toEqual([AND]);
  });

  it('suggests OR operator after non-empty term for term starting with "R"', () => {
    const prefix = 'R';
    const token = term(prefix, 2, 4);
    const lastToken = whitespace();
    const tokens = [term('foo'), lastToken, token];
    const results = operatorCompletion.getCompletions(token, lastToken, prefix, tokens, 2);

    expect(results).toEqual([OR]);
  });

  it('suggests OR/NOT operators after non-empty term for term starting with "O"', () => {
    const prefix = 'O';
    const token = term(prefix, 2, 4);
    const lastToken = whitespace();
    const tokens = [term('foo'), lastToken, token];
    const results = operatorCompletion.getCompletions(token, lastToken, prefix, tokens, 2);

    expect(results).toEqual([OR, NOT]);
  });

  it('does not suggest OR operator after operator for term starting with "O"', () => {
    const prefix = 'O';
    const token = term(prefix, 4, 8);
    const lastToken = whitespace();
    const tokens = [term('foo'), whitespace(), and(), whitespace(), token];
    const results = operatorCompletion.getCompletions(token, lastToken, prefix, tokens, 4);

    expect(results).toEqual([NOT]);
  });

  it('does not suggest AND operator after operator for term starting with "A"', () => {
    const prefix = 'A';
    const token = term(prefix, 4, 8);
    const lastToken = whitespace();
    const tokens = [term('foo'), whitespace(), or(), whitespace(), token];
    const results = operatorCompletion.getCompletions(token, lastToken, prefix, tokens, 4);

    expect(results).toEqual([]);
  });

  it('does not suggest AND operator after operator for term starting with "A", followed by other terms', () => {
    const prefix = 'A';
    const currentToken = term(prefix, 4, 8);
    const lastToken = whitespace();
    const tokens = [term('foo'), whitespace(), or(), whitespace(), currentToken, whitespace(), term('qux')];
    const results = operatorCompletion.getCompletions(currentToken, lastToken, prefix, tokens, 4);

    expect(results).toEqual([]);
  });
});
