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
import { InputStatesStore } from 'stores/inputs/InputStatesStore';

import InputSetupWizardProvider from './contexts/InputSetupWizardProvider';
import InputSetupWizard from './Wizard';

jest.mock('components/streams/hooks/useStreams', () => jest.fn());
jest.mock('hooks/usePipelinesConnectedStream');
jest.mock('components/indices/hooks/useIndexSetsList');
jest.mock('logic/rest/FetchProvider', () => jest.fn(() => Promise.resolve()));
jest.mock('views/stores/StreamsStore', () => ({ StreamsStore: MockStore() }));
jest.mock('stores/system/SystemStore', () => ({ SystemStore: MockStore() }));

jest.mock('stores/inputs/InputStatesStore', () => ({
  InputStatesStore: MockStore(
    ['start', jest.fn(() => Promise.resolve())],
    ['stop', jest.fn(() => Promise.resolve())],
  ),
}));

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

const updateRoutingUrlRegEx = /.+(system\/pipelines\/pipeline\/routing)/i;
const createStreamUrl = '/streams';
const startStreamUrl = (streamId) => `/streams/${streamId}/resume`;
const createPipelineUrlRegEx = /.+(system\/pipelines\/pipeline)/i;

const newStreamConfig = {
  description: 'Wingardium new stream',
  index_set_id: 'default_id',
  remove_matches_from_default_stream: undefined,
  title: 'Wingardium',
};

const newPipelineConfig = {
  description: 'Pipeline for Stream: Wingardium created by the Input Setup Wizard.',
  source: 'pipeline "Wingardium"\nstage 0 match either\nend',
  title: 'Wingardium',
};

const goToStartInputStep = async () => {
  const nextButton = await screen.findByRole('button', { name: /Finish & Start Input/i, hidden: true });

  fireEvent.click(nextButton);
};

const startInput = async () => {
  const startInputButton = await screen.findByTestId('start-input-button');

  fireEvent.click(startInputButton);
};

const createStream = async (newPipeline = false) => {
  const createStreamButton = await screen.findByRole('button', {
    name: /Create Stream/i,
    hidden: true,
  });

  fireEvent.click(createStreamButton);

  await screen.findByRole('heading', { name: /Create new stream/i, hidden: true });

  const titleInput = await screen.findByRole('textbox', {
    name: /Title/i,
    hidden: true,
  });

  const descriptionInput = await screen.findByRole('textbox', {
    name: /Description/i,
    hidden: true,
  });

  const newPipelineCheckbox = await screen.findByRole('checkbox', {
    name: /Create a new pipeline for this stream/i,
    hidden: true,
  });

  const submitButton = await screen.findByRole('button', {
    name: 'Create',
    hidden: true,
  });

  fireEvent.change(titleInput, { target: { value: 'Wingardium' } });
  fireEvent.change(descriptionInput, { target: { value: 'Wingardium new stream' } });

  if (newPipeline) {
    fireEvent.click(newPipelineCheckbox);
  }

  await waitFor(() => expect(submitButton).toBeEnabled());
  fireEvent.click(submitButton);
};

beforeEach(() => {
  asMock(useStreams).mockReturnValue(useStreamsResult);
  asMock(usePipelinesConnectedStream).mockReturnValue(pipelinesConnectedMock());
  asMock(useIndexSetsList).mockReturnValue(useIndexSetsListResult);
  asMock(fetch).mockImplementation(() => Promise.resolve({}));
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

    await waitFor(() => expect(InputStatesStore.start).toHaveBeenCalledWith(input));

    expect(await screen.findByRole('heading', { name: /Setting up Input.../i, hidden: true })).toBeInTheDocument();
    expect(await screen.findByText(/Input started sucessfully!/i)).toBeInTheDocument();
  });

  it('should start input when an existing stream is selected', async () => {
    renderWizard();

    const streamSelect = await screen.findByLabelText(/All messages \(Default\)/i);

    await selectEvent.openMenu(streamSelect);

    await selectEvent.select(streamSelect, 'One Stream');

    goToStartInputStep();
    startInput();

    await waitFor(() => expect(InputStatesStore.start).toHaveBeenCalledWith(input));

    await waitFor(() => expect(fetch).toHaveBeenCalledWith(
      'PUT',
      expect.stringMatching(updateRoutingUrlRegEx),
      { input_id: 'input-test-id', stream_id: 'streamId1' },
    ));

    expect(await screen.findByRole('heading', { name: /Setting up Input.../i, hidden: true })).toBeInTheDocument();
    expect(await screen.findByText(/Routing set up!/i)).toBeInTheDocument();
    expect(await screen.findByText(/Input started sucessfully!/i)).toBeInTheDocument();
  });

  describe('new stream', () => {
    it('should show the progress for all steps', async () => {
      renderWizard();
      await waitFor(() => createStream(true));
      goToStartInputStep();
      startInput();

      expect(await screen.findByRole('heading', { name: /Setting up Input.../i, hidden: true })).toBeInTheDocument();
      expect(await screen.findByText(/Stream "Wingardium" created!/i)).toBeInTheDocument();
      expect(await screen.findByText(/Pipeline "Wingardium" created!/i)).toBeInTheDocument();
      expect(await screen.findByText(/Routing set up!/i)).toBeInTheDocument();
      expect(await screen.findByText(/Input started sucessfully!/i)).toBeInTheDocument();
    });

    it('should start the input', async () => {
      renderWizard();
      await waitFor(() => createStream());
      goToStartInputStep();
      startInput();

      await waitFor(() => expect(InputStatesStore.start).toHaveBeenCalledWith(input));
    });

    it('should create the new stream', async () => {
      renderWizard();
      await waitFor(() => createStream());
      goToStartInputStep();
      startInput();

      await waitFor(() => expect(fetch).toHaveBeenCalledWith(
        'POST',
        expect.stringContaining(createStreamUrl),
        newStreamConfig,
      ));
    });

    it('should start the new stream', async () => {
      asMock(fetch).mockImplementation(() => Promise.resolve({ stream_id: 1 }));

      renderWizard();
      await waitFor(() => createStream());
      goToStartInputStep();
      startInput();

      await waitFor(() => expect(fetch).toHaveBeenCalledWith(
        'POST',
        expect.stringContaining(startStreamUrl(1)),
      ));
    });

    it('should create the new pipeline', async () => {
      renderWizard();
      await waitFor(() => createStream(true));
      goToStartInputStep();
      startInput();

      await waitFor(() => expect(fetch).toHaveBeenCalledWith(
        'POST',
        expect.stringMatching(createPipelineUrlRegEx),
        newPipelineConfig,
      ));
    });

    it('create routing for the new stream', async () => {
      renderWizard();
      await waitFor(() => createStream(true));
      goToStartInputStep();
      startInput();

      await waitFor(() => expect(fetch).toHaveBeenCalledWith(
        'PUT',
        expect.stringMatching(updateRoutingUrlRegEx),
        { input_id: 'input-test-id', stream_id: 'streamId1' },
      ));
    });
  });
});
