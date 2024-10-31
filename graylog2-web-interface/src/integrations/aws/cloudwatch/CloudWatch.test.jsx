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
import userEvent from '@testing-library/user-event';

import { exampleFormDataWithKeySecretAuth } from 'fixtures/aws/FormData.fixtures';
import { FormDataProvider } from 'integrations/aws/context/FormData';
import { ApiContext } from 'integrations/aws/context/Api';
import { StepsContext } from 'integrations/aws/context/Steps';
import { SidebarContext } from 'integrations/aws/context/Sidebar';
import Routes from 'routing/Routes';

import CloudWatch from './CloudWatch';

const mockNavigate = jest.fn();

jest.mock('react-router-dom', () => {
  const original = jest.requireActual('react-router-dom');

  return {
    __esModule: true,
    ...original,
    useNavigate: jest.fn(() => mockNavigate),
  };
});

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

    await userEvent.click(submitButton);

    await waitFor(() => expect(mockNavigate).toHaveBeenCalledTimes(1));

    expect(mockNavigate).toHaveBeenCalledWith(Routes.SYSTEM.INPUTS);
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

    await userEvent.click(submitButton);

    await waitFor(() => expect(submitFunction).toHaveBeenCalledTimes(1));

    expect(submitFunction).toHaveBeenCalledWith(exampleFormDataWithKeySecretAuth);
  });
});
