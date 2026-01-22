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

import { StreamRoutingRules } from '@graylog/server-api';

import type { Attribute } from 'stores/PaginationTypes';
import DefaultQueryParamProvider from 'routing/DefaultQueryParamProvider';
import { createStreamFixture } from 'components/streams/fixtures';
import { asMock } from 'helpers/mocking';
import StreamConnectedPipelines from 'components/streams/StreamDetails/StreamDataRoutingIntake/StreamConnectedPipelines';

jest.mock('@graylog/server-api', () => ({
  StreamRoutingRules: {
    getPage: jest.fn(),
  },
}));

const listResponse = {
  query: '',
  pagination: {
    total: 1,
    count: 1,
    page: 1,
    per_page: 20,
  },
  total: 0,
  sort: 'rule',
  order: 'asc' as 'asc' | 'desc',
  elements: [
    {
      id: 'test-id-1',
      pipeline_id: 'pipeline-id-1',
      pipeline: 'Pipeline 1',
      rule_id: 'rule-id-1',
      rule: 'Rule 1',
      connected_streams: [
        {
          id: 'stream-id-1',
          title: 'Stream Title 1',
        },
        {
          id: 'stream-id-2',
          title: 'Stream Title 2',
        },
      ],
    },
  ],
  attributes: [
    {
      id: 'id',
      title: 'id',
      type: 'OBJECT_ID' as Attribute['type'],
      sortable: true,
      searchable: true,
      hidden: true,
      related_property: '',
      related_collection: '',
      filterable: false,
      filter_options: [],
    },
    {
      id: 'rule_id',
      title: 'Pipeline Rule ID',
      type: 'STRING' as Attribute['type'],
      sortable: true,
      searchable: false,
      related_property: '',
      related_collection: '',
      filterable: false,
      filter_options: [],
      hidden: false,
    },
    {
      id: 'rule',
      title: 'Pipeline Rule',
      type: 'STRING' as Attribute['type'],
      sortable: true,
      searchable: false,
      related_property: '',
      related_collection: '',
      filterable: false,
      filter_options: [],
      hidden: false,
    },
    {
      id: 'pipeline_id',
      title: 'Pipeline ID',
      type: 'STRING' as Attribute['type'],
      sortable: true,
      searchable: false,
      hidden: true,
      related_property: '',
      related_collection: '',
      filterable: false,
      filter_options: [],
    },
    {
      id: 'pipeline',
      title: 'Pipeline',
      type: 'STRING' as Attribute['type'],
      sortable: true,
      searchable: false,
      related_property: '',
      related_collection: '',
      filterable: false,
      filter_options: [],
      hidden: false,
    },
    {
      id: 'connected_streams',
      title: 'Connected Streams',
      type: 'STRING' as Attribute['type'],
      sortable: true,
      searchable: false,
      related_property: '',
      related_collection: '',
      filterable: false,
      filter_options: [],
      hidden: false,
    },
  ],
  defaults: {
    sort: {
      id: 'rule',
      direction: 'ASC' as 'ASC' | 'DESC',
    },
  },
};

const renderList = () =>
  render(
    <DefaultQueryParamProvider>
      <StreamConnectedPipelines stream={createStreamFixture('1')} />
    </DefaultQueryParamProvider>,
  );

describe('<StreamConnectedPipelines />', () => {
  beforeEach(() => {
    asMock(StreamRoutingRules.getPage).mockReturnValue(Promise.resolve(listResponse));
  });

  it('should render connected rules', async () => {
    renderList();

    const row = await screen.findByTestId('table-row-test-id-1');

    const rule = await within(row).findByText(/Rule 1/i);
    const pipeline = await within(row).findByText(/Pipeline 1/i);
    const connectedStream1 = await within(row).findByText(/Stream Title 1/i);
    const connectedStream2 = await within(row).findByText(/Stream Title 2/i);

    expect(rule).toBeInTheDocument();
    expect(pipeline).toBeInTheDocument();
    expect(connectedStream1).toBeInTheDocument();
    expect(connectedStream2).toBeInTheDocument();
  });
});
