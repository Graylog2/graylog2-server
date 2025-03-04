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

import { asMock } from 'helpers/mocking';
import useInputDiagnosis from 'components/inputs/InputDiagnosis/useInputDiagnosis';

import InputDiagnosisPage from './InputDiagnosisPage';

jest.mock('routing/useParams', () => jest.fn(() => ({
  inputId: 'test-input-id',
})));

jest.mock('components/inputs/InputDiagnosis/useInputDiagnosis');

const input = {
  id: 'test-input-id',
  title: 'inputTitle',
  type: 'type',
  global: false,
  name: 'inputName',
  created_at: '',
  creator_user_id: 'creatorId',
  static_fields: { },
  attributes: { },
};

const inputNodeStates = {
  total: 2,
  states: {
    RUNNING: [{ node_id: 'test-node-id-1', detailed_message: undefined }],
    FAILED: [{ node_id: 'test-node-id-2', detailed_message: 'failed for testing' }],
  },
};

const inputMetrics = {
  incomingMessagesTotal: 11,
  emptyMessages: 12,
  open_connections: 13,
  total_connections: 14,
  read_bytes_1sec: 15,
  read_bytes_total: 16,
  write_bytes_1sec: 17,
  write_bytes_total: 18,
  message_errors:{
    failures_indexing: 19,
    failures_processing: 20,
    failures_inputs_codecs: 21,
  },
  stream_message_count: [
    { stream_name: 'Test Stream 1', stream_id: '1', count: 22 },
    { stream_name: 'Test Stream 2', stream_id: '2', count: 23 },
  ],
};

const useInputDiagnosisMock = { input, inputNodeStates, inputMetrics };

describe('Input Diagnosis Page', () => {
  beforeEach(() => {
    asMock(useInputDiagnosis).mockReturnValue(useInputDiagnosisMock);
  });

  it('renders the page for the given input with its metrics', async () => {
    render(
      <InputDiagnosisPage />,
    );

    expect(await screen.findByText(/inputTitle/)).toBeInTheDocument();
    expect(await screen.findByText(/11 events/)).toBeInTheDocument();
    expect(await screen.findByText(/12/)).toBeInTheDocument();
    expect(await screen.findByText(/13/)).toBeInTheDocument();
    expect(await screen.findByText(/14 total/)).toBeInTheDocument();
    expect(await screen.findByText(/15.0B/)).toBeInTheDocument();
    expect(await screen.findByText(/16.0B/)).toBeInTheDocument();
    expect(await screen.findByText(/17.0B/)).toBeInTheDocument();
    expect(await screen.findByText(/18.0B/)).toBeInTheDocument();
    expect(await screen.findByText(/19/)).toBeInTheDocument();
    expect(await screen.findByText(/20/)).toBeInTheDocument();
    expect(await screen.findByText(/21/)).toBeInTheDocument();
    expect(await screen.findByText(/Test Stream 1/)).toBeInTheDocument();
    expect(await screen.findByText(/22/)).toBeInTheDocument();
    expect(await screen.findByText(/Test Stream 2/)).toBeInTheDocument();
    expect(await screen.findByText(/23/)).toBeInTheDocument();
    expect(await screen.findByRole('button', { name: /running: 1\/2/ })).toBeInTheDocument();
    expect(await screen.findByRole('button', { name: /failed: 1\/2/ })).toBeInTheDocument();
  });

  it('shows nodes related to state on button click', async () => {
    render(
      <InputDiagnosisPage />,
    );

    const runningButton = await screen.findByRole('button', { name: /running: 1\/2/ });
    const failedButton = await screen.findByRole('button', { name: /failed: 1\/2/ });

    fireEvent.click(runningButton);

    expect(await screen.findByText(/test-node-id-1/)).toBeInTheDocument();

    fireEvent.click(failedButton);

    expect(await screen.findByText(/test-node-id-2/)).toBeInTheDocument();
  });

  it('shows detailed messages on state nodes related to state on button click', async () => {
    render(
      <InputDiagnosisPage />,
    );

    const failedButton = await screen.findByRole('button', { name: /failed: 1\/2/ });

    fireEvent.click(failedButton);

    expect(await screen.findByText(/failed for testing/)).toBeInTheDocument();
  });
});
