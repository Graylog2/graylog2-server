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

import { StoreMock as MockStore } from 'helpers/mocking';
import asMock from 'helpers/mocking/AsMock';
import { FieldTypeMappingsList, FieldTypesStore, FieldTypesStoreState } from 'views/stores/FieldTypesStore';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import FieldType from 'views/logic/fieldtypes/FieldType';
import fetch from 'logic/rest/FetchProvider';

import FieldValueCompletion from './FieldValueCompletion';

const httpMethodField = new FieldTypeMapping('http_method', new FieldType('string', ['enumerable'], []));
const messageField = new FieldTypeMapping('message', new FieldType('string', [], []));
const MockFieldTypesStoreState = {
  all: Immutable.List([httpMethodField]),
  queryFields: Immutable.fromJS({ query1: [httpMethodField] }),
};

jest.mock('views/stores/FieldTypesStore', () => ({
  FieldTypesStore: MockStore(
    'listen',
    ['getInitialState', jest.fn(() => MockFieldTypesStoreState)],
  ),
}));

jest.mock('views/stores/ViewMetadataStore', () => ({
  ViewMetadataStore: MockStore(
    ['getInitialState', jest.fn(() => ({ activeQuery: 'query1' }))],
  ),
}));

jest.mock('logic/rest/FetchProvider', () => jest.fn());

describe('FieldValueCompletion', () => {
  const suggestionsResponse = {
    field: 'http_method',
    input: '',
    suggestions: [
      { value: 'GET', occurrence: 100 },
      { value: 'DELETE', occurrence: 200 },
      { value: 'POST', occurrence: 300 },
      { value: 'PUT', occurrence: 400 },
    ],
  };
  const expectedSuggestions = [
    { name: 'GET', value: 'GET', score: 100 },
    { name: 'DELETE', value: 'DELETE', score: 200 },
    { name: 'POST', value: 'POST', score: 300 },
    { name: 'PUT', value: 'PUT', score: 400 },
  ];

  const createKeywordToken = (value: string) => ({
    index: 0,
    start: 0,
    type: 'keyword',
    value,
  });

  beforeEach(() => {
    jest.clearAllMocks();
    asMock(fetch).mockReturnValue(Promise.resolve(suggestionsResponse));
  });

  it('returns empty list if inputs are empty', () => {
    const completer = new FieldValueCompletion();

    expect(completer.getCompletions(null, null, '', [], -1, undefined, undefined)).toEqual([]);
  });

  it('returns suggestions, when current token is a keyword', async () => {
    const currentToken = createKeywordToken('http_method:');
    const completer = new FieldValueCompletion();

    const suggestions = await completer.getCompletions(
      currentToken,
      null,
      '',
      [currentToken],
      0,
      undefined,
      undefined,
    );

    expect(suggestions).toEqual(expectedSuggestions);
  });

  it('returns suggestions, when current token is a term and last token is a keyword', async () => {
    const currentToken = {
      type: 'term',
      value: 'P',
      index: 1,
      start: 12,
    };
    const lastToken = {
      type: 'keyword',
      value: 'http_method:',
    };
    const completer = new FieldValueCompletion();

    const suggestions = await completer.getCompletions(
      currentToken,
      lastToken,
      'P',
      [lastToken, currentToken],
      1,
      undefined,
      undefined,
    );

    expect(suggestions).toEqual(expectedSuggestions);
  });

  it('returns suggestions when field type can only be found in all field types', async () => {
    asMock(FieldTypesStore.getInitialState).mockReturnValue({
      all: Immutable.List([httpMethodField]),
      queryFields: Immutable.fromJS({ query1: [] }),
    });

    const currentToken = createKeywordToken('http_method:');

    const completer = new FieldValueCompletion();

    const suggestions = await completer.getCompletions(
      currentToken,
      null,
      '',
      [currentToken],
      0,
      undefined,
      undefined,
    );

    expect(suggestions).toEqual(expectedSuggestions);
  });

  it('returns empty list when current token is a term which does not end with ":"', async () => {
    const currentToken = {
      index: 0,
      start: 0,
      type: 'term',
      value: 'http_method',
    };
    const completer = new FieldValueCompletion();

    const suggestions = await completer.getCompletions(
      currentToken,
      null,
      '',
      [currentToken],
      0,
      undefined,
      undefined,
    );

    expect(suggestions).toEqual([]);
  });

  it('returns empty list when field type can not be found in all and query field types', async () => {
    asMock(FieldTypesStore.getInitialState).mockReturnValue({
      all: Immutable.List(),
      queryFields: Immutable.fromJS({ query1: [] }),
    });

    const currentToken = createKeywordToken('unknown_field:');
    const completer = new FieldValueCompletion();

    const suggestions = await completer.getCompletions(
      currentToken,
      null,
      '',
      [currentToken],
      0,
      undefined,
      undefined,
    );

    expect(suggestions).toEqual([]);
  });

  it('returns empty list when field type is not enumerable', async () => {
    asMock(FieldTypesStore.getInitialState).mockReturnValue({
      all: Immutable.List([messageField]),
      queryFields: Immutable.fromJS({ query1: [messageField] }),
    });

    const currentToken = createKeywordToken('message:');

    const completer = new FieldValueCompletion();

    const suggestions = await completer.getCompletions(
      currentToken,
      null,
      '',
      [currentToken],
      0,
      undefined,
      undefined,
    );

    expect(suggestions).toEqual([]);
  });
});
