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
import { MISSING_BUCKET_NAME } from 'views/Constants';
import FieldType from 'views/logic/fieldtypes/FieldType';
import Query from 'views/logic/queries/Query';
import View from 'views/logic/views/View';
import { createSearch } from 'fixtures/searches';
import mockDispatch from 'views/test/mockDispatch';
import SearchExecutionState from 'views/logic/search/SearchExecutionState';
import { updateQueryString } from 'views/logic/slices/viewSlice';
import type { RootState } from 'views/types';

import { addValuesToQuery, anyValueClause, excludeValuesFromQuery } from './MultipleValuesQueryHandler';

jest.mock('views/logic/slices/viewSlice', () => ({
  ...jest.requireActual('views/logic/slices/viewSlice'),
  updateQueryString: jest.fn(() => Promise.resolve()),
}));

const keywordType = new FieldType('keyword', [], []);
const dateType = new FieldType('date', [], []);

describe('MultipleValuesQueryHandler', () => {
  describe('anyValueClause', () => {
    it('creates simple predicate for single value', () => {
      expect(anyValueClause('source', ['host1'], keywordType)).toBe('source:host1');
    });

    it('combines multiple values with OR', () => {
      expect(anyValueClause('source', ['host1', 'host2', 42], keywordType)).toBe('source:(host1 OR host2 OR 42)');
    });

    it('escapes values', () => {
      expect(anyValueClause('path', ['/var/log', 'with space'], keywordType)).toBe(
        'path:(\\/var\\/log OR "with space")',
      );
    });

    it('formats date values', () => {
      expect(anyValueClause('timestamp', ['2019-01-17T11:00:09.025Z'], dateType)).toBe(
        'timestamp:"2019-01-17T11:00:09.025Z"',
      );
    });

    it('matches missing bucket with non-existing field', () => {
      expect(anyValueClause('source', [MISSING_BUCKET_NAME], keywordType)).toBe('NOT _exists_:source');
      expect(anyValueClause('source', ['host1', MISSING_BUCKET_NAME, 'host2'], keywordType)).toBe(
        '(source:(host1 OR host2) OR NOT _exists_:source)',
      );
    });
  });

  describe('query update', () => {
    const createState = (queryString: string) => {
      const query = Query.builder()
        .id('query-id')
        .query({ type: 'elasticsearch', query_string: queryString })
        .build();
      const defaultView = createSearch();
      const view = defaultView
        .toBuilder()
        .type(View.Type.Search)
        .search(defaultView.search.toBuilder().queries([query]).build())
        .build();

      return { view: { view }, searchExecution: { executionState: SearchExecutionState.empty() } } as RootState;
    };

    beforeEach(() => {
      jest.clearAllMocks();
    });

    it('adds values to empty query', async () => {
      const dispatch = mockDispatch(createState(''));

      await dispatch(
        addValuesToQuery({ queryId: 'query-id', field: 'source', values: ['host1', 'host2'], type: keywordType }),
      );

      expect(updateQueryString).toHaveBeenCalledWith('query-id', 'source:(host1 OR host2)');
    });

    it('adds values to existing query with AND', async () => {
      const dispatch = mockDispatch(createState('action:login'));

      await dispatch(
        addValuesToQuery({ queryId: 'query-id', field: 'source', values: ['host1', 'host2'], type: keywordType }),
      );

      expect(updateQueryString).toHaveBeenCalledWith('query-id', 'action:login AND source:(host1 OR host2)');
    });

    it('excludes values', async () => {
      const dispatch = mockDispatch(createState('action:login'));

      await dispatch(
        excludeValuesFromQuery({ queryId: 'query-id', field: 'source', values: ['host1', 'host2'], type: keywordType }),
      );

      expect(updateQueryString).toHaveBeenCalledWith('query-id', 'action:login AND NOT source:(host1 OR host2)');
    });

    it('excludes missing bucket by requiring the field', async () => {
      const dispatch = mockDispatch(createState(''));

      await dispatch(
        excludeValuesFromQuery({
          queryId: 'query-id',
          field: 'source',
          values: [MISSING_BUCKET_NAME],
          type: keywordType,
        }),
      );

      expect(updateQueryString).toHaveBeenCalledWith('query-id', '_exists_:source');
    });
  });
});
