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
import selectEvent from 'react-select-event';

import { Button } from 'components/bootstrap';
import { asMock } from 'helpers/mocking';
import usePipelinesConnectedStream from 'hooks/usePipelinesConnectedStream';
import { useInputSetupWizard, InputSetupWizardProvider, INPUT_WIZARD_STEPS } from 'components/inputs/InputSetupWizard';
import type { WizardData } from 'components/inputs/InputSetupWizard';
import useStreams from 'components/streams/hooks/useStreams';

import InputSetupWizard from './InputSetupWizard';

const OpenWizardTestButton = ({ wizardData } : { wizardData: WizardData}) => {
  const { openWizard, setActiveStep } = useInputSetupWizard();

  const open = () => {
    setActiveStep(INPUT_WIZARD_STEPS.SETUP_ROUTING);
    openWizard(wizardData);
  };

  return (<Button onClick={() => open()}>Open Wizard!</Button>);
};

const renderWizard = (wizardData: WizardData = {}) => (
  render(
    <InputSetupWizardProvider>
      <OpenWizardTestButton wizardData={wizardData} />
      <InputSetupWizard />
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

describe('InputSetupWizard Setup Routing', () => {
  it('should render the Setup Routing step', async () => {
    renderWizard();

    const openButton = await screen.findByRole('button', { name: /Open Wizard!/ });

    fireEvent.click(openButton);

    const wizard = await screen.findByText('Setup Routing');

    expect(wizard).toBeInTheDocument();
  });

  it('should only show editable existing streams', async () => {
    asMock(useStreams).mockReturnValue(useStreamsResult(
      [
        { id: 'alohoid', title: 'Aloho', is_editable: true },
        { id: 'moraid', title: 'Mora', is_editable: false },
      ],
    ));

    renderWizard();

    const openButton = await screen.findByRole('button', { name: /Open Wizard!/ });

    fireEvent.click(openButton);

    const streamSelect = await screen.findByLabelText(/All messages \(Default\)/i);

    await selectEvent.openMenu(streamSelect);

    const alohoOption = await screen.findByText(/Aloho/i);
    const moraOption = screen.queryByText(/Mora/i);

    expect(alohoOption).toBeInTheDocument();
    expect(moraOption).not.toBeInTheDocument();
  });

  it('should not show existing default stream in select', async () => {
    asMock(useStreams).mockReturnValue(useStreamsResult(
      [
        { id: 'alohoid', title: 'Aloho', is_editable: true, is_default: true },
        { id: 'moraid', title: 'Mora', is_editable: true },
      ],
    ));

    renderWizard();

    const openButton = await screen.findByRole('button', { name: /Open Wizard!/ });

    fireEvent.click(openButton);

    const streamSelect = await screen.findByLabelText(/All messages \(Default\)/i);

    await selectEvent.openMenu(streamSelect);

    const moraOption = await screen.findByText(/Mora/i);
    const alohoOption = screen.queryByText(/Aloho/i);

    expect(moraOption).toBeInTheDocument();
    expect(alohoOption).not.toBeInTheDocument();
  });

  it('should allow the user to select a stream', async () => {
    asMock(useStreams).mockReturnValue(useStreamsResult(
      [
        { id: 'alohoid', title: 'Aloho', is_editable: true },
        { id: 'moraid', title: 'Mora', is_editable: true },
      ],
    ));

    renderWizard();

    const openButton = await screen.findByRole('button', { name: /Open Wizard!/ });

    fireEvent.click(openButton);

    const streamSelect = await screen.findByLabelText(/All messages \(Default\)/i);

    await selectEvent.openMenu(streamSelect);

    await selectEvent.select(streamSelect, 'Aloho');
  });

  it('should show a warning if the selected stream has connected pipelines', async () => {
    asMock(useStreams).mockReturnValue(useStreamsResult(
      [
        { id: 'alohoid', title: 'Aloho', is_editable: true },
        { id: 'moraid', title: 'Mora', is_editable: true },
      ],
    ));

    asMock(usePipelinesConnectedStream).mockReturnValue(pipelinesConnectedMock([
      { id: 'pipeline1', title: 'Pipeline1' },
      { id: 'pipeline2', title: 'Pipeline2' },
    ]));

    renderWizard();

    const openButton = await screen.findByRole('button', { name: /Open Wizard!/ });

    fireEvent.click(openButton);

    const streamSelect = await screen.findByLabelText(/All messages \(Default\)/i);

    await selectEvent.openMenu(streamSelect);

    await selectEvent.select(streamSelect, 'Aloho');

    const warning = await screen.findByText(/The selected stream has existing pipelines/i);
    const warningPipeline1 = await screen.findByText(/Pipeline1/i);
    const warningPipeline2 = await screen.findByText(/Pipeline2/i);

    expect(warning).toBeInTheDocument();
    expect(warningPipeline1).toBeInTheDocument();
    expect(warningPipeline2).toBeInTheDocument();
  });

  it('should allow the user to create a new stream', async () => {
    renderWizard();

    const openButton = await screen.findByRole('button', { name: /Open Wizard!/i });

    fireEvent.click(openButton);

    const createStreamButton = await screen.findByRole('button', {
      name: /Create Stream/i,
      hidden: true,
    });
    fireEvent.click(createStreamButton);

    const createStreamHeadline = await screen.findByRole('heading', { name: /Create new stream/i, hidden: true });

    expect(createStreamHeadline).toBeInTheDocument();
  });
});
