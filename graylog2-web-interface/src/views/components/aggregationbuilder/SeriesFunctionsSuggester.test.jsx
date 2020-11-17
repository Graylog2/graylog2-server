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
const AggregationFunctionsStore = {
  getInitialState: jest.fn(() => ({ avg: undefined, min: undefined, max: undefined })),
  listen: jest.fn(),
};

describe('SeriesFunctionsSuggester', () => {
  let SeriesFunctionsSuggester;

  beforeEach(() => {
    jest.doMock('views/stores/AggregationFunctionsStore', () => AggregationFunctionsStore);

    // eslint-disable-next-line global-require
    SeriesFunctionsSuggester = require('./SeriesFunctionsSuggester').default;
  });

  afterEach(() => {
    jest.clearAllMocks();
    jest.resetModules();
  });

  it('returns default functions', () => {
    const suggester = new SeriesFunctionsSuggester();

    expect(suggester.defaults).toMatchSnapshot();
  });

  it('completes functions with field names', () => {
    const suggester = new SeriesFunctionsSuggester(['action', 'controller', 'took_ms']);

    expect(suggester.for('avg')).toMatchSnapshot();
  });

  it('does not complete functions without field names', () => {
    const suggester = new SeriesFunctionsSuggester([]);

    expect(suggester.for('avg')).toEqual([]);
  });

  it('updates functions when triggered', () => {
    const suggester = new SeriesFunctionsSuggester(['action', 'controller', 'took_ms']);

    expect(AggregationFunctionsStore.listen).toHaveBeenCalled();
    expect(AggregationFunctionsStore.getInitialState).toHaveBeenCalled();
    expect(suggester.defaults).toHaveLength(4);
    expect(suggester.defaults).toMatchSnapshot('default functions before update');

    const callback = AggregationFunctionsStore.listen.mock.calls[0][0];

    const newFunctions = { card: undefined, stddev: undefined };

    callback(newFunctions);

    expect(suggester.defaults).toHaveLength(3);
    expect(suggester.defaults).toMatchSnapshot('default functions after update');
  });
});
