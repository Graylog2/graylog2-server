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
import SearchBarAutoCompletions, { Completer } from './SearchBarAutocompletions';

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
  getCompletions = () => ([sourceIpCompletion]);
}

class AsyncCompleter implements Completer {
  getCompletions = () => {
    return Promise.resolve([sourceCompletion]);
  }
}

const EditorMock = {
  completer: {},
  session: {
    getTokens: () => ([{ type: 'term', value: 's', index: 0, start: 0 }]),
    getTokenAt: () => ({ type: 'term', value: 's', index: 0, start: 0 }),
  },
};

describe('SearchAutoCompletions', () => {
  it('should return completions based on provided Completers', async () => {
    const searchBarAutoCompletions = new SearchBarAutoCompletions([new SimpleCompleter()]);

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
    const searchBarAutoCompletions = new SearchBarAutoCompletions([new SimpleCompleter(), new AsyncCompleter()]);

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
