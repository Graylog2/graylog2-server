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
import { ReactRouter6Adapter } from 'use-query-params/adapters/react-router-6';
import { QueryParamProvider } from 'use-query-params';

import { indexSets } from 'fixtures/indexSets';
import { asMock, MockStore } from 'helpers/mocking';
import useFetchEntities from 'components/common/PaginatedEntityTable/useFetchEntities';
import { stream } from 'fixtures/streams';
import useUserLayoutPreferences from 'components/common/EntityDataTable/hooks/useUserLayoutPreferences';
import { layoutPreferences } from 'fixtures/entityListLayoutPreferences';
import useStreamRuleTypes from 'components/streams/hooks/useStreamRuleTypes';
import { streamRuleTypes } from 'fixtures/streamRuleTypes';

import StreamsOverview from './StreamsOverview';

jest.mock('components/common/PaginatedEntityTable/useFetchEntities');
jest.mock('components/streams/hooks/useStreamRuleTypes');
jest.mock('components/common/EntityDataTable/hooks/useUserLayoutPreferences');

jest.mock('stores/inputs/StreamRulesInputsStore', () => ({
  StreamRulesInputsActions: {
    list: jest.fn(),
  },
  StreamRulesInputsStore: MockStore(['getInitialState', () => ({
    inputs: [
      { id: 'my-id', title: 'input title', name: 'name' },
    ],
  })]),
}));

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
  const renderSut = () => render(
    <QueryParamProvider adapter={ReactRouter6Adapter}>
      <StreamsOverview indexSets={indexSets} />
    </QueryParamProvider>,
  );

  beforeEach(() => {
    asMock(useUserLayoutPreferences).mockReturnValue({
      data: { ...layoutPreferences, displayedAttributes: ['title', 'description', 'rules'] },
      isInitialLoading: false,
    });

    asMock(useStreamRuleTypes).mockReturnValue({ data: streamRuleTypes });
  });

  it('should render empty', async () => {
    const emptyPaginatedStreams = ({
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
    });
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

    userEvent.click(within(tableRow).getByTitle('Show stream rules'));

    await screen.findByText(/must match all of the 2 configured stream \./i);
    const deleteStreamRuleButtons = await screen.findAllByRole('button', { name: /delete stream rule/i });
    const editStreamRuleButtons = await screen.findAllByRole('button', { name: /edit stream rule/i });

    expect(deleteStreamRuleButtons.length).toBe(2);
    expect(editStreamRuleButtons.length).toBe(2);
  });
});
