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
import { DEFAULT_TIMERANGE } from 'views/Constants';

import type { Completer } from './SearchBarAutocompletions';
import SearchBarAutoCompletions from './SearchBarAutocompletions';

const sourceIpCompletion = {
  exactMatch: 0,
  matchMask: 64,
  meta: 'string',
  name: 'source_ip',
  score: 1,
  value: 'source_ip:',
};

const sourceCompletion = {
  exactMatch: 0,
  matchMask: 65,
  meta: 'string',
  name: 'source',
  score: 2,
  value: 'source:',
};

class SimpleCompleter implements Completer {
  // eslint-disable-next-line class-methods-use-this
  getCompletions = () => ([sourceIpCompletion]);
}

class AsyncCompleter implements Completer {
  // eslint-disable-next-line class-methods-use-this
  getCompletions = () => Promise.resolve([sourceCompletion]);
}

const EditorMock = {
  completer: {},
  session: {
    getTokens: () => ([{ type: 'term', value: 's', index: 0, start: 0 }]),
    getTokenAt: () => ({ type: 'term', value: 's', index: 0, start: 0 }),
  },
};

const EMPTY_FIELDTYPES = { all: {}, query: {} };

describe('SearchAutoCompletions', () => {
  describe('getCompletions', () => {
    it('should return completions based on provided Completers', async () => {
      const searchBarAutoCompletions = new SearchBarAutoCompletions([new SimpleCompleter()], DEFAULT_TIMERANGE, [], EMPTY_FIELDTYPES, 'Europe/Berlin');

      const callback = jest.fn();

      await searchBarAutoCompletions.getCompletions(
        // @ts-ignore
        EditorMock,
        {},
        { row: 0, column: 1 },
        's',
        callback,
      );

      expect(callback).toHaveBeenCalledWith(null, [sourceIpCompletion]);
    });

    it('should support Completers which provide the completions asynchronously', async () => {
      const searchBarAutoCompletions = new SearchBarAutoCompletions([new SimpleCompleter(), new AsyncCompleter()], DEFAULT_TIMERANGE, [], EMPTY_FIELDTYPES, 'Europe/Berlin');

      const callback = jest.fn();

      await searchBarAutoCompletions.getCompletions(
        // @ts-ignore
        EditorMock,
        {},
        { row: 0, column: 1 },
        's',
        callback,
      );

      expect(callback).toHaveBeenCalledWith(null, [sourceIpCompletion, sourceCompletion]);
    });
  });

  describe('shouldShowCompletions', () => {
    it('should not show completions manually when no Completer provides `shouldShowCompletions`', async () => {
      const searchBarAutoCompletions = new SearchBarAutoCompletions([new SimpleCompleter()], DEFAULT_TIMERANGE, [], EMPTY_FIELDTYPES, 'Europe/Berlin');

      const result = searchBarAutoCompletions.shouldShowCompletions(1, [[{
        type: 'keyword',
        value: 'http_method:',
        index: 0,
        start: 0,
      }], null]);

      expect(result).toBe(false);
    });

    it('should not show completions manually when a Completer provides `shouldShowCompletions` which returns false', async () => {
      class ExampleCompleter implements Completer {
        // eslint-disable-next-line class-methods-use-this
        getCompletions = () => ([sourceIpCompletion]);

        // eslint-disable-next-line class-methods-use-this
        shouldShowCompletions = () => false;
      }

      const searchBarAutoCompletions = new SearchBarAutoCompletions([new ExampleCompleter()], DEFAULT_TIMERANGE, [], EMPTY_FIELDTYPES, 'Europe/Berlin');
      const result = searchBarAutoCompletions.shouldShowCompletions(1, [[{
        type: 'keyword',
        value: 'http_method:',
        index: 0,
        start: 0,
      }], null]);

      expect(result).toBe(false);
    });

    it('should consider a Completer when deciding if it should show completions', async () => {
      const mockShouldShowCompletions = jest.fn(() => true);

      class ExampleCompleter implements Completer {
        // eslint-disable-next-line class-methods-use-this
        getCompletions = () => ([sourceIpCompletion]);

        shouldShowCompletions = mockShouldShowCompletions;
      }

      const searchBarAutoCompletions = new SearchBarAutoCompletions([new ExampleCompleter()], DEFAULT_TIMERANGE, [], EMPTY_FIELDTYPES, 'Europe/Berlin');
      const result = searchBarAutoCompletions.shouldShowCompletions(1, [[{
        type: 'keyword',
        value: 'http_method:',
        index: 0,
        start: 0,
      }], null]);

      expect(mockShouldShowCompletions).toHaveBeenCalledTimes(1);

      expect(mockShouldShowCompletions).toHaveBeenCalledWith(1, [[{
        type: 'keyword',
        value: 'http_method:',
        index: 0,
        start: 0,
      }], null]);

      expect(result).toBe(true);
    });
  });
});
