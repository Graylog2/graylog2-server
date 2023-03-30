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
import React from 'react';
import { render, screen, act, waitFor } from 'wrappedTestingLibrary';
import selectEvent from 'react-select-event';

import mockAction from 'helpers/mocking/MockAction';
import CurrentUserContext from 'contexts/CurrentUserContext';
import { adminUser } from 'fixtures/users';
import { paginatedUsers } from 'fixtures/userOverviews';
import UsersDomain from 'domainActions/users/UsersDomain';

import UsersSelectField from './UsersSelectField';

jest.useFakeTimers();
const mockLoadUserPaginatedPromise = Promise.resolve(paginatedUsers);

describe('<UsersSelectField>', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  const onChange = jest.fn();
  const value = 'Baker Owens,Wiggins Vicki,tester,Abbott Schneider';

  const renderUSF = (values) => {
    return render(
      <CurrentUserContext.Provider value={adminUser}>
        <UsersSelectField value={values} onChange={onChange} />
      </CurrentUserContext.Provider>,
    );
  };

  it('should render UserSelectField', async () => {
    UsersDomain.loadUsersPaginated = mockAction(jest.fn(() => mockLoadUserPaginatedPromise));
    renderUSF(value);
    await act(() => mockLoadUserPaginatedPromise.then());

    const valueText = screen.getByText('Wiggins Vicki');

    expect(valueText).toBeInTheDocument();
  });

  it('should call onChange UserSelectField', async () => {
    UsersDomain.loadUsersPaginated = mockAction(jest.fn(() => mockLoadUserPaginatedPromise));
    renderUSF('');
    await act(() => mockLoadUserPaginatedPromise.then());

    const select = screen.getByText(/select user\(s\)\.\.\./i);
    await selectEvent.openMenu(select);

    await selectEvent.select(select, 'admin (Administrator)');

    await waitFor(() => expect(onChange).toHaveBeenCalledWith('admin'));
  });
});
