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

import { PipelinesPipelines, Streams, PipelinesConnections } from '@graylog/server-api';

import { asMock, StoreMock as MockStore } from 'helpers/mocking';
import usePipelinesConnectedStream from 'hooks/usePipelinesConnectedStream';
import useFilteredStreams from 'components/inputs/InputSetupWizard/hooks/useFilteredStreams';
import useIndexSetsList from 'components/indices/hooks/useIndexSetsList';
import { streams } from 'fixtures/streams';
import { InputStatesStore } from 'stores/inputs/InputStatesStore';

import InputSetupWizardProvider from './contexts/InputSetupWizardProvider';
import InputSetupWizard from './Wizard';

jest.mock('@graylog/server-api', () => ({
  PipelinesPipelines: {
    createFromParser: jest.fn(),
    routing: jest.fn(),
    remove: jest.fn(),
  },
  Streams: {
    create: jest.fn(),
    resume: jest.fn(),
    remove: jest.fn(),
  },
  PipelinesRules: {
    remove: jest.fn(),
  },
  PipelinesConnections: {
    connectStreams: jest.fn(),
  },
}));

jest.mock('components/inputs/InputSetupWizard/hooks/useFilteredStreams');
jest.mock('hooks/usePipelinesConnectedStream');
jest.mock('components/indices/hooks/useIndexSetsList');
jest.mock('logic/rest/FetchProvider', () => jest.fn(() => Promise.resolve()));
jest.mock('views/stores/StreamsStore', () => ({ StreamsStore: MockStore() }));
jest.mock('stores/system/SystemStore', () => ({ SystemStore: MockStore() }));

jest.mock('stores/inputs/InputStatesStore', () => ({
  InputStatesStore: MockStore(['start', jest.fn(() => Promise.resolve())], ['stop', jest.fn(() => Promise.resolve())]),
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
  static_fields: {},
  attributes: {},
};

const onClose = jest.fn();

const renderWizard = () =>
  render(
    <InputSetupWizardProvider>
      <InputSetupWizard show input={input} onClose={onClose} />
    </InputSetupWizardProvider>,
  );

const useStreamsResult = {
  data: { streams, total: 1 },
  isLoading: false,
  isFetching: false,
  error: undefined,
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
    indexSets: [
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

const newStreamConfig = {
  description: 'Wingardium new stream',
  index_set_id: 'default_id',
  remove_matches_from_default_stream: true,
  title: 'Wingardium',
};

const newPipelineConfig = {
  description: 'Pipeline for Stream: Wingardium created by the Input Setup Wizard.',
  source: 'pipeline "Wingardium"\nstage 0 match either\nend',
  title: 'Wingardium',
};

const goToStartInputStep = async () => {
  const nextButton = await screen.findByRole('button', { name: /Next/i, hidden: true });

  fireEvent.click(nextButton);
};

const startInput = async () => {
  const startInputButton = await screen.findByRole('button', { name: /Start Input/i, hidden: true });

  fireEvent.click(startInputButton);
};

const createStream = async (newPipeline = false, removeFromDefault = true) => {
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

  const removeFromDefaultCheckbox = await screen.findByRole('checkbox', {
    name: /remove matches from ‘default stream’/i,
    hidden: true,
  });

  const submitButton = await screen.findByRole('button', {
    name: 'Next',
    hidden: true,
  });

  fireEvent.change(titleInput, { target: { value: 'Wingardium' } });
  fireEvent.change(descriptionInput, { target: { value: 'Wingardium new stream' } });

  if (!removeFromDefault) {
    fireEvent.click(removeFromDefaultCheckbox);
  }

  if (!newPipeline) {
    fireEvent.click(newPipelineCheckbox);
  }

  await waitFor(() => expect(submitButton).toBeEnabled());
  fireEvent.click(submitButton);
};

describe('InputSetupWizard Start Input', () => {
  beforeEach(() => {
    asMock(useFilteredStreams).mockReturnValue(useStreamsResult);
    asMock(usePipelinesConnectedStream).mockReturnValue(pipelinesConnectedMock());
    asMock(useIndexSetsList).mockReturnValue(useIndexSetsListResult);
  });

  describe('Start Input', () => {
    it('should render the Start Input step', async () => {
      renderWizard();
      goToStartInputStep();

      expect(
        await screen.findByText(/Set up and start the Input according to the configuration made./i),
      ).toBeInTheDocument();
    });

    it('should start when default stream is selected', async () => {
      renderWizard();
      goToStartInputStep();
      startInput();

      await waitFor(() => expect(InputStatesStore.start).toHaveBeenCalledWith(input));

      expect(await screen.findByRole('heading', { name: /Setting up Input.../i, hidden: true })).toBeInTheDocument();
      expect(await screen.findByText(/Input started successfully!/i)).toBeInTheDocument();
    });

    it('should start input when an existing stream is selected', async () => {
      renderWizard();

      const selectStreamButton = await screen.findByRole('button', {
        name: /Select Stream/i,
        hidden: true,
      });

      fireEvent.click(selectStreamButton);

      const streamSelect = await screen.findByLabelText(/Default Stream/i);

      await selectEvent.openMenu(streamSelect);

      await selectEvent.select(streamSelect, 'One Stream');

      goToStartInputStep();
      startInput();

      await waitFor(() => expect(InputStatesStore.start).toHaveBeenCalledWith(input));

      await waitFor(() =>
        expect(PipelinesPipelines.routing).toHaveBeenCalledWith({
          input_id: 'input-test-id',
          stream_id: 'streamId1',
          remove_from_default: true,
        }),
      );

      expect(await screen.findByRole('heading', { name: /Setting up Input.../i, hidden: true })).toBeInTheDocument();
      expect(await screen.findByText(/Routing set up!/i)).toBeInTheDocument();
      expect(await screen.findByText(/Input started successfully!/i)).toBeInTheDocument();
    });

    it('should not remove matches from default stream when unchecked', async () => {
      renderWizard();

      const selectStreamButton = await screen.findByRole('button', {
        name: /Select Stream/i,
        hidden: true,
      });

      fireEvent.click(selectStreamButton);

      const streamSelect = await screen.findByLabelText(/Default Stream/i);

      await selectEvent.openMenu(streamSelect);

      await selectEvent.select(streamSelect, 'One Stream');

      const removeFromDefaultCheckbox = await screen.findByRole('checkbox', {
        name: /remove matches from ‘default stream’/i,
        hidden: true,
      });

      fireEvent.click(removeFromDefaultCheckbox);
      goToStartInputStep();
      startInput();

      await waitFor(() => expect(InputStatesStore.start).toHaveBeenCalledWith(input));

      await waitFor(() =>
        expect(PipelinesPipelines.routing).toHaveBeenCalledWith({
          input_id: 'input-test-id',
          stream_id: 'streamId1',
          remove_from_default: false,
        }),
      );

      expect(await screen.findByRole('heading', { name: /Setting up Input.../i, hidden: true })).toBeInTheDocument();
      expect(await screen.findByText(/Routing set up!/i)).toBeInTheDocument();
      expect(await screen.findByText(/Input started successfully!/i)).toBeInTheDocument();
    });
  });

  describe('new stream', () => {
    beforeEach(() => {
      asMock(Streams.create).mockReturnValue(Promise.resolve({ stream_id: '1' }));
    });

    it('should show the progress for all steps', async () => {
      renderWizard();
      await waitFor(() => createStream());
      goToStartInputStep();
      startInput();

      expect(await screen.findByRole('heading', { name: /Setting up Input.../i, hidden: true })).toBeInTheDocument();
      expect(await screen.findByText(/Stream "Wingardium" created!/i)).toBeInTheDocument();
      expect(await screen.findByText(/Routing set up!/i)).toBeInTheDocument();
      expect(await screen.findByText(/Input started successfully!/i)).toBeInTheDocument();
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

      await waitFor(() => expect(Streams.create).toHaveBeenCalledWith(expect.objectContaining(newStreamConfig)));
    });

    it('should start the new stream', async () => {
      renderWizard();
      await waitFor(() => createStream());
      goToStartInputStep();
      startInput();

      await waitFor(() => expect(Streams.resume).toHaveBeenCalled());
    });

    it('create routing for the new stream', async () => {
      renderWizard();
      await waitFor(() => createStream(true));
      goToStartInputStep();
      startInput();

      await waitFor(() =>
        expect(PipelinesPipelines.routing).toHaveBeenCalledWith({
          input_id: 'input-test-id',
          stream_id: '1',
          remove_from_default: undefined,
        }),
      );
    });

    describe('and new pipeline', () => {
      beforeEach(() => {
        asMock(PipelinesPipelines.createFromParser).mockReturnValue(
          Promise.resolve({
            id: '2',
            stages: [],
            description: undefined,
            created_at: '',
            title: 'Wingardium',
            source: undefined,
            modified_at: '',
            errors: [],
            _scope: undefined,
          }),
        );
      });

      it('should create the new pipeline', async () => {
        renderWizard();
        await waitFor(() => createStream(true));
        goToStartInputStep();
        startInput();

        await waitFor(() =>
          expect(PipelinesPipelines.createFromParser).toHaveBeenCalledWith(expect.objectContaining(newPipelineConfig)),
        );

        expect(await screen.findByText(/Pipeline "Wingardium" created!/i)).toBeInTheDocument();
      });

      it('should connect the new pipeline to the stream', async () => {
        renderWizard();
        await waitFor(() => createStream(true));
        goToStartInputStep();
        startInput();

        await waitFor(() =>
          expect(PipelinesConnections.connectStreams).toHaveBeenCalledWith({ pipeline_id: '2', stream_ids: ['1'] }),
        );
      });
    });
  });
});
