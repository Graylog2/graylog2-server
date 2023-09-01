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
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import FieldType from 'views/logic/fieldtypes/FieldType';
import type { CompletionResult, Token } from 'views/components/searchbar/queryinput/ace-types';
import { createSearch } from 'fixtures/searches';

import type { Suggestion } from './FieldNameCompletion';
import FieldNameCompletion from './FieldNameCompletion';

const _createField = (name: string) => FieldTypeMapping.create(name, FieldType.create('string', []));

const toIndex = (fields: Array<FieldTypeMapping>) => Object.fromEntries(fields.map((field) => [field.name, field]));
const createFieldTypes = (fields: Array<FieldTypeMapping>) => ({ all: toIndex(fields), query: toIndex(fields) });

const dummyFieldTypes = createFieldTypes(['source', 'message', 'timestamp'].map(_createField));
const userTimezone = 'Europe/Berlin';

describe('FieldNameCompletion', () => {
  const requestDefaults = {
    prevToken: null,
    tokens: [],
    currentTokenIdx: 0,
    fieldTypes: dummyFieldTypes,
    userTimezone,
    view: createSearch(),
  };

  const completionsFor = ({
    currentToken,
    currentTokenIdx = requestDefaults.currentTokenIdx,
    prefix,
    prevToken = requestDefaults.prevToken,
    staticSuggestions,
    targetKey = 'name',
    tokens = requestDefaults.tokens,
  }: {
    currentToken: Token,
    currentTokenIdx?: number
    prefix: string,
    prevToken?: Token,
    staticSuggestions?: Array<Suggestion>
    targetKey?: string
    tokens: Array<Token>
  }) => {
    const completer = new FieldNameCompletion(staticSuggestions);

    return completer.getCompletions({
      ...requestDefaults,
      currentToken,
      currentTokenIdx,
      prefix,
      prevToken,
      tokens,
    }).map((result) => result[targetKey]);
  };

  it('returns empty list if inputs are empty', () => {
    expect(completionsFor({ prefix: '', currentToken: null, tokens: [] })).toEqual([]);
  });

  it('returns matching fields if prefix is present in one field name', () => {
    const token = {
      type: 'term',
      value: 'mess',
    };

    expect(completionsFor({ prefix: 'mess', currentToken: token, tokens: [token] })).toEqual(['message']);
  });

  it('returns matching fields if prefix is present in at least one field name', () => {
    const token = {
      type: 'term',
      value: 'e',
    };

    expect(completionsFor({
      prefix: 'e',
      currentToken: token,
      tokens: [token],
      staticSuggestions: [],
    })).toEqual(['source', 'message', 'timestamp']);
  });

  it('suffixes matching fields with colon', () => {
    const token = {
      type: 'term',
      value: 'e',
    };

    expect(completionsFor({
      prefix: 'e',
      currentToken: token,
      tokens: [token],
      targetKey: 'value',
      staticSuggestions: [],
    })).toEqual(['source:', 'message:', 'timestamp:']);
  });

  it('returns _exist_-operator if matching prefix', () => {
    const token = {
      type: 'term',
      value: '_e',
    };

    expect(completionsFor({
      prefix: '_e',
      currentToken: token,
      tokens: [token],
      targetKey: 'value',
    })).toEqual(['_exists_:']);
  });

  it('returns matching fields after _exists_-operator', () => {
    const prevToken = {
      type: 'keyword',
      value: '_exists_:',
    };
    const currentToken = {
      type: 'term',
      value: 'e',
    };

    expect(completionsFor({
      prevToken,
      currentToken,
      tokens: [prevToken, currentToken],
      currentTokenIdx: 1,
      prefix: 'e',
    })).toEqual(['source', 'message', 'timestamp']);
  });

  it('returns exists operator together with matching fields', () => {
    const currentToken = {
      type: 'term',
      value: 'e',
    };

    expect(completionsFor({
      currentToken,
      tokens: [currentToken],
      prefix: 'e',
    })).toEqual(['_exists_', 'source', 'message', 'timestamp']);
  });

  it('returns empty list when current token is a keyword and the the prefix is empty', () => {
    const currentToken = { type: 'keyword', value: 'http_method:', index: 0, start: 0 };

    expect(completionsFor({
      currentToken,
      tokens: [currentToken],
      prefix: '',
    })).toEqual([]);
  });

  describe('considers current query', () => {
    const completionByName = (fieldName: string, completions: CompletionResult[]) => completions.find(({ name }) => (name === fieldName));

    const fieldTypes = {
      all: {
        foo: _createField('foo'),
        foo2: _createField('foo2'),
      },
      query: {
        foo: _createField('foo'),
      },
    };

    it('scores fields of current query higher', () => {
      const completer = new FieldNameCompletion([]);
      const token = {
        type: 'term',
        value: 'fo',
      };

      const completions = completer.getCompletions({
        ...requestDefaults,
        fieldTypes,
        tokens: [token],
        currentToken: token,
        prefix: 'fo',
      });

      const completion = (fieldName: string) => completionByName(fieldName, completions);

      expect(completion('foo')?.score).toEqual(12);
      expect(completion('foo')?.meta).not.toMatch('(not in streams)');

      expect(completion('foo2')?.score).toEqual(3);
      expect(completion('foo2')?.meta).toMatch('(not in streams)');
    });
  });
});
