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

import { asMock } from 'helpers/mocking';
import usePipelinesConnectedStream from 'hooks/usePipelinesConnectedStream';
import useFilteredStreams from 'components/inputs/InputSetupWizard/hooks/useFilteredStreams';
import useIndexSetsList from 'components/indices/hooks/useIndexSetsList';
import useInputReferences from 'components/inputs/InputSetupWizard/hooks/useInputReferences';
import useStreamsByIndexSet from 'components/inputs/InputSetupWizard/hooks/useStreamsByIndexSet';

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

jest.mock('components/inputs/InputSetupWizard/hooks/useFilteredStreams');
jest.mock('hooks/usePipelinesConnectedStream');
jest.mock('components/indices/hooks/useIndexSetsList');
jest.mock('components/inputs/InputSetupWizard/hooks/useInputReferences');
jest.mock('components/inputs/InputSetupWizard/hooks/useStreamsByIndexSet');

const useStreamsResult = (list = []) => ({
  data: { streams: list, total: 1 },
  isLoading: false,
  isFetching: false,
  error: undefined,
});

const useStreamByIndexSetResult = (data = { total: 0, streams: [] }) => ({
  data,
  isLoading: false,
});

const pipelinesConnectedMock = (response = []) => ({
  data: response,
  refetch: jest.fn(),
  isInitialLoading: false,
  error: undefined,
  isError: false,
});

const useInputReferencesResult = {
  isLoading: false,
  data: {
    isInputAlreadyInUse: true,
    stream_refs: [
      { name: 'In use stream 1', id: 'stream_1' },
      { name: 'In use stream 2', id: 'stream_2' },
    ],
    pipeline_refs: [
      { name: 'In use pipeline 1', id: 'pipeline_1' },
      { name: 'In use pipeline 2', id: 'pipeline_2' },
    ],
  },
};

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

const getStreamCreateFormFields = async () => {
  const titleInput = await screen.findByRole('textbox', {
    name: /Title/i,
    hidden: true,
  });

  const descriptionInput = await screen.findByRole('textbox', {
    name: /Description/i,
    hidden: true,
  });

  const indexSetSelect = await screen.findByLabelText('Index Set');

  const removeMatchesCheckbox = await screen.findByRole('checkbox', {
    name: /Remove matches from/i,
    hidden: true,
  });

  const newPipelineCheckbox = await screen.findByRole('checkbox', {
    name: /Create a new pipeline for this stream/i,
    hidden: true,
  });

  const submitButton = await screen.findByRole('button', {
    name: 'Next',
    hidden: true,
  });

  return {
    titleInput,
    descriptionInput,
    indexSetSelect,
    removeMatchesCheckbox,
    newPipelineCheckbox,
    submitButton,
  };
};

beforeEach(() => {
  asMock(useFilteredStreams).mockReturnValue(useStreamsResult());
  asMock(usePipelinesConnectedStream).mockReturnValue(pipelinesConnectedMock());
  asMock(useIndexSetsList).mockReturnValue(useIndexSetsListResult);
});

describe('InputSetupWizard Setup Routing', () => {
  describe('with existing stream', () => {
    beforeEach(() => {
      asMock(useStreamsByIndexSet).mockReturnValue(useStreamByIndexSetResult());
      asMock(useFilteredStreams).mockReturnValue(useStreamsResult());
      asMock(usePipelinesConnectedStream).mockReturnValue(pipelinesConnectedMock());
      asMock(useIndexSetsList).mockReturnValue(useIndexSetsListResult);
      asMock(useInputReferences).mockReturnValue(useInputReferencesResult);
    });

    it('should render the Setup Routing step', async () => {
      renderWizard();
      const routingStepText = await screen.findByText(
        /Select a destination Stream to route messages from this input to./i,
      );

      expect(routingStepText).toBeInTheDocument();
    });

    it('should show a warning if the input is already in use', async () => {
      renderWizard();

      await screen.findByText('Input already in use - Message Duplication Risk!');
      await screen.findByText('In use stream 1');
      await screen.findByText('In use stream 2');
      await screen.findByText('In use pipeline 1');
      await screen.findByText('In use pipeline 2');
    });

    describe('Stream Selection', () => {
      it('should show the stream select when clicking on choose stream', async () => {
        renderWizard();
        const selectStreamButton = await screen.findByRole('button', {
          name: /Select Stream/i,
          hidden: true,
        });

        fireEvent.click(selectStreamButton);

        await screen.findByLabelText(/Default Stream/i);
      });

      it('should only show editable existing streams', async () => {
        asMock(useFilteredStreams).mockReturnValue(
          useStreamsResult([
            { id: 'alohoid', title: 'Aloho', is_editable: true },
            { id: 'moraid', title: 'Mora', is_editable: false },
          ]),
        );

        renderWizard();
        const selectStreamButton = await screen.findByRole('button', {
          name: /Select Stream/i,
          hidden: true,
        });

        fireEvent.click(selectStreamButton);

        const streamSelect = await screen.findByLabelText(/Default Stream/i);

        await selectEvent.openMenu(streamSelect);

        const alohoOption = await screen.findByText(/Aloho/i);
        const moraOption = screen.queryByText(/Mora/i);

        expect(alohoOption).toBeInTheDocument();
        expect(moraOption).not.toBeInTheDocument();
      });

      it('should not show existing default stream in select', async () => {
        asMock(useFilteredStreams).mockReturnValue(
          useStreamsResult([
            { id: 'alohoid', title: 'Aloho', is_editable: true, is_default: true },
            { id: 'moraid', title: 'Mora', is_editable: true },
          ]),
        );

        renderWizard();

        const selectStreamButton = await screen.findByRole('button', {
          name: /Select Stream/i,
          hidden: true,
        });

        fireEvent.click(selectStreamButton);

        const streamSelect = await screen.findByLabelText(/Default Stream/i);

        await selectEvent.openMenu(streamSelect);

        const moraOption = await screen.findByText(/Mora/i);
        const alohoOption = screen.queryByText(/Aloho/i);

        expect(moraOption).toBeInTheDocument();
        expect(alohoOption).not.toBeInTheDocument();
      });

      it('should allow the user to select a stream', async () => {
        asMock(useFilteredStreams).mockReturnValue(
          useStreamsResult([
            { id: 'alohoid', title: 'Aloho', is_editable: true },
            { id: 'moraid', title: 'Mora', is_editable: true },
          ]),
        );

        renderWizard();

        const selectStreamButton = await screen.findByRole('button', {
          name: /Select Stream/i,
          hidden: true,
        });

        fireEvent.click(selectStreamButton);

        const streamSelect = await screen.findByLabelText(/Default Stream/i);

        await selectEvent.openMenu(streamSelect);
        await selectEvent.select(streamSelect, 'Aloho');
      });

      it('should show a warning if the selected stream has connected pipelines', async () => {
        asMock(useFilteredStreams).mockReturnValue(
          useStreamsResult([
            { id: 'alohoid', title: 'Aloho', is_editable: true },
            { id: 'moraid', title: 'Mora', is_editable: true },
          ]),
        );

        asMock(usePipelinesConnectedStream).mockReturnValue(
          pipelinesConnectedMock([
            { id: 'pipeline1', title: 'Pipeline1' },
            { id: 'pipeline2', title: 'Pipeline2' },
          ]),
        );

        renderWizard();

        const selectStreamButton = await screen.findByRole('button', {
          name: /Select Stream/i,
          hidden: true,
        });

        fireEvent.click(selectStreamButton);

        const streamSelect = await screen.findByLabelText(/Default Stream/i);

        await selectEvent.openMenu(streamSelect);

        await selectEvent.select(streamSelect, 'Aloho');

        const warning = await screen.findByText(/Pipelines connected to target Stream/i);
        const warningPipeline1 = await screen.findByText(/Pipeline1/i);
        const warningPipeline2 = await screen.findByText(/Pipeline2/i);

        expect(warning).toBeInTheDocument();
        expect(warningPipeline1).toBeInTheDocument();
        expect(warningPipeline2).toBeInTheDocument();
      });
    });
  });

  describe('Stream creation', () => {
    beforeEach(() => {
      asMock(useStreamsByIndexSet).mockReturnValue(
        useStreamByIndexSetResult({
          total: 2,
          streams: [
            { id: 'alohoid', title: 'Aloho', is_editable: true },
            { id: 'moraid', title: 'Mora', is_editable: true },
          ],
        }),
      );
    });

    it('should allow the user to create a new stream', async () => {
      renderWizard();

      const createStreamButton = await screen.findByRole('button', {
        name: /Create Stream/i,
        hidden: true,
      });

      fireEvent.click(createStreamButton);

      await screen.findByRole('heading', { name: /Create new stream/i, hidden: true });

      const { titleInput, descriptionInput, indexSetSelect, removeMatchesCheckbox, newPipelineCheckbox, submitButton } =
        await getStreamCreateFormFields();

      fireEvent.change(descriptionInput, { target: { value: 'Wingardium new stream' } });
      await selectEvent.openMenu(indexSetSelect);
      await selectEvent.select(indexSetSelect, 'Nox');
      fireEvent.click(removeMatchesCheckbox);
      fireEvent.click(newPipelineCheckbox);

      expect(submitButton).toBeDisabled();

      fireEvent.change(titleInput, { target: { value: 'Wingardium' } });

      await waitFor(() => expect(submitButton).toBeEnabled());
    });

    it('should show a warning when the user selects the default index set', async () => {
      renderWizard();

      const createStreamButton = await screen.findByRole('button', {
        name: /Create Stream/i,
        hidden: true,
      });

      fireEvent.click(createStreamButton);

      await screen.findByRole('heading', { name: /Create new stream/i, hidden: true });

      const { titleInput, descriptionInput, indexSetSelect } = await getStreamCreateFormFields();

      fireEvent.change(titleInput, { target: { value: 'Wingardium' } });
      fireEvent.change(descriptionInput, { target: { value: 'Wingardium new stream' } });
      await selectEvent.openMenu(indexSetSelect);
      await selectEvent.select(indexSetSelect, 'Default');

      expect(await screen.findByText(/You have selected the Default Index Set./i)).toBeInTheDocument();
    });

    it('should show a warning when the user selects an index set already associated with other streams', async () => {
      renderWizard();

      const createStreamButton = await screen.findByRole('button', {
        name: /Create Stream/i,
        hidden: true,
      });

      fireEvent.click(createStreamButton);

      await screen.findByRole('heading', { name: /Create new stream/i, hidden: true });

      const { titleInput, descriptionInput, indexSetSelect } = await getStreamCreateFormFields();

      fireEvent.change(titleInput, { target: { value: 'Wingardium' } });
      fireEvent.change(descriptionInput, { target: { value: 'Wingardium new stream' } });
      await selectEvent.openMenu(indexSetSelect);
      await selectEvent.select(indexSetSelect, 'Nox');

      await screen.findByText(/Selected index set already associated with another stream/i);
    });
  });
});
