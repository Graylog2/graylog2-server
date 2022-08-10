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
import type { CompletionResult } from 'views/components/searchbar/queryinput/ace-types';

import FieldNameCompletion from './FieldNameCompletion';

const _createField = (name: string) => FieldTypeMapping.create(name, FieldType.create('string', []));

const toIndex = (fields: Array<FieldTypeMapping>) => Object.fromEntries(fields.map((field) => [field.name, field]));
const createFieldTypes = (fields: Array<FieldTypeMapping>) => ({ all: toIndex(fields), query: toIndex(fields) });

const dummyFieldTypes = createFieldTypes(['source', 'message', 'timestamp'].map(_createField));
const userTimezone = 'Europe/Berlin';

describe('FieldNameCompletion', () => {
  it('returns empty list if inputs are empty', () => {
    const fieldTypes = createFieldTypes([]);

    const completer = new FieldNameCompletion([]);

    expect(completer.getCompletions({
      currentToken: null,
      lastToken: null,
      prefix: '',
      tokens: [],
      currentTokenIdx: 0,
      fieldTypes,
      userTimezone,
    })).toEqual([]);
  });

  it('returns matching fields if prefix is present in one field name', () => {
    const completer = new FieldNameCompletion();

    expect(completer.getCompletions({
      currentToken: null,
      lastToken: null,
      prefix: 'mess',
      tokens: [],
      currentTokenIdx: 0,
      fieldTypes: dummyFieldTypes,
      userTimezone,
    }).map((result) => result.name)).toEqual(['message']);
  });

  it('returns matching fields if prefix is present in at least one field name', () => {
    const completer = new FieldNameCompletion([]);

    expect(completer.getCompletions({
      currentToken: null,
      lastToken: null,
      prefix: 'e',
      tokens: [],
      currentTokenIdx: 0,
      fieldTypes: dummyFieldTypes,
      userTimezone,
    }).map((result) => result.name)).toEqual(['source', 'message', 'timestamp']);
  });

  it('suffixes matching fields with colon', () => {
    const completer = new FieldNameCompletion([]);

    expect(completer.getCompletions({
      currentToken: null,
      lastToken: null,
      prefix: 'e',
      tokens: [],
      currentTokenIdx: 0,
      fieldTypes: dummyFieldTypes,
      userTimezone,
    }).map((result) => result.value))
      .toEqual(['source:', 'message:', 'timestamp:']);
  });

  it('returns _exist_-operator if matching prefix', () => {
    const completer = new FieldNameCompletion();

    expect(completer.getCompletions({
      currentToken: null,
      lastToken: null,
      prefix: '_e',
      tokens: [],
      currentTokenIdx: 0,
      fieldTypes: dummyFieldTypes,
      userTimezone,
    }).map((result) => result.value)).toEqual(['_exists_:']);
  });

  it('returns matching fields after _exists_-operator', () => {
    const completer = new FieldNameCompletion();

    expect(completer.getCompletions({
      currentToken: null,
      lastToken: {
        type: 'keyword',
        value: '_exists_:',
      },
      prefix: 'e',
      tokens: [],
      currentTokenIdx: 0,
      fieldTypes: dummyFieldTypes,
      userTimezone,
    }).map((result) => result.name)).toEqual(['source', 'message', 'timestamp']);
  });

  it('returns exists operator together with matching fields', () => {
    const completer = new FieldNameCompletion();

    expect(completer.getCompletions({
      currentToken: null,
      lastToken: null,
      prefix: 'e',
      tokens: [],
      currentTokenIdx: 0,
      fieldTypes: dummyFieldTypes,
      userTimezone,
    }).map((result) => result.name)).toEqual(['_exists_', 'source', 'message', 'timestamp']);
  });

  it('returns empty list when current token is a keyword and the the prefix is empty', () => {
    const completer = new FieldNameCompletion();
    const currentToken = { type: 'keyword', value: 'http_method:', index: 0, start: 0 };

    expect(completer.getCompletions({
      currentToken,
      lastToken: null,
      prefix: '',
      tokens: [],
      currentTokenIdx: 0,
      fieldTypes: dummyFieldTypes,
      userTimezone,
    })).toEqual([]);
  });

  describe('considers current query', () => {
    const completionByName = (fieldName: string, completions: CompletionResult[]) => completions.find(({ name }) => (name === fieldName));

    const fieldTypes = {
      all: {
        foo: _createField('foo'),
        bar: _createField('bar'),
      },
      query: {
        foo: _createField('foo'),
      },
    };

    it('scores fields of current query higher', () => {
      const completer = new FieldNameCompletion([]);

      const completions = completer.getCompletions({
        currentToken: null,
        lastToken: null,
        prefix: '',
        tokens: [],
        currentTokenIdx: 0,
        fieldTypes,
        userTimezone,
      });

      const completion = (fieldName: string) => completionByName(fieldName, completions);

      expect(completion('foo')?.score).toEqual(12);
      expect(completion('foo')?.meta).not.toMatch('(not in streams)');

      expect(completion('bar')?.score).toEqual(3);
      expect(completion('bar')?.meta).toMatch('(not in streams)');
    });
  });
});
