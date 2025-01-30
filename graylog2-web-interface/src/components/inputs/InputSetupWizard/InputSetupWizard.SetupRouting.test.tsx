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
import useStreams from 'components/streams/hooks/useStreams';
import useIndexSetsList from 'components/indices/hooks/useIndexSetsList';

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
jest.mock('components/indices/hooks/useIndexSetsList');

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

const getStreamCreateFormFields = async () => {
  const titleInput = await screen.findByRole('textbox', {
    name: /Title/i,
    hidden: true,
  });

  const descriptionInput = await screen.findByRole('textbox', {
    name: /Description/i,
    hidden: true,
  });

  const indexSetSelect = await screen.findByLabelText(/Index Set/i);

  const removeMatchesCheckbox = await screen.findByRole('checkbox', {
    name: /Remove matches from/i,
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
  asMock(useStreams).mockReturnValue(useStreamsResult());
  asMock(usePipelinesConnectedStream).mockReturnValue(pipelinesConnectedMock());
  asMock(useIndexSetsList).mockReturnValue(useIndexSetsListResult);
});

describe('InputSetupWizard Setup Routing', () => {
  it('should render the Setup Routing step', async () => {
    renderWizard();

    const routingStepText = await screen.findByText(/Choose a Destination Stream to route Messages from this Input to./i);

    expect(routingStepText).toBeInTheDocument();
  });

  it('should only show editable existing streams', async () => {
    asMock(useStreams).mockReturnValue(useStreamsResult(
      [
        { id: 'alohoid', title: 'Aloho', is_editable: true },
        { id: 'moraid', title: 'Mora', is_editable: false },
      ],
    ));

    renderWizard();

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

  describe('Stream creation', () => {
    it('should allow the user to create a new stream', async () => {
      asMock(useIndexSetsList).mockReturnValue(useIndexSetsListResult);

      renderWizard();

      const createStreamButton = await screen.findByRole('button', {
        name: /Create Stream/i,
        hidden: true,
      });

      fireEvent.click(createStreamButton);

      await screen.findByRole('heading', { name: /Create new stream/i, hidden: true });

      const { titleInput, descriptionInput, indexSetSelect, removeMatchesCheckbox, newPipelineCheckbox, submitButton } = await getStreamCreateFormFields();

      fireEvent.change(titleInput, { target: { value: 'Wingardium' } });
      fireEvent.change(descriptionInput, { target: { value: 'Wingardium new stream' } });
      await selectEvent.openMenu(indexSetSelect);
      await selectEvent.select(indexSetSelect, 'Nox');
      fireEvent.click(removeMatchesCheckbox);
      fireEvent.click(newPipelineCheckbox);

      await waitFor(() => expect(submitButton).toBeEnabled());
      fireEvent.click(submitButton);

      expect(await screen.findByText(/This input will use a new stream: "Wingardium"./i)).toBeInTheDocument();
      expect(await screen.findByText(/Matches will be removed from the Default stream./i)).toBeInTheDocument();
      expect(await screen.findByText(/A new pipeline will be created./i)).toBeInTheDocument();

      expect(await screen.findByRole('button', {
        name: /Reset/i,
        hidden: true,
      }),
      ).toBeInTheDocument();
    });

    it('should disable and enable the next step button', async () => {
      asMock(useIndexSetsList).mockReturnValue(useIndexSetsListResult);

      renderWizard();

      const createStreamButton = await screen.findByRole('button', {
        name: /Create Stream/i,
        hidden: true,
      });

      const nextStepButton = await screen.findByRole('button', {
        name: /Finish & Start Input/i,
        hidden: true,
      });

      expect(nextStepButton).toBeEnabled();

      fireEvent.click(createStreamButton);

      expect(nextStepButton).toBeDisabled();

      await screen.findByRole('heading', { name: /Create new stream/i, hidden: true });

      const { titleInput, descriptionInput, indexSetSelect, removeMatchesCheckbox, newPipelineCheckbox, submitButton } = await getStreamCreateFormFields();

      fireEvent.change(titleInput, { target: { value: 'Wingardium' } });
      fireEvent.change(descriptionInput, { target: { value: 'Wingardium new stream' } });
      await selectEvent.openMenu(indexSetSelect);
      await selectEvent.select(indexSetSelect, 'Nox');
      fireEvent.click(removeMatchesCheckbox);
      fireEvent.click(newPipelineCheckbox);

      await waitFor(() => expect(submitButton).toBeEnabled());
      fireEvent.click(submitButton);

      expect(await screen.findByText(/This input will use a new stream: "Wingardium"./i)).toBeInTheDocument();

      expect(nextStepButton).toBeEnabled();
    });

    it('should allow the user to reset the new stream', async () => {
      asMock(useIndexSetsList).mockReturnValue(useIndexSetsListResult);

      renderWizard();

      const createStreamButton = await screen.findByRole('button', {
        name: /Create Stream/i,
        hidden: true,
      });

      fireEvent.click(createStreamButton);

      await screen.findByRole('heading', { name: /Create new stream/i, hidden: true });

      const { titleInput, descriptionInput, indexSetSelect, removeMatchesCheckbox, newPipelineCheckbox, submitButton } = await getStreamCreateFormFields();

      fireEvent.change(titleInput, { target: { value: 'Wingardium' } });
      fireEvent.change(descriptionInput, { target: { value: 'Wingardium new stream' } });
      await selectEvent.openMenu(indexSetSelect);
      await selectEvent.select(indexSetSelect, 'Nox');
      fireEvent.click(removeMatchesCheckbox);
      fireEvent.click(newPipelineCheckbox);

      await waitFor(() => expect(submitButton).toBeEnabled());
      fireEvent.click(submitButton);

      expect(await screen.findByText(/This input will use a new stream: "Wingardium"./i)).toBeInTheDocument();

      const resetButton = await screen.findByRole('button', {
        name: /Reset/i,
        hidden: true,
      });

      fireEvent.click(resetButton);

      expect(screen.queryByRole('heading', { name: /Create new stream/i, hidden: true })).not.toBeInTheDocument();
      expect(await screen.findByRole('heading', { name: /Route to a new Stream/i, hidden: true })).toBeInTheDocument();
    });
  });
});
