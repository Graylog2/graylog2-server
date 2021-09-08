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
import { fireEvent, within } from '@testing-library/react';
import { List as MockList } from 'immutable';
import { render, screen } from 'wrappedTestingLibrary';
import { MockStore, asMock } from 'helpers/mocking';
import { adminUser } from 'fixtures/users';
import MockAction from 'helpers/mocking/MockAction';

import ViewManagementPage from 'views/pages/ViewManagementPage';
import { ViewManagementActions, ViewManagementStoreState } from 'views/stores/ViewManagementStore';
import CurrentUserContext from 'contexts/CurrentUserContext';

jest.mock('routing/Routes', () => ({
  VIEWS: '/views',
  EXTENDEDSEARCH: '/extendedsearch',
  pluginRoute: jest.fn(() => () => '/mockroute'),
}));

jest.mock('views/stores/ViewManagementStore', () => ({
  ViewManagementStore: MockStore(['getInitialState', () => ({
    list: [{
      id: 'foobar',
      type: 'DASHBOARD',
      title: 'A View',
      owner: 'franz',
      properties: MockList(),
      created_at: '2021-09-08T08:13:00Z' as unknown as Date,
      requires: {},
      description: 'This is my very awesome dashboard',
      search_id: 'deadbeef',
      state: {},
      summary: 'This summary tells more about the dashboard',

    }],
    pagination: {
      total: 1,
      page: 1,
      perPage: 1,
      query: '',
    },
  } as ViewManagementStoreState)]),
  ViewManagementActions: {
    search: MockAction(),
    delete: MockAction(),
  },
}));

describe('ViewManagementPage', () => {
  let oldConfirm;

  beforeEach(() => {
    oldConfirm = window.confirm;
    window.confirm = jest.fn(() => false);
  });

  afterEach(() => {
    window.confirm = oldConfirm;
  });

  it('renders list of views', async () => {
    render(<ViewManagementPage />);

    await screen.findByRole('link', { name: 'A View' });
  });

  it('asks for confirmation when deleting view', async () => {
    render((
      <CurrentUserContext.Provider value={adminUser}>
        <ViewManagementPage />
      </CurrentUserContext.Provider>
    ));

    const actionsContainer = await screen.findByTestId('actions-container');
    fireEvent.click(await within(actionsContainer).findByRole('button', { name: 'Actions' }));
    fireEvent.click(await within(actionsContainer).findByText('Delete'));

    expect(window.confirm).toHaveBeenCalledWith('Are you sure you want to delete "A View"?');
    expect(ViewManagementActions.delete).not.toHaveBeenCalled();

    asMock(window.confirm).mockReturnValue(true);

    fireEvent.click(await within(actionsContainer).findByRole('button', { name: 'Actions' }));
    fireEvent.click(await within(actionsContainer).findByText('Delete'));

    expect(ViewManagementActions.delete).toHaveBeenCalledWith(expect.objectContaining({ id: 'foobar' }));
  });
});
