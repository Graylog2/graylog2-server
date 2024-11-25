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
import { render, screen, fireEvent } from 'wrappedTestingLibrary';

import { Button } from 'components/bootstrap';
import { asMock } from 'helpers/mocking';
import mockAction from 'helpers/mocking/MockAction';
import { StreamsActions } from 'stores/streams/StreamsStore';
import usePipelinesConnectedStream from 'hooks/usePipelinesConnectedStream';
import { useInputSetupWizard, InputSetupWizardProvider } from 'components/inputs/InputSetupWizard';
import type { WizardData } from 'components/inputs/InputSetupWizard';

import InputSetupWizard from './InputSetupWizard';

const OpenWizardTestButton = ({ wizardData } : { wizardData: WizardData}) => {
  const { openWizard } = useInputSetupWizard();

  return (<Button onClick={() => openWizard(wizardData)}>Open Wizard!</Button>);
};

const CloseWizardTestButton = () => {
  const { closeWizard } = useInputSetupWizard();

  return (<Button onClick={closeWizard}>Close Wizard!</Button>);
};

const renderWizard = (wizardData: WizardData = {}) => (
  render(
    <InputSetupWizardProvider>
      <OpenWizardTestButton wizardData={wizardData} />
      <CloseWizardTestButton />
      <InputSetupWizard />
    </InputSetupWizardProvider>,
  )
);

jest.mock('views/stores/StreamsStore');

jest.mock('hooks/usePipelinesConnectedStream');

const pipelinesConnectedMock = (response = []) => ({
  data: response,
  refetch: jest.fn(),
  isInitialLoading: false,
  error: undefined,
  isError: false,
});

beforeEach(() => {
  asMock(usePipelinesConnectedStream).mockReturnValue(pipelinesConnectedMock());
  StreamsActions.listStreams = mockAction(jest.fn(() => Promise.resolve([])));
});

describe('InputSetupWizard', () => {
  it('renders the wizard and shows routing step as first step', async () => {
    renderWizard();

    const openButton = await screen.findByRole('button', { name: /Open Wizard!/ });

    fireEvent.click(openButton);

    const wizard = await screen.findByText('Setup Routing');

    expect(wizard).toBeInTheDocument();
  });

  it('closes the wizard', async () => {
    renderWizard();
    const openButton = await screen.findByRole('button', { name: /Open Wizard!/ });
    const closeButton = await screen.findByRole('button', { name: /Close Wizard!/ });

    fireEvent.click(openButton);

    const wizard = await screen.findByText('Setup Routing');

    expect(wizard).toBeInTheDocument();

    fireEvent.click(closeButton);

    expect(wizard).not.toBeInTheDocument();
  });
});
