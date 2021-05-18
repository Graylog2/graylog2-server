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
import { StoreMock as MockStore } from 'helpers/mocking';
import userEvent from '@testing-library/user-event';
import { exampleFormDataWithKeySecretAuth } from 'aws/FormData.fixtures';

import history from 'util/History';
import { FormDataProvider } from 'aws/context/FormData';
import { ApiContext } from 'aws/context/Api';
import { StepsContext } from 'aws/context/Steps';
import { SidebarContext } from 'aws/context/Sidebar';
import Routes from 'routing/Routes';

import CloudWatch from './CloudWatch';

const mockCurrentUser = { currentUser: { fullname: 'Ada Lovelace', username: 'ada' } };
jest.mock('injection/StoreProvider', () => ({ getStore: () => MockStore('get', 'listen') }));

jest.mock('stores/users/CurrentUserStore', () => MockStore(
  ['get', () => mockCurrentUser],
  ['getInitialState', () => mockCurrentUser],
));

jest.mock('util/History');

// eslint-disable-next-line react/prop-types
const TestCommonProviders = ({ children }) => (
  <ApiContext.Provider value={{
    availableStreams: [],
  }}>
    <StepsContext.Provider value={{
      availableSteps: ['review'],
      currentStep: 'review',
      isDisabledStep: () => false,
    }}>
      <FormDataProvider initialFormData={exampleFormDataWithKeySecretAuth}>
        <SidebarContext.Provider value={{
          sidebar: <></>,
          clearSidebar: jest.fn(),
        }}>
          {children}
        </SidebarContext.Provider>
      </FormDataProvider>
    </StepsContext.Provider>
  </ApiContext.Provider>
);

describe('<CloudWatch>', () => {
  it('redirects to system/inputs after input is created', async () => {
    render(
      <TestCommonProviders>
        <CloudWatch />
      </TestCommonProviders>,
    );

    const submitButton = screen.getByRole('button', { name: /complete cloudWatch setup/i });

    expect(submitButton).toBeInTheDocument();

    userEvent.click(submitButton);

    await waitFor(() => expect(history.push).toHaveBeenCalledTimes(1));

    expect(history.push).toHaveBeenCalledWith(Routes.SYSTEM.INPUTS);
  });

  it('calls onSubmit when input is submitted externally', async () => {
    const submitFunction = jest.fn();

    render(
      <TestCommonProviders>
        <CloudWatch onSubmit={submitFunction} externalInputSubmit />
      </TestCommonProviders>,
    );

    const submitButton = screen.getByRole('button', { name: /complete cloudWatch setup/i });

    expect(submitButton).toBeInTheDocument();

    userEvent.click(submitButton);

    await waitFor(() => expect(submitFunction).toHaveBeenCalledTimes(1));

    expect(submitFunction).toHaveBeenCalledWith(exampleFormDataWithKeySecretAuth);
  });
});
