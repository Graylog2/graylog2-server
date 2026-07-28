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
import React from 'react';
import { render, screen, within } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';
import * as Immutable from 'immutable';
import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';

import { indexSets } from 'fixtures/indexSets';
import { asMock } from 'helpers/mocking';
import useFetchEntities from 'components/common/PaginatedEntityTable/useFetchEntities';
import { stream } from 'fixtures/streams';
import useUserLayoutPreferences from 'components/common/EntityDataTable/hooks/useUserLayoutPreferences';
import { layoutPreferences } from 'fixtures/entityListLayoutPreferences';
import useStreamRuleTypes from 'components/streams/hooks/useStreamRuleTypes';
import { streamRuleTypes } from 'fixtures/streamRuleTypes';
import useStreamDestinationFilterRuleCount from 'components/streams/hooks/useStreamDestinationFilterRuleCount';
import useStreamOutputFilters from 'components/streams/hooks/useStreamOutputFilters';
import useStreamRulesInputs from 'hooks/useStreamRulesInputs';
import useStreamOutputs from 'hooks/useStreamOutputs';

import StreamsOverview from './StreamsOverview';

jest.mock('components/common/PaginatedEntityTable/useFetchEntities');
jest.mock('components/streams/hooks/useStreamRuleTypes');
jest.mock('components/common/EntityDataTable/hooks/useUserLayoutPreferences');
jest.mock('components/streams/hooks/useStreamDestinationFilterRuleCount');
jest.mock('components/streams/hooks/useStreamOutputFilters');
jest.mock('hooks/useStreamRulesInputs');
jest.mock('hooks/useStreamOutputs');

const attributes = [
  {
    id: 'title',
    title: 'Title',
    sortable: true,
  },
  {
    id: 'description',
    title: 'Description',
    sortable: true,
    hidden: true,
  },
];

const paginatedStreams = (exampleStream = stream) => ({
  data: {
    pagination: {
      total: 1,
      page: 1,
      perPage: 5,
      count: 1,
    },
    list: [exampleStream],
    attributes,
  },
  refetch: () => {},
  isInitialLoading: false,
});

describe('StreamsOverview', () => {
  const renderSut = () => render(<StreamsOverview indexSets={indexSets} />);

  beforeEach(() => {
    asMock(useUserLayoutPreferences).mockReturnValue({
      data: {
        ...layoutPreferences,
        attributes: {
          title: { status: 'show' },
          description: { status: 'show' },
          rules: { status: 'show' },
          destination_filters: { status: 'show' },
          outputs: { status: 'show' },
        },
      },
      isInitialLoading: false,
      refetch: () => {},
    });

    asMock(useStreamRuleTypes).mockReturnValue({ data: streamRuleTypes });
    asMock(useStreamRulesInputs).mockReturnValue({
      data: [{ id: 'my-id', title: 'input title', name: 'name' }],
    } as any);
    asMock(useStreamDestinationFilterRuleCount).mockReturnValue({
      data: 0,
      refetch: () => {},
      isInitialLoading: false,
      error: undefined,
      isError: false,
    });
    asMock(useStreamOutputs).mockReturnValue({
      data: { outputs: [], total: 0 },
      refetch: () => {},
      isInitialLoading: false,
      isError: false,
    });
    asMock(useStreamOutputFilters).mockReturnValue({
      data: {
        list: Immutable.List([]),
        pagination: {
          total: 0,
          page: 1,
          perPage: 10,
          query: '',
          count: 0,
        },
      },
      refetch: () => {},
      isLoading: false,
      isSuccess: true,
    });
  });

  it('should render empty', async () => {
    const emptyPaginatedStreams = {
      data: {
        pagination: {
          total: 0,
          page: 1,
          perPage: 5,
          count: 0,
        },
        list: [],
        attributes,
      },
      refetch: () => {},
      isInitialLoading: false,
    };
    asMock(useFetchEntities).mockReturnValue(emptyPaginatedStreams);

    renderSut();

    await screen.findByText('No streams have been found.');
  });

  it('should render list', async () => {
    asMock(useFetchEntities).mockReturnValue(paginatedStreams());

    renderSut();

    await screen.findByText(stream.title);
    await screen.findByText(stream.description);
  });

  it('should open and close stream rules overview for a stream', async () => {
    const streamWithRules = {
      ...stream,
      rules: [
        {
          field: 'gl2_remote_ip',
          stream_id: stream.id,
          description: '',
          id: 'stream-rule-id-1',
          type: 1,
          inverted: false,
          value: '127.0.0.1',
        },
        {
          field: 'source',
          stream_id: stream.id,
          description: '',
          id: 'stream-rule-id-2',
          type: 1,
          inverted: false,
          value: 'example.org',
        },
      ],
    };
    asMock(useFetchEntities).mockReturnValue(paginatedStreams(streamWithRules));

    renderSut();

    const tableRow = await screen.findByTestId(`table-row-${streamWithRules.id}`);

    await userEvent.click(within(tableRow).getByTitle('Show stream rules'));

    await screen.findByText(/must match all of the 2 configured stream \./i);
    const deleteStreamRuleButtons = await screen.findAllByRole('button', { name: /delete stream rule/i });
    const editStreamRuleButtons = await screen.findAllByRole('button', { name: /edit stream rule/i });

    expect(deleteStreamRuleButtons.length).toBe(2);
    expect(editStreamRuleButtons.length).toBe(2);
  });

  it('should open and close filter rules overview for a stream', async () => {
    asMock(useFetchEntities).mockReturnValue(paginatedStreams());
    asMock(useStreamDestinationFilterRuleCount).mockReturnValue({
      data: 1,
      refetch: () => {},
      isInitialLoading: false,
      error: undefined,
      isError: false,
    });
    asMock(useStreamOutputFilters).mockReturnValue({
      data: {
        list: Immutable.List([
          {
            id: 'filter-id-1',
            stream_id: stream.id,
            destination_type: 'indexer',
            title: 'Only prod logs',
            description: 'Drops noisy data',
            status: 'enabled',
            rule: {
              operator: 'AND',
              conditions: [],
              actions: [],
            },
          },
        ]),
        pagination: {
          total: 1,
          page: 1,
          perPage: 10,
          query: '',
          count: 1,
        },
      },
      refetch: () => {},
      isLoading: false,
      isSuccess: true,
    });

    renderSut();

    const filterRulesBadge = await screen.findByTitle('Show filter rules');
    await userEvent.click(filterRulesBadge);

    expect(screen.getByText('Only prod logs')).toBeInTheDocument();
    expect(screen.getByText(/Showing 1 configured filter/)).toBeInTheDocument();

    const hideFilterRulesBadge = await screen.findByTitle('Hide filter rules');
    await userEvent.click(hideFilterRulesBadge);

    expect(screen.queryByText('Only prod logs')).not.toBeInTheDocument();
  });

  it('should open and close outputs overview for a stream', async () => {
    const streamWithOutputs = {
      ...stream,
      outputs: ['output-id-1'] as any,
    };
    asMock(useFetchEntities).mockReturnValue(paginatedStreams(streamWithOutputs));
    asMock(useStreamOutputs).mockReturnValue({
      data: {
        outputs: [
          {
            id: 'output-id-1',
            title: 'My GELF Output',
            type: 'org.graylog2.outputs.GelfOutput',
            configuration: {},
          },
        ],
        total: 1,
      },
      refetch: () => {},
      isInitialLoading: false,
      isError: false,
    });

    renderSut();

    const tableRow = await screen.findByTestId(`table-row-${streamWithOutputs.id}`);

    await userEvent.click(within(tableRow).getByTitle('Show stream outputs'));

    expect(
      await screen.findByText(
        (_, element) => element?.tagName.toLowerCase() === 'p' && element.textContent === '1 connected output.',
      ),
    ).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /my gelf output/i })).toHaveAttribute(
      'href',
      `/streams/${streamWithOutputs.id}/view?segment=destinations&edit_output=output-id-1`,
    );
    expect(screen.getByRole('link', { name: /manage outputs/i })).toBeInTheDocument();

    await userEvent.click(within(tableRow).getByTitle('Hide stream outputs'));

    expect(screen.queryByText(/1 connected output\./i)).not.toBeInTheDocument();
  });

  it('should only show grouped plugin table elements in their matching view', async () => {
    const plugin = new PluginManifest(
      {},
      {
        'components.streams.overview.tableElements': [
          {
            attributeName: 'my_plugin_column',
            group: 'routing',
            attributes: [{ id: 'my_plugin_column', title: 'My Plugin Column' }],
            columnRenderers: {
              my_plugin_column: {
                renderCell: () => 'plugin cell',
                staticWidth: 'matchHeader',
              },
            },
          },
          {
            attributeName: 'my_ungrouped_column',
            attributes: [{ id: 'my_ungrouped_column', title: 'My Ungrouped Column' }],
            columnRenderers: {
              my_ungrouped_column: {
                renderCell: () => 'ungrouped cell',
                staticWidth: 'matchHeader',
              },
            },
          },
        ],
      },
    );

    PluginStore.register(plugin);
    asMock(useFetchEntities).mockReturnValue(paginatedStreams());
    asMock(useUserLayoutPreferences).mockReturnValue({
      data: {
        ...layoutPreferences,
        attributes: undefined,
      },
      isInitialLoading: false,
      refetch: () => {},
    });

    try {
      renderSut();

      await screen.findByText('Title');
      expect(screen.queryByText('My Plugin Column')).not.toBeInTheDocument();
      expect(screen.queryByText('My Ungrouped Column')).not.toBeInTheDocument();

      await userEvent.click(screen.getByRole('radio', { name: 'Routing' }));

      await screen.findByText('My Plugin Column');
      expect(screen.getByText('plugin cell')).toBeInTheDocument();
      expect(screen.queryByText('My Ungrouped Column')).not.toBeInTheDocument();
    } finally {
      PluginStore.unregister(plugin);
    }
  });
});
