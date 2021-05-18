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
import { screen, render, waitFor } from 'wrappedTestingLibrary';
import { asMock, StoreMock as MockStore } from 'helpers/mocking';
import userEvent from '@testing-library/user-event';
import { exampleFormDataWithKeySecretAuth } from 'aws/FormData.fixtures';

import fetch from 'logic/rest/FetchProvider';
import { FormDataProvider } from 'aws/context/FormData';
import { ApiContext } from 'aws/context/Api';

import StepReview from './StepReview';

const mockCurrentUser = { currentUser: { fullname: 'Ada Lovelace', username: 'ada' } };
jest.mock('injection/StoreProvider', () => ({ getStore: () => MockStore('get', 'listen') }));

jest.mock('stores/users/CurrentUserStore', () => MockStore(
  ['get', () => mockCurrentUser],
  ['getInitialState', () => mockCurrentUser],
));

jest.mock('logic/rest/FetchProvider', () => jest.fn());

describe('<StepReview>', () => {
  afterEach(() => {
    jest.resetAllMocks();
  });

  it('creates an input as default action', async () => {
    asMock(fetch).mockReturnValueOnce(Promise.resolve({ title: 'Test', configuration: {} }));

    const submitFunction = jest.fn();

    render(
      <ApiContext.Provider value={{ logData: null }}>
        <FormDataProvider initialFormData={exampleFormDataWithKeySecretAuth}>
          <StepReview onSubmit={submitFunction} onEditClick={() => {}} />
        </FormDataProvider>
      </ApiContext.Provider>,
    );

    const submitButton = screen.getByRole('button', { name: /complete cloudWatch setup/i });

    expect(submitButton).toBeInTheDocument();

    userEvent.click(submitButton);

    await waitFor(() => expect(fetch).toHaveBeenCalledTimes(1));
    await waitFor(() => expect(submitFunction).toHaveBeenCalledTimes(1));

    expect(submitFunction).toHaveBeenCalledWith();
  });

  it('calls back to onSubmit with formData if Input is submitted externally', async () => {
    asMock(fetch);
    const submitFunction = jest.fn();

    render(
      <ApiContext.Provider value={{ logData: null }}>
        <FormDataProvider initialFormData={exampleFormDataWithKeySecretAuth}>
          <StepReview onSubmit={submitFunction} onEditClick={() => {}} externalInputSubmit />
        </FormDataProvider>
      </ApiContext.Provider>,
    );

    const submitButton = screen.getByRole('button', { name: /complete cloudWatch setup/i });

    expect(submitButton).toBeInTheDocument();

    userEvent.click(submitButton);

    await waitFor(() => expect(submitFunction).toHaveBeenCalledTimes(1));

    expect(submitFunction).toHaveBeenCalledWith(exampleFormDataWithKeySecretAuth);
    expect(fetch).toHaveBeenCalledTimes(0);
  });
});
