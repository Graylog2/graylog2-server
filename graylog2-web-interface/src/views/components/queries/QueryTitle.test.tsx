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
import * as Immutable from 'immutable';
import { render, screen, waitFor } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import mockAction from 'helpers/mocking/MockAction';
import { QueriesActions } from 'views/stores/QueriesStore';
import { ViewActions } from 'views/stores/ViewStore';

import QueryTitle from './QueryTitle';

jest.mock('views/stores/QueriesStore', () => ({ QueriesActions: {} }));
jest.mock('views/stores/ViewStore', () => ({ ViewActions: {} }));

describe('QueryTitle', () => {
  beforeEach(() => {
    QueriesActions.duplicate = mockAction(jest.fn(() => Promise.resolve(Immutable.OrderedMap())));
    ViewActions.selectQuery = mockAction(jest.fn((queryId) => Promise.resolve(queryId)));
  });

  const clickQueryAction = async (name: string) => {
    const openMenuTrigger = await screen.findByTestId('query-action-dropdown');

    userEvent.click(openMenuTrigger);

    const menuItem = await screen.findByText(name);

    userEvent.click(menuItem);
  };

  describe('duplicate action', () => {
    it('triggers duplication of query', async () => {
      render(
        <QueryTitle active
                    id="deadbeef"
                    openEditModal={() => {}}
                    onClose={() => Promise.resolve()}
                    title="Foo" />,
      );

      await clickQueryAction('Duplicate');

      await waitFor(() => expect(QueriesActions.duplicate).toHaveBeenCalled());
    });

    it('does not explicitly select new query after duplicating it', async () => {
      // Selecting the new query after duplication has become unnecessary, as `ViewStore#createQuery` does it already
      render(
        <QueryTitle active
                    id="deadbeef"
                    openEditModal={() => {}}
                    onClose={() => Promise.resolve()}
                    title="Foo" />,
      );

      await clickQueryAction('Duplicate');

      expect(ViewActions.selectQuery).not.toHaveBeenCalled();
    });
  });

  describe('edit title action', () => {
    it('opens edit modal', async () => {
      const openEditModalFn = jest.fn();

      render(
        <QueryTitle active
                    id="deadbeef"
                    openEditModal={openEditModalFn}
                    onClose={() => Promise.resolve()}
                    title="Foo" />,
      );

      await clickQueryAction('Edit Title');

      await waitFor(() => expect(openEditModalFn).toHaveBeenCalledTimes(1));
    });
  });
});
