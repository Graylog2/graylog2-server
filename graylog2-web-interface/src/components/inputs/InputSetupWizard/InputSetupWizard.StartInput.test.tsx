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
import { render, screen, fireEvent, waitFor } from 'wrappedTestingLibrary';
import selectEvent from 'react-select-event';

import fetch from 'logic/rest/FetchProvider';
import { asMock, StoreMock as MockStore } from 'helpers/mocking';
import usePipelinesConnectedStream from 'hooks/usePipelinesConnectedStream';
import useStreams from 'components/streams/hooks/useStreams';
import useIndexSetsList from 'components/indices/hooks/useIndexSetsList';
import { streams } from 'fixtures/streams';

import InputSetupWizardProvider from './contexts/InputSetupWizardProvider';
import InputSetupWizard from './Wizard';

jest.mock('components/streams/hooks/useStreams', () => jest.fn());
jest.mock('hooks/usePipelinesConnectedStream');
jest.mock('components/indices/hooks/useIndexSetsList');
jest.mock('logic/rest/FetchProvider', () => jest.fn(() => Promise.resolve()));
jest.mock('views/stores/StreamsStore', () => ({ StreamsStore: MockStore() }));
jest.mock('stores/system/SystemStore', () => ({ SystemStore: MockStore() }));
jest.mock('stores/inputs/InputStatesStore', () => ({ InputStatesStore: { start: jest.fn(() => Promise.resolve()), stop: jest.fn(() => Promise.resolve()) } }));

jest.mock('stores/nodes/NodesStore', () => ({
  NodesStore: MockStore(),
}));

const input = {
  id: 'input-test-id',
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

const useStreamsResult = {
  data: {
    list: streams,
    pagination: { total: 1 },
    attributes: [],
  },
  isInitialLoading: false,
  isFetching: false,
  error: undefined,
  refetch: () => {},
};

const pipelinesConnectedMock = (response = []) => ({
  data: response,
  refetch: jest.fn(),
  isInitialLoading: false,
  error: undefined,
  isError: false,
});

const useIndexSetsListResult = {
  data: {
    indexSets:
     [
       {
         id: 'default_id',
         title: 'Default',
         description: 'default index set',
         index_prefix: 'default',
         shards: 1,
         replicas: 1,
         rotation_strategy_class: 'org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategy',
         rotation_strategy: {
           type: 'org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategyConfig',
           max_docs_per_index: 20000000,
         },
         retention_strategy_class: 'org.graylog2.indexer.retention.strategies.NoopRetentionStrategy',
         retention_strategy: {
           type: 'org.graylog2.indexer.retention.strategies.NoopRetentionStrategyConfig',
           max_number_of_indices: 2147483647,
         },
         index_analyzer: '',
         index_optimization_max_num_segments: 0,
         index_optimization_disabled: false,
         field_type_refresh_interval: 1,
         writable: true,
         default: true,
         can_be_default: true,
       },
       {
         id: 'nox_id',
         title: 'Nox',
         description: 'nox index set',
         index_prefix: 'nox',
         shards: 1,
         replicas: 1,
         rotation_strategy_class: 'org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategy',
         rotation_strategy: {
           type: 'org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategyConfig',
           max_docs_per_index: 20000000,
         },
         retention_strategy_class: 'org.graylog2.indexer.retention.strategies.NoopRetentionStrategy',
         retention_strategy: {
           type: 'org.graylog2.indexer.retention.strategies.NoopRetentionStrategyConfig',
           max_number_of_indices: 2147483647,
         },
         index_analyzer: '',
         index_optimization_max_num_segments: 0,
         index_optimization_disabled: false,
         field_type_refresh_interval: 1,
         writable: true,
         default: false,
         can_be_default: true,
       },
     ],
    indexSetsCount: 2,
    indexSetStats: null,
  },
  isInitialLoading: false,
  isSuccess: true,
  error: undefined,
  refetch: () => {},
};

const updateRoutingUrl = '/system/pipelines/pipeline/routing';

const goToStartInputStep = async () => {
  const nextButton = await screen.findByRole('button', { name: /Finish & Start Input/i, hidden: true });

  fireEvent.click(nextButton);
};

const startInput = async () => {
  const startInputButton = await screen.findByTestId('start-input-button');

  fireEvent.click(startInputButton);
};

beforeEach(() => {
  asMock(useStreams).mockReturnValue(useStreamsResult);
  asMock(usePipelinesConnectedStream).mockReturnValue(pipelinesConnectedMock());
  asMock(useIndexSetsList).mockReturnValue(useIndexSetsListResult);
  asMock(fetch).mockImplementation(() => Promise.resolve({}));
});

afterEach(() => {
  jest.clearAllMocks();
});

describe('InputSetupWizard Start Input', () => {
  it('should render the Start Input step', async () => {
    renderWizard();
    goToStartInputStep();

    expect(await screen.findByText(/Set up and start the Input according to the configuration made./i)).toBeInTheDocument();
  });

  it('should start when default stream is selected', async () => {
    renderWizard();
    goToStartInputStep();
    startInput();

    expect(await screen.findByRole('heading', { name: /Setting up Input.../i, hidden: true })).toBeInTheDocument();
    expect(await screen.findByText(/Input started sucessfully!/i)).toBeInTheDocument();
  });

  it('should start input when an existing stream is selected', async () => {
    asMock(fetch).mockImplementation(() => Promise.resolve({}));

    renderWizard();

    const streamSelect = await screen.findByLabelText(/All messages \(Default\)/i);

    await selectEvent.openMenu(streamSelect);

    await selectEvent.select(streamSelect, 'One Stream');

    goToStartInputStep();
    startInput();

    await waitFor(() => expect(fetch).toHaveBeenCalledWith(
      'PUT',
      expect.stringContaining(updateRoutingUrl),
      { input_id: 'input-test-id', stream_id: 'streamId1' },
    ));
  });
});
