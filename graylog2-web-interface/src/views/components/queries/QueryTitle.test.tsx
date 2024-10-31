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
import userEvent from '@testing-library/user-event';

import TestStoreProvider from 'views/test/TestStoreProvider';
import useViewsPlugin from 'views/test/testViewsPlugin';
import { asMock } from 'helpers/mocking';
import createSearch from 'views/logic/slices/createSearch';
import useAppSelector from 'stores/useAppSelector';
import { selectViewStates } from 'views/logic/slices/viewSelectors';
import useActiveQueryId from 'views/hooks/useActiveQueryId';

import QueryTitle from './QueryTitle';

jest.mock('views/logic/slices/createSearch');

const QueryCount = () => {
  const queries = useAppSelector(selectViewStates);
  const activeQuery = useActiveQueryId();

  return (
    <>
      <span>Query count: {queries.size}</span>
      <span>Active query: {activeQuery}</span>
    </>
  );
};

describe('QueryTitle', () => {
  beforeEach(() => {
    asMock(createSearch).mockImplementation(async (s) => s);
  });

  const clickQueryAction = async (name: string) => {
    const openMenuTrigger = await screen.findByTestId('query-action-dropdown');

    userEvent.click(openMenuTrigger);

    const menuItem = await screen.findByText(name);

    userEvent.click(menuItem);
  };

  const SUT = (props: Partial<React.ComponentProps<typeof QueryTitle>>) => (
    <TestStoreProvider>
      <QueryTitle active
                  id="query-id-1"
                  openEditModal={() => {}}
                  onRemove={() => Promise.resolve()}
                  title="Foo"
                  openCopyToDashboardModal={() => {}}
                  {...props} />
      <QueryCount />
    </TestStoreProvider>
  );

  useViewsPlugin();

  describe('duplicate action', () => {
    it('triggers duplication of query', async () => {
      render(<SUT />);

      await clickQueryAction('Duplicate');

      await screen.findByText(/query count: 2/i);
    });

    it('does not explicitly select new query after duplicating it', async () => {
      // Selecting the new query after duplication has become unnecessary, as `ViewStore#createQuery` does it already
      render(<SUT />);

      await clickQueryAction('Duplicate');

      await screen.findByText(/active query: query-id-1/i);
    });
  });

  describe('edit title action', () => {
    it('opens edit modal', async () => {
      const openEditModalFn = jest.fn();

      render(<SUT openEditModal={openEditModalFn} />);

      await clickQueryAction('Edit Title');

      await waitFor(() => expect(openEditModalFn).toHaveBeenCalledTimes(1));
    });
  });
});
