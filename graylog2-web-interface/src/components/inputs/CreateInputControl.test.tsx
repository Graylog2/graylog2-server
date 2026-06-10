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
import DefaultQueryClientProvider from 'DefaultQueryClientProvider';

import selectEvent from 'helpers/selectEvent';
import asMock from 'helpers/mocking/AsMock';
import { fetchInputType } from 'hooks/useInputType';
import { createInput } from 'hooks/useInputs';
import Store from 'logic/local-storage/Store';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import useLocation from 'routing/useLocation';
import useFeature from 'hooks/useFeature';
import useInput from 'hooks/useInput';

import useInputTypes from './useInputTypes';
import CreateInputControl, { SETUP_WIZARD_INPUT_ID_KEY } from './CreateInputControl';

jest.mock('./useInputTypes');
jest.mock('hooks/useInputType');
jest.mock('hooks/useInputs');
jest.mock('logic/telemetry/useSendTelemetry');
jest.mock('routing/useLocation');
jest.mock('hooks/useFeature');
jest.mock('hooks/useInput');
jest.mock('components/inputs/InputSetupWizard', () => ({
  __esModule: true,
  InputSetupWizard: ({ input, onClose }: { input: { id: string; title: string }; onClose: () => void }) => (
    <div data-testid="input-setup-wizard">
      <span>InputSetupWizard</span>
      <span>{input.title}</span>
      <span>{input.id}</span>
      <button type="button" onClick={onClose}>
        Close wizard
      </button>
    </div>
  ),
  INPUT_SETUP_MODE_FEATURE_FLAG: 'setup_mode',
}));

const SUT = () => (
  <DefaultQueryClientProvider>
    <CreateInputControl />
  </DefaultQueryClientProvider>
);

const mockInput = {
  id: 'enterprise-input-123',
  title: 'My O365 Input',
  type: 'org.graylog.enterprise.integrations.office365.Office365Input',
  global: true,
  node: null,
  attributes: { pollingInterval: 5 },
  name: 'O365',
  created_at: '2024-01-01T00:00:00Z',
  creator_user_id: 'admin',
  static_fields: {},
};

describe('CreateInputControl', () => {
  beforeEach(() => {
    sessionStorage.clear();
    jest.clearAllMocks();

    asMock(useInputTypes).mockReturnValue({
      'input-type-1': 'Input Type 1',
      'input-type-2': 'Input Type 2',
    });

    asMock(fetchInputType).mockResolvedValue({
      requested_configuration: {},
      description: 'Test input type',
    } as any);

    asMock(createInput).mockResolvedValue({ id: 'input-id-1' });

    asMock(useSendTelemetry).mockReturnValue(jest.fn());
    asMock(useLocation).mockReturnValue({
      pathname: '/system/inputs',
      search: '',
      hash: '',
      state: undefined,
      key: 'mock-location-key',
    });
    asMock(useFeature).mockReturnValue(false);
    asMock(useInput).mockReturnValue({ data: undefined, isLoading: false } as any);
  });

  it('renders select and button', async () => {
    render(<SUT />);

    expect(await selectEvent.findSelectInput('Select input')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /launch new input/i })).toBeInTheDocument();
  });

  it('enables button when input is selected using selectEvent', async () => {
    render(<SUT />);

    await selectEvent.chooseOption('select input', 'Input Type 1');
    expect(screen.getByRole('button', { name: /launch new input/i })).toBeEnabled();
  });

  it('shows configuration form after selecting input and submitting', async () => {
    render(<SUT />);

    await selectEvent.chooseOption('select input', 'Input Type 1');
    await userEvent.click(screen.getByRole('button', { name: /launch new input/i }));

    await waitFor(() => {
      expect(screen.getByText(/test input type/i)).toBeInTheDocument();
    });
  });

  it('calls createInput when submitting the configuration form', async () => {
    render(<SUT />);

    await selectEvent.chooseOption('select input', 'Input Type 1');
    await userEvent.click(screen.getByRole('button', { name: /launch new input/i }));

    const title = await screen.findByRole('textbox', {
      name: /title/i,
    });
    await userEvent.type(title, 'Test');

    const submitButton = await screen.findByRole('button', { name: /launch input/i });
    await userEvent.click(submitButton);

    await waitFor(() => {
      expect(createInput).toHaveBeenCalled();
    });
  });

  describe('setup wizard from sessionStorage', () => {
    it('opens the wizard when sessionStorage has an input ID and feature flag is enabled', async () => {
      Store.sessionSet(SETUP_WIZARD_INPUT_ID_KEY, 'enterprise-input-123');
      asMock(useFeature).mockReturnValue(true);
      asMock(useInput).mockReturnValue({ data: mockInput, isLoading: false } as any);

      render(<SUT />);

      await screen.findByTestId('input-setup-wizard');

      expect(screen.getByText('InputSetupWizard')).toBeInTheDocument();
    });

    it('does not open the wizard when the feature flag is disabled', () => {
      Store.sessionSet(SETUP_WIZARD_INPUT_ID_KEY, 'enterprise-input-123');
      asMock(useFeature).mockReturnValue(false);
      asMock(useInput).mockReturnValue({ data: mockInput, isLoading: false } as any);

      render(<SUT />);

      expect(screen.queryByTestId('input-setup-wizard')).not.toBeInTheDocument();
    });

    it('does not open the wizard when sessionStorage is empty', () => {
      asMock(useFeature).mockReturnValue(true);

      render(<SUT />);

      expect(screen.queryByTestId('input-setup-wizard')).not.toBeInTheDocument();
    });

    it('does not open the wizard when input data has not loaded yet', () => {
      Store.sessionSet(SETUP_WIZARD_INPUT_ID_KEY, 'enterprise-input-123');
      asMock(useFeature).mockReturnValue(true);
      asMock(useInput).mockReturnValue({ data: undefined, isLoading: true } as any);

      render(<SUT />);

      expect(screen.queryByTestId('input-setup-wizard')).not.toBeInTheDocument();
    });

    it('keeps sessionStorage until the stored input has loaded', () => {
      Store.sessionSet(SETUP_WIZARD_INPUT_ID_KEY, 'enterprise-input-123');
      asMock(useFeature).mockReturnValue(true);
      asMock(useInput).mockReturnValue({ data: undefined, isLoading: true } as any);

      render(<SUT />);

      expect(Store.sessionGet(SETUP_WIZARD_INPUT_ID_KEY)).toEqual('enterprise-input-123');
    });

    it('clears sessionStorage after the stored-input wizard opens', () => {
      Store.sessionSet(SETUP_WIZARD_INPUT_ID_KEY, 'enterprise-input-123');
      asMock(useFeature).mockReturnValue(true);
      asMock(useInput).mockReturnValue({ data: mockInput, isLoading: false } as any);

      render(<SUT />);

      expect(Store.sessionGet(SETUP_WIZARD_INPUT_ID_KEY)).toBeUndefined();
    });

    it('fetches the input by ID from sessionStorage', () => {
      Store.sessionSet(SETUP_WIZARD_INPUT_ID_KEY, 'enterprise-input-123');
      asMock(useFeature).mockReturnValue(true);
      asMock(useInput).mockReturnValue({ data: mockInput, isLoading: false } as any);

      render(<SUT />);

      expect(useInput).toHaveBeenCalledWith('enterprise-input-123');
    });

    it('dismisses the wizard when close is clicked', async () => {
      Store.sessionSet(SETUP_WIZARD_INPUT_ID_KEY, 'enterprise-input-123');
      asMock(useFeature).mockReturnValue(true);
      asMock(useInput).mockReturnValue({ data: mockInput, isLoading: false } as any);

      render(<SUT />);

      await screen.findByTestId('input-setup-wizard');

      await userEvent.click(screen.getByRole('button', { name: /close wizard/i }));

      await waitFor(() => {
        expect(screen.queryByTestId('input-setup-wizard')).not.toBeInTheDocument();
      });

      expect(Store.sessionGet(SETUP_WIZARD_INPUT_ID_KEY)).toBeUndefined();
    });

    it('blocks launching a local wizard while a stored-input wizard is pending', async () => {
      Store.sessionSet(SETUP_WIZARD_INPUT_ID_KEY, 'enterprise-input-123');
      asMock(useFeature).mockReturnValue(true);
      asMock(useInput).mockReturnValue({ data: undefined, isLoading: true } as any);

      render(<SUT />);

      await selectEvent.chooseOption('select input', 'Input Type 1');

      const launchButton = screen.getByRole('button', { name: /launch new input/i });

      expect(launchButton).toBeDisabled();

      await userEvent.click(launchButton);

      expect(screen.queryByText(/test input type/i)).not.toBeInTheDocument();
    });

    it('blocks launching a local wizard while a stored-input wizard is ready', async () => {
      Store.sessionSet(SETUP_WIZARD_INPUT_ID_KEY, 'enterprise-input-123');
      asMock(useFeature).mockReturnValue(true);
      asMock(useInput).mockReturnValue({ data: mockInput, isLoading: false } as any);

      render(<SUT />);

      await screen.findByTestId('input-setup-wizard');
      await selectEvent.chooseOption('select input', 'Input Type 1');

      expect(screen.getByRole('button', { name: /launch new input/i })).toBeDisabled();
      expect(screen.getByText(mockInput.title)).toBeInTheDocument();
      expect(screen.getByText(mockInput.id)).toBeInTheDocument();
    });

    it('still opens the local wizard after creating an input when there is no stored-input flow', async () => {
      jest.useFakeTimers();
      asMock(useFeature).mockReturnValue(true);
      const user = userEvent.setup({ advanceTimers: jest.advanceTimersByTime });

      try {
        render(<SUT />);

        await selectEvent.chooseOption('select input', 'Input Type 1');
        await user.click(screen.getByRole('button', { name: /launch new input/i }));

        const title = await screen.findByRole('textbox', {
          name: /title/i,
        });
        await user.type(title, 'Fresh Input');

        const submitButton = await screen.findByRole('button', { name: /launch input/i });
        await user.click(submitButton);

        await waitFor(() => {
          expect(createInput).toHaveBeenCalled();
        });

        await React.act(async () => {
          jest.advanceTimersByTime(500);
        });

        await screen.findByTestId('input-setup-wizard');

        expect(screen.getByText('Fresh Input')).toBeInTheDocument();
        expect(screen.getByText('input-id-1')).toBeInTheDocument();
      } finally {
        jest.useRealTimers();
      }
    });
  });
});
