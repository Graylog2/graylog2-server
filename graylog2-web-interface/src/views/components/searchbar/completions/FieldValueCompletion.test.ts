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
import { StoreMock as MockStore } from 'helpers/mocking';
import asMock from 'helpers/mocking/AsMock';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import FieldType from 'views/logic/fieldtypes/FieldType';
import fetch from 'logic/rest/FetchProvider';
import type { FieldTypes } from 'views/components/searchbar/SearchBarAutocompletions';

import FieldValueCompletion from './FieldValueCompletion';

const httpMethodField = new FieldTypeMapping('http_method', new FieldType('string', ['enumerable'], []));
const actionField = new FieldTypeMapping('action', new FieldType('string', ['enumerable'], []));
const messageField = new FieldTypeMapping('message', new FieldType('string', [], []));

const fieldTypes: FieldTypes = {
  all: {
    http_method: httpMethodField,
  },
  query: {
    http_method: httpMethodField,
    action: actionField,
  },
};

jest.mock('views/stores/ViewMetadataStore', () => ({
  ViewMetadataStore: MockStore(
    ['getInitialState', jest.fn(() => ({ activeQuery: 'query1' }))],
  ),
}));

jest.mock('logic/rest/FetchProvider', () => jest.fn());
jest.mock('stores/users/CurrentUserStore', () => ({ CurrentUserStore: MockStore('get') }));

describe('FieldValueCompletion', () => {
  const suggestionsResponse = {
    field: 'http_method',
    input: '',
    sum_other_docs_count: 2,
    suggestions: [
      { value: 'POST', occurrence: 300 },
      { value: 'PUT', occurrence: 400 },
    ],
  };
  const expectedSuggestions = [
    { name: 'POST', value: 'POST', caption: 'POST', score: 300, meta: '300 hits' },
    { name: 'PUT', value: 'PUT', caption: 'PUT', score: 400, meta: '400 hits' },
  ];

  const createCurrentToken = (type: string, value: string, index: number, start: number) => ({ type, value, index, start });

  const createKeywordToken = (value: string) => createCurrentToken('keyword', value, 0, 0);

  beforeEach(() => {
    jest.clearAllMocks();
    asMock(fetch).mockReturnValue(Promise.resolve(suggestionsResponse));
  });

  describe('getCompletions', () => {
    it('returns empty list if inputs are empty', () => {
      const completer = new FieldValueCompletion();

      expect(completer.getCompletions({
        currentToken: null,
        lastToken: null,
        prefix: '',
        tokens: [],
        currentTokenIdx: -1,
        timeRange: undefined,
        streams: undefined,
        fieldTypes,
      })).toEqual([]);
    });

    it('returns suggestions, when current token is a keyword', async () => {
      const currentToken = createKeywordToken('http_method:');
      const completer = new FieldValueCompletion();

      const suggestions = await completer.getCompletions({
        currentToken,
        lastToken: null,
        prefix: '',
        tokens: [currentToken],
        currentTokenIdx: 0,
        timeRange: undefined,
        streams: undefined,
        fieldTypes,
      });

      expect(suggestions).toEqual(expectedSuggestions);
    });

    it('returns suggestions, when current token is a term and last token is a keyword', async () => {
      const currentToken = createCurrentToken('term', 'P', 1, 12);
      const lastToken = {
        type: 'keyword',
        value: 'http_method:',
      };
      const completer = new FieldValueCompletion();

      const suggestions = await completer.getCompletions({
        currentToken,
        lastToken,
        prefix: 'P',
        tokens: [lastToken, currentToken],
        currentTokenIdx: 1,
        timeRange: undefined,
        streams: undefined,
        fieldTypes,
      });

      expect(suggestions).toEqual(expectedSuggestions);
    });

    it('returns suggestions when field type can only be found in all field types', async () => {
      const currentToken = createKeywordToken('http_method:');

      const completer = new FieldValueCompletion();

      const suggestions = await completer.getCompletions({
        currentToken,
        lastToken: null,
        prefix: '',
        tokens: [currentToken],
        currentTokenIdx: 0,
        timeRange: undefined,
        streams: undefined,
        fieldTypes: {
          all: { http_method: httpMethodField },
          query: {},
        },
      });

      expect(suggestions).toEqual(expectedSuggestions);
    });

    it('returns empty list when current token is a term which does not end with ":"', async () => {
      const currentToken = createCurrentToken('term', 'http_method', 0, 0);
      const completer = new FieldValueCompletion();

      const suggestions = await completer.getCompletions({
        currentToken,
        lastToken: null,
        prefix: '',
        tokens: [currentToken],
        currentTokenIdx: 0,
        timeRange: undefined,
        streams: undefined,
        fieldTypes,
      });

      expect(suggestions).toEqual([]);
    });

    it('returns empty list when field type can not be found in all and query field types', async () => {
      const currentToken = createKeywordToken('unknown_field:');
      const completer = new FieldValueCompletion();

      const suggestions = await completer.getCompletions({
        currentToken,
        lastToken: null,
        prefix: '',
        tokens: [currentToken],
        currentTokenIdx: 0,
        timeRange: undefined,
        streams: undefined,
        fieldTypes: { all: {}, query: {} },
      });

      expect(suggestions).toEqual([]);
    });

    it('returns empty list when field type is not enumerable', async () => {
      const currentToken = createKeywordToken('message:');

      const completer = new FieldValueCompletion();

      const suggestions = await completer.getCompletions({
        currentToken,
        lastToken: null,
        prefix: '',
        tokens: [currentToken],
        currentTokenIdx: 0,
        timeRange: undefined,
        streams: undefined,
        fieldTypes: { all: { message: messageField }, query: { message: messageField } },
      });

      expect(suggestions).toEqual([]);
    });

    it('handles suggestions for spelling mistakes correctly', async () => {
      const response = {
        field: 'http_method',
        input: 'PSOT',
        sum_other_docs_count: 0,
        suggestions: [
          { value: 'POST', occurrence: 300 },
        ],
      };
      const currentToken = createCurrentToken('term', 'PSOT', 1, 12);
      const lastToken = {
        type: 'keyword',
        value: 'http_method:',
      };
      asMock(fetch).mockReturnValue(Promise.resolve(response));

      const completer = new FieldValueCompletion();

      const suggestions = await completer.getCompletions({
        currentToken,
        lastToken,
        prefix: 'PSOT',
        tokens: [lastToken, currentToken],
        currentTokenIdx: 1,
        timeRange: undefined,
        streams: undefined,
        fieldTypes,
      });

      const expectedCorrections = [
        { name: 'POST', value: 'POST', caption: 'POST ⭢ PSOT', score: 300, meta: '300 hits' },
      ];

      expect(suggestions).toEqual(expectedCorrections);
    });

    describe('refetching suggestions', () => {
      const currentToken = createCurrentToken('term', 'a', 1, 8);
      const lastToken = {
        type: 'keyword',
        value: 'action:',
      };

      const firstResponse = {
        field: 'action',
        input: 'a',
        sum_other_docs_count: 2,
        suggestions: [
          { value: 'action1', occurrence: 400 },
          { value: 'action2', occurrence: 300 },
        ],
      };

      const expectedFirstSuggestions = [
        { name: 'action1', value: 'action1', caption: 'action1', score: 400, meta: '400 hits' },
        { name: 'action2', value: 'action2', caption: 'action2', score: 300, meta: '300 hits' },
      ];

      it('is fetching further suggestions when there are some', async () => {
        asMock(fetch).mockReturnValue(Promise.resolve(firstResponse));

        const completer = new FieldValueCompletion();

        const firstSuggestions = await completer.getCompletions({
          currentToken,
          lastToken,
          prefix: 'a',
          tokens: [lastToken, currentToken],
          currentTokenIdx: 1,
          timeRange: undefined,
          streams: undefined,
          fieldTypes,
        });

        expect(firstSuggestions).toEqual(expectedFirstSuggestions);

        const secondResponse = {
          field: 'action',
          input: 'ac',
          sum_other_docs_count: 0,
          suggestions: [
            { value: 'action3', occurrence: 200 },
            { value: 'action4', occurrence: 100 },
          ],
        };
        asMock(fetch).mockReturnValue(Promise.resolve(secondResponse));

        const secondSuggestions = await completer.getCompletions({
          currentToken,
          lastToken,
          prefix: 'ac',
          tokens: [lastToken, currentToken],
          currentTokenIdx: 1,
          timeRange: undefined,
          streams: undefined,
          fieldTypes,
        });

        expect(secondSuggestions).toEqual([
          { name: 'action3', value: 'action3', caption: 'action3', score: 200, meta: '200 hits' },
          { name: 'action4', value: 'action4', caption: 'action4', score: 100, meta: '100 hits' },
        ]);
      });

      it('is not fetching further suggestions when there are none', async () => {
        asMock(fetch).mockReturnValue(Promise.resolve({ ...firstResponse, sum_other_docs_count: 0 }));

        const completer = new FieldValueCompletion();

        const firstSuggestions = await completer.getCompletions({
          currentToken,
          lastToken,
          prefix: 'a',
          tokens: [lastToken, currentToken],
          currentTokenIdx: 1,
          timeRange: undefined,
          streams: undefined,
          fieldTypes,
        });

        expect(firstSuggestions).toEqual(expectedFirstSuggestions);

        const secondSuggestions = await completer.getCompletions({
          currentToken,
          lastToken,
          prefix: 'ac',
          tokens: [lastToken, currentToken],
          currentTokenIdx: 1,
          timeRange: undefined,
          streams: undefined,
          fieldTypes,
        });

        expect(secondSuggestions).toEqual(expectedFirstSuggestions);
      });
    });
  });

  describe('shouldShowCompletions', () => {
    it('returns false by default', async () => {
      const completer = new FieldValueCompletion();
      const result = completer.shouldShowCompletions(1, [[], null]);

      expect(result).toEqual(false);
    });

    it('returns false if current token is a keyword which does not end with :', async () => {
      const completer = new FieldValueCompletion();
      const result = completer.shouldShowCompletions(1, [[{ type: 'keyword', value: 'http_method', index: 0, start: 0 }, null]]);

      expect(result).toEqual(false);
    });

    it('returns true if current token is a keyword and ends with ":"', async () => {
      const completer = new FieldValueCompletion();
      const result = completer.shouldShowCompletions(1, [[{ type: 'keyword', value: 'http_method:', index: 0, start: 0 }, null]]);

      expect(result).toEqual(true);
    });

    it('returns false if current token is a keyword that ends with ":" which is already followed by a term', async () => {
      const completer = new FieldValueCompletion();
      const result = completer.shouldShowCompletions(1, [[
        { type: 'keyword', value: 'http_method:', index: 0, start: 0 },
        { type: 'term', value: 'POST' }, null]]);

      expect(result).toEqual(false);
    });

    it('returns true if current token is a keyword in a complex query and ends with :', async () => {
      const completer = new FieldValueCompletion();
      const result = completer.shouldShowCompletions(
        1,
        [
          [
            { type: 'keyword', value: 'source:' },
            { type: 'term', value: 'example' },
            { type: 'text', value: '.' },
            { type: 'term', value: 'org' },
            { type: 'text', value: ' ' },
            { type: 'term', value: 'and' },
            { type: 'text', value: ' ' },
            {
              type: 'keyword',
              value: 'http_method:',
              index: 7,
              start: 23,
            },
          ],
          null,
        ],
      );

      expect(result).toEqual(true);
    });
  });
});
