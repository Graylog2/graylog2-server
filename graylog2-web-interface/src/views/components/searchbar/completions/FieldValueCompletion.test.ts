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
import { SearchSuggestions } from '@graylog/server-api';

import { StoreMock as MockStore } from 'helpers/mocking';
import asMock from 'helpers/mocking/AsMock';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import FieldType, { Properties } from 'views/logic/fieldtypes/FieldType';
import type { FieldTypes } from 'views/components/searchbar/SearchBarAutocompletions';

import FieldValueCompletion from './FieldValueCompletion';

const httpMethodField = FieldTypeMapping.create('http_method', FieldType.create('string', [Properties.Enumerable], []));
const actionField = FieldTypeMapping.create('action', FieldType.create('string', [Properties.Enumerable], []));
const messageField = FieldTypeMapping.create('message', FieldType.create('string', [], []));
const processField = FieldTypeMapping.create('process', FieldType.create('string', [Properties.Enumerable]));

const fieldTypes: FieldTypes = {
  all: {
    http_method: httpMethodField,
    process: processField,
  },
  query: {
    http_method: httpMethodField,
    action: actionField,
    process: processField,
  },
};

jest.mock('views/stores/ViewMetadataStore', () => ({
  ViewMetadataStore: MockStore(
    ['getInitialState', jest.fn(() => ({ activeQuery: 'query1' }))],
  ),
}));

jest.mock('stores/users/CurrentUserStore', () => ({ CurrentUserStore: MockStore('get') }));

jest.mock('@graylog/server-api', () => ({
  SearchSuggestions: {
    suggestFieldValue: jest.fn(),
  },
}));

describe('FieldValueCompletion', () => {
  const suggestionsResponse = {
    field: 'http_method',
    input: '',
    sum_other_docs_count: 2,
    suggestions: [
      { value: 'POST', occurrence: 300 },
      { value: 'PUT', occurrence: 400 },
    ],
    error: undefined,
  };
  const expectedSuggestions = [
    { name: 'POST', value: 'POST', caption: 'POST', score: 300, meta: '300 hits' },
    { name: 'PUT', value: 'PUT', caption: 'PUT', score: 400, meta: '400 hits' },
  ];

  const createCurrentToken = (type: string, value: string, index: number, start: number) => ({ type, value, index, start });

  const createKeywordToken = (value: string) => createCurrentToken('keyword', value, 0, 0);

  beforeEach(() => {
    jest.clearAllMocks();
    asMock(SearchSuggestions.suggestFieldValue).mockResolvedValue(suggestionsResponse);
  });

  describe('getCompletions', () => {
    const requestDefaults = {
      currentToken: null,
      lastToken: null,
      prefix: '',
      tokens: [],
      currentTokenIdx: -1,
      timeRange: undefined,
      streams: undefined,
      fieldTypes,
    };

    it('returns empty list if inputs are empty', () => {
      const completer = new FieldValueCompletion();

      expect(completer.getCompletions({
        ...requestDefaults,
      })).toEqual([]);
    });

    it('returns suggestions, when current token is a keyword', async () => {
      const currentToken = createKeywordToken('http_method:');
      const completer = new FieldValueCompletion();

      const suggestions = await completer.getCompletions({
        ...requestDefaults,
        currentToken,
        tokens: [currentToken],
        currentTokenIdx: 0,
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
        ...requestDefaults,
        currentToken,
        lastToken,
        prefix: 'P',
        tokens: [lastToken, currentToken],
        currentTokenIdx: 1,
      });

      expect(suggestions).toEqual(expectedSuggestions);
    });

    it('returns suggestions, field value is a quoted string', async () => {
      const currentToken = createCurrentToken('string', '"P"', 1, 12);
      const lastToken = {
        type: 'keyword',
        value: 'http_method:',
      };
      const completer = new FieldValueCompletion();

      const suggestions = await completer.getCompletions({
        ...requestDefaults,
        currentToken,
        lastToken,
        prefix: 'P',
        tokens: [lastToken, currentToken],
        currentTokenIdx: 1,
      });

      expect(suggestions).toEqual(expectedSuggestions);
    });

    it('returns suggestions, field value is an empty quoted string', async () => {
      const currentToken = createCurrentToken('string', '""', 1, 12);
      const lastToken = {
        type: 'keyword',
        value: 'http_method:',
      };
      const completer = new FieldValueCompletion();

      const suggestions = await completer.getCompletions({
        ...requestDefaults,
        currentToken,
        lastToken,
        prefix: '',
        tokens: [lastToken, currentToken],
        currentTokenIdx: 1,
      });

      expect(suggestions).toEqual(expectedSuggestions);
    });

    it('returns suggestions when field type can only be found in all field types', async () => {
      const currentToken = createKeywordToken('http_method:');

      const completer = new FieldValueCompletion();

      const suggestions = await completer.getCompletions({
        ...requestDefaults,
        currentToken,
        tokens: [currentToken],
        currentTokenIdx: 0,
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
        ...requestDefaults,
        currentToken,
        tokens: [currentToken],
        currentTokenIdx: 0,
      });

      expect(suggestions).toEqual([]);
    });

    it('returns empty list when field type can not be found in all and query field types', async () => {
      const currentToken = createKeywordToken('unknown_field:');
      const completer = new FieldValueCompletion();

      const suggestions = await completer.getCompletions({
        ...requestDefaults,
        currentToken,
        tokens: [currentToken],
        currentTokenIdx: 0,
        fieldTypes: { all: {}, query: {} },
      });

      expect(suggestions).toEqual([]);
    });

    it('returns empty list when field type is not enumerable', async () => {
      const currentToken = createKeywordToken('message:');

      const completer = new FieldValueCompletion();

      const suggestions = await completer.getCompletions({
        ...requestDefaults,
        currentToken,
        tokens: [currentToken],
        currentTokenIdx: 0,
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
        error: undefined,
      };
      const currentToken = createCurrentToken('term', 'PSOT', 1, 12);
      const lastToken = {
        type: 'keyword',
        value: 'http_method:',
      };
      asMock(SearchSuggestions.suggestFieldValue).mockResolvedValue(response);

      const completer = new FieldValueCompletion();

      const suggestions = await completer.getCompletions({
        ...requestDefaults,
        currentToken,
        lastToken,
        prefix: 'PSOT',
        tokens: [lastToken, currentToken],
        currentTokenIdx: 1,
      });

      const expectedCorrections = [
        { name: 'POST', value: 'POST', caption: 'POST ⭢ PSOT', score: 300, meta: '300 hits' },
      ];

      expect(suggestions).toEqual(expectedCorrections);
    });

    it('escapes value for suggestions correctly', async () => {
      const response = {
        field: 'process',
        input: '',
        sum_other_docs_count: 0,
        suggestions: [
          { value: 'C:\\Windows\\System32\\lsass.exe', occurrence: 300 },
        ],
        error: undefined,
      };
      const currentToken = createCurrentToken('term', '', 1, 12);
      const lastToken = {
        type: 'keyword',
        value: 'process:',
      };
      asMock(SearchSuggestions.suggestFieldValue).mockResolvedValue(response);

      const completer = new FieldValueCompletion();

      const suggestions = await completer.getCompletions({
        ...requestDefaults,
        currentToken,
        lastToken,
        tokens: [lastToken, currentToken],
        currentTokenIdx: 1,
      });

      expect(suggestions).toEqual([
        {
          name: 'C:\\Windows\\System32\\lsass.exe',
          value: 'C\\:\\\\Windows\\\\System32\\\\lsass.exe',
          caption: 'C\\:\\\\Windows\\\\System32\\\\lsass.exe',
          score: 300,
          meta: '300 hits',
        },
      ]);
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
        error: undefined,
      };

      const expectedFirstSuggestions = [
        { name: 'action1', value: 'action1', caption: 'action1', score: 400, meta: '400 hits' },
        { name: 'action2', value: 'action2', caption: 'action2', score: 300, meta: '300 hits' },
      ];

      it('is fetching further suggestions when there are some', async () => {
        asMock(SearchSuggestions.suggestFieldValue).mockResolvedValue(firstResponse);

        const completer = new FieldValueCompletion();

        const firstSuggestions = await completer.getCompletions({
          ...requestDefaults,
          currentToken,
          lastToken,
          prefix: 'a',
          tokens: [lastToken, currentToken],
          currentTokenIdx: 1,
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
          error: undefined,
        };
        asMock(SearchSuggestions.suggestFieldValue).mockResolvedValue(secondResponse);

        const secondSuggestions = await completer.getCompletions({
          ...requestDefaults,
          currentToken,
          lastToken,
          prefix: 'ac',
          tokens: [lastToken, currentToken],
          currentTokenIdx: 1,
        });

        expect(secondSuggestions).toEqual([
          { name: 'action3', value: 'action3', caption: 'action3', score: 200, meta: '200 hits' },
          { name: 'action4', value: 'action4', caption: 'action4', score: 100, meta: '100 hits' },
        ]);
      });

      it('is not fetching further suggestions when there are none', async () => {
        asMock(SearchSuggestions.suggestFieldValue).mockResolvedValue({ ...firstResponse, sum_other_docs_count: 0 });

        const completer = new FieldValueCompletion();

        const firstSuggestions = await completer.getCompletions({
          ...requestDefaults,
          currentToken,
          lastToken,
          prefix: 'a',
          tokens: [lastToken, currentToken],
          currentTokenIdx: 1,
        });

        expect(firstSuggestions).toEqual(expectedFirstSuggestions);

        const secondSuggestions = await completer.getCompletions({
          ...requestDefaults,
          currentToken,
          lastToken,
          prefix: 'ac',
          tokens: [lastToken, currentToken],
          currentTokenIdx: 1,
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

    it('returns true if current token is a string and consits only of ""', async () => {
      const completer = new FieldValueCompletion();
      const result = completer.shouldShowCompletions(1, [[{ type: 'keyword', value: 'http_method:' }, { type: 'string', value: '""', index: 1, start: 12 }, null]]);

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
