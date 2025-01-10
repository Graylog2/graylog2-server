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
import { render, screen } from 'wrappedTestingLibrary';

import { asMock } from 'helpers/mocking';
import useStreams from 'components/streams/hooks/useStreams';
import usePipelinesConnectedStream from 'hooks/usePipelinesConnectedStream';

import InputSetupWizardProvider from './contexts/InputSetupWizardProvider';
import InputSetupWizard from './Wizard';

const input = {
  id: 'inputId',
  title: 'inputTitle',
  type: 'type',
  global: false,
  name: 'inputName',
  created_at: '',
  creator_user_id: 'creatorId',
  static_fields: { },
  attributes: { },
};

const onClose = jest.fn();

const renderWizard = () => (
  render(
    <InputSetupWizardProvider>
      <InputSetupWizard show input={input} onClose={onClose} />
    </InputSetupWizardProvider>,
  )
);

jest.mock('components/streams/hooks/useStreams');
jest.mock('hooks/usePipelinesConnectedStream');

const useStreamsResult = (list = []) => ({
  data: { list: list, pagination: { total: 1 }, attributes: [] },
  isInitialLoading: false,
  isFetching: false,
  error: undefined,
  refetch: () => {},
});

const pipelinesConnectedMock = (response = []) => ({
  data: response,
  refetch: jest.fn(),
  isInitialLoading: false,
  error: undefined,
  isError: false,
});

beforeEach(() => {
  asMock(useStreams).mockReturnValue(useStreamsResult());
  asMock(usePipelinesConnectedStream).mockReturnValue(pipelinesConnectedMock());
});

describe('InputSetupWizard', () => {
  it('should render the wizard and shows routing step as first step', async () => {
    renderWizard();

    const wizard = await screen.findByText('Setup Routing');

    expect(wizard).toBeInTheDocument();
  });
});
