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
import * as React from 'react';
import { render, screen, waitFor } from 'wrappedTestingLibrary';
import * as Immutable from 'immutable';
import userEvent from '@testing-library/user-event';

import { execute } from 'views/logic/slices/searchExecutionSlice';
import asMock from 'helpers/mocking/AsMock';
import { simpleFields, simpleQueryFields } from 'fixtures/fields';
import useCurrentQuery from 'views/logic/queries/useCurrentQuery';
import Query, { filtersForQuery } from 'views/logic/queries/Query';
import useFieldTypes from 'views/logic/fieldtypes/useFieldTypes';
import type { SearchExecutionResult } from 'views/types';
import TestStoreProvider from 'views/test/TestStoreProvider';
import useViewsPlugin from 'views/test/testViewsPlugin';
import useAppDispatch from 'stores/useAppDispatch';
import executeSearch from 'views/logic/slices/executeJobResult';
import generateId from 'logic/generateId';

import type { FieldTypes } from './FieldTypesContext';
import FieldTypesContext from './FieldTypesContext';
import DefaultFieldTypesProvider from './DefaultFieldTypesProvider';

jest.mock('views/logic/queries/useCurrentQuery');
jest.mock('views/logic/fieldtypes/useFieldTypes', () => jest.fn());
jest.mock('views/logic/slices/executeJobResult');

const refetch = () => {};

describe('DefaultFieldTypesProvider', () => {
  const renderSUT = (consume: (value: FieldTypes) => React.ReactNode = jest.fn()) => {
    render((
      <TestStoreProvider>
        <DefaultFieldTypesProvider>
          <FieldTypesContext.Consumer>
            {consume}
          </FieldTypesContext.Consumer>
        </DefaultFieldTypesProvider>
      </TestStoreProvider>
    ));

    return consume;
  };

  useViewsPlugin();

  it('provides no field types with empty store', () => {
    asMock(useCurrentQuery).mockReturnValue(Query.builder().id('foobar').build());
    asMock(useFieldTypes).mockReturnValue({ data: undefined, refetch });

    const consume = renderSUT();

    expect(consume).toHaveBeenCalledWith({ all: Immutable.List(), queryFields: Immutable.Map({ foobar: Immutable.List() }) });
  });

  it('provides field types of field types store', () => {
    asMock(useCurrentQuery).mockReturnValue(Query.builder()
      .id('queryId')
      .filter(filtersForQuery(['dummyStream']))
      .build());

    asMock(useFieldTypes).mockImplementation((streams) => (streams.length === 0
      ? { data: simpleFields().toArray(), refetch }
      : { data: simpleQueryFields('foo').get('foo').toArray(), refetch }));

    const consume = renderSUT();

    const fieldTypes = { all: simpleFields(), queryFields: simpleQueryFields('queryId') };

    expect(consume).toHaveBeenCalledWith(fieldTypes);
  });

  it('refetches field types upon search refresh', async () => {
    asMock(useCurrentQuery).mockReturnValue(Query.builder().id('foobar').build());
    const refetchMock = jest.fn();

    asMock(useFieldTypes).mockImplementation((streams) => (streams.length === 0
      ? { data: simpleFields().toArray(), refetch: refetchMock }
      : { data: simpleQueryFields('foo').get('foo').toArray(), refetch: refetchMock }));

    const TriggerRefresh = () => {
      const dispatch = useAppDispatch();

      return <button type="button" onClick={() => dispatch(execute())}>Refresh search</button>;
    };

    asMock(executeSearch).mockResolvedValue({ result: { result: { id: generateId() } } } as SearchExecutionResult);

    const consume = () => <TriggerRefresh />;

    renderSUT(consume);

    const button = await screen.findByRole('button', { name: /refresh search/i });
    userEvent.click(button);

    await waitFor(() => {
      expect(refetchMock).toHaveBeenCalledTimes(2);
    });
  });
});
